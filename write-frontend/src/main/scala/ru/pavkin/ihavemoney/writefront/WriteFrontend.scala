package ru.pavkin.ihavemoney.writefront

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCredentials, Location, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Framing
import akka.util.{ByteString, Timeout}
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.syntax._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{AssetId, ExpenseCategory, FortuneId, IncomeCategory, LiabilityId}
import ru.pavkin.ihavemoney.domain.unexpected
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.protocol.writefront._
import ru.pavkin.ihavemoney.protocol.{Auth, CommandProcessedWithResult, FailedRequest, TSVImportResult}
import ru.pavkin.ihavemoney.auth.JWTTokenFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Try}

object WriteFrontend extends App with CirceSupport with CorsDirectives {

  implicit val system = ActorSystem("IHaveMoneyWriteFront")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(30.seconds)

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val writeBack = new WriteBackClusterClient(system)

  val tokenFactory: JWTTokenFactory = new JWTTokenFactory(config.getString("app.secret-key"))
  val authenticator: (Option[HttpCredentials]) ⇒ Future[AuthenticationResult[UserId]] = (credentials: Option[HttpCredentials]) ⇒ Future {
    credentials.flatMap {
      case token: OAuth2BearerToken ⇒
        tokenFactory.authenticate(token.token)
      case _ ⇒ None
    } match {
      case Some(userId) ⇒ Right(userId)
      case None ⇒ Left(HttpChallenge("Bearer", "ihavemoney", Map("error" → "invalid_token")))
    }
  }

  val unsafeRoutesEnabled = Try(config.getBoolean("app.unsafe-routes-enabled")).getOrElse(false)

  def safe(m: => ToResponseMarshallable) = complete {
    if (unsafeRoutesEnabled) m
    else Future.successful(NotFound)
  }

  val routes: Route =
    cors(CorsSettings.defaultSettings.copy(allowCredentials = false)) {
      logRequestResult("i-have-money-write-frontend", Logging.InfoLevel) {
        (path("signIn") & post & entity(as[CreateUserRequest])) { req ⇒
          safe {
            writeBack.sendCommandAndIgnoreResult(UserId(req.email), CreateUser(req.password, req.displayName))
          }
        } ~
          (path("logIn") &
            post &
            entity(as[LogInRequest])) { req ⇒
            complete {
              val command = LoginUser(req.password)
              writeBack.sendCommand(UserId(req.email), command)((evt: UserEvent) ⇒ evt match {
                case e: UserLoggedIn ⇒ OK → CommandProcessedWithResult(command.id.value, Auth(req.email, e.displayName, tokenFactory.issue(req.email))).asJson
                case e: UserFailedToLogIn ⇒ Unauthorized → FailedRequest(
                  command.id.value.toString,
                  "Login failed: Invalid password"
                ).asJson
                case _ ⇒ unexpected
              })
            }
          } ~ path("confirmEmail") {
          get {
            parameters('email, 'code) { (email, code) =>
              safe {
                writeBack.sendCommandAndIgnoreResult(UserId(email), ConfirmEmail(code))
                  .map(_ ⇒
                    HttpResponse(
                      status = Found,
                      // todo: read frontend
                      headers = Location("/") :: Nil
                    )
                  )
              }
            }
          }
        } ~
          (path("resendConfirmationEmail") & post & entity(as[ResendConfirmationEmailRequest])) { req ⇒
            safe {
              writeBack.sendCommandAndIgnoreResult(UserId(req.email), ResendConfirmationEmail())
            }
          } ~ authenticateOrRejectWithChallenge(authenticator).optional {
          case Some(userId) ⇒
            pathPrefix("fortune") {
              (pathEndOrSingleSlash & post) {
                complete {
                  val fortuneId = FortuneId.generate
                  println(s"Generating new fortune with id: $fortuneId")
                  writeBack.sendCommandAndIgnoreResult(fortuneId, CreateFortune(userId))
                }
              } ~
                pathPrefix(JavaUUID.map(i ⇒ FortuneId(i.toString))) { fortuneId: FortuneId ⇒
                  (path("exchange") & post & entity(as[ExchangeCurrencyRequest])) { req ⇒
                    complete {
                      writeBack.sendCommandAndIgnoreResult(fortuneId, ExchangeCurrency(
                        userId,
                        req.fromAmount,
                        req.fromCurrency,
                        req.toAmount,
                        req.toCurrency,
                        req.comment
                      ))
                    }
                  } ~
                    (path("import") & post) {
                      fileUpload("transactions"){
                        case (metadata, byteSource) =>
                          val processingResult: Future[TSVImportResult] =
                            byteSource.via(Framing.delimiter(ByteString("\n"), 100000))
                                .mapConcat { line =>
                                  TSVTransactionsParser.parseLine(userId)(line.utf8String) match {
                                    case Failure(exception) =>
                                      println(s"$exception for ${line.utf8String}")
                                      Nil
                                    case util.Success(value) => value
                                  }
                                }
                                .mapAsync(4)(writeBack.sendCommandAndIgnoreResult(fortuneId, _))
                                .runFold(TSVImportResult(0, 0)) {
                                  case (acc, (code, _)) =>
                                    if (code == OK) acc.copy(success = acc.success + 1)
                                    else acc.copy(failure = acc.failure + 1)
                                }

                          complete(processingResult.map(r =>
                            if (r.success > 0) OK -> r.asJson
                            else BadRequest -> r.asJson
                          ))
                      }
                    }~
                    (path("correct") & post & entity(as[CorrectBalancesRequest])) { req ⇒
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, CorrectBalances(
                          userId,
                          req.realBalances,
                          req.comment
                        ))
                      }
                    } ~
                    (path("finish-initialization") & post) {
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, FinishInitialization(userId))
                      }
                    } ~
                    (path("editors") & post & entity(as[AddEditorRequest])) { req ⇒
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, AddEditor(userId, UserId(req.email)))
                      }
                    } ~
                    (path("income") & post & entity(as[ReceiveIncomeRequest])) { req ⇒
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, ReceiveIncome(
                          userId,
                          req.amount,
                          req.currency,
                          IncomeCategory(req.category),
                          req.initializer,
                          req.comment
                        ))
                      }
                    } ~
                    (path("spend") & post & entity(as[SpendRequest])) { req ⇒
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, Spend(
                          userId,
                          req.amount,
                          req.currency,
                          ExpenseCategory(req.category),
                          Some(req.date),
                          req.initializer,
                          req.comment
                        ))
                      }
                    } ~
                    (path("limit") & post & entity(as[UpdateLimitsRequest])) { req ⇒
                      complete {
                        writeBack.sendCommandAndIgnoreResult(fortuneId, UpdateLimits(
                          userId,
                          req.weekly,
                          req.monthly
                        ))
                      }
                    } ~
                    pathPrefix("assets") {
                      (pathEndOrSingleSlash & post & entity(as[BuyAssetRequest])) { req ⇒
                        complete {
                          writeBack.sendCommandAndIgnoreResult(fortuneId, BuyAsset(
                            userId,
                            req.asset,
                            req.initializer,
                            req.comment
                          ))
                        }
                      } ~
                        pathPrefix(JavaUUID.map(AssetId(_))) { assetId ⇒
                          (path("sell") & post & entity(as[SellAssetRequest])) { req ⇒
                            complete {
                              writeBack.sendCommandAndIgnoreResult(fortuneId, SellAsset(
                                userId,
                                assetId,
                                req.comment
                              ))
                            }
                          } ~
                            (path("reevaluate") & post & entity(as[ReevaluateAssetRequest])) { req ⇒
                              complete {
                                writeBack.sendCommandAndIgnoreResult(fortuneId, ReevaluateAsset(
                                  userId,
                                  assetId,
                                  req.newPrice,
                                  req.comment
                                ))
                              }
                            }
                        }
                    } ~
                    pathPrefix("liabilities") {
                      (pathEndOrSingleSlash & post & entity(as[TakeOnLiabilityRequest])) { req ⇒
                        complete {
                          writeBack.sendCommandAndIgnoreResult(fortuneId, TakeOnLiability(
                            userId,
                            req.liability,
                            req.initializer,
                            req.comment
                          ))
                        }
                      } ~
                        pathPrefix(JavaUUID.map(LiabilityId(_))) { liabilityId ⇒
                          (path("pay-off") & post & entity(as[PayLiabilityOffRequest])) { req ⇒
                            complete {
                              writeBack.sendCommandAndIgnoreResult(fortuneId, PayLiabilityOff(
                                userId,
                                liabilityId,
                                req.byAmount,
                                req.comment
                              ))
                            }
                          }
                        }
                    }
                }
            }
          case None ⇒
            complete(Unauthorized)
        }
      }
    }

  Http().bindAndHandle(routes, config.getString("app.host"), config.getInt("app.http-port"))
}
