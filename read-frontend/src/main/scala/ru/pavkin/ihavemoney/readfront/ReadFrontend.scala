package ru.pavkin.ihavemoney.readfront

import java.util.UUID

import io.circe.syntax._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.AskTimeoutException
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCredentials, OAuth2BearerToken}

import scala.concurrent.duration._
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.query._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.protocol.{Expense, FailedRequest, Income}
import ru.pavkin.ihavemoney.protocol
import ru.pavkin.ihavemoney.protocol.readfront._
import ru.pavkin.ihavemoney.auth.JWTTokenFactory
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.{CurrencyExchanged, FortuneIncreased, FortuneSpent}

import scala.concurrent.Future

object ReadFrontend extends App with FailFastCirceSupport {

  implicit val system = ActorSystem("IHaveMoneyReadFront")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(30.seconds)

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val readBack = new ReadBackClient(system, config.getString("read-backend.interface"))

  val writeFrontURL = s"http://${config.getString("write-frontend.host")}:${config.getString("write-frontend.port")}"

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

  def toFrontendEvents(r: EventLogQueryResult): FrontendEvents = FrontendEvents(
    r.id.value,
    r.events
        .sortBy(-_.metadata.date.toEpochSecond)
        .collect {
          case e: FortuneIncreased ⇒ Income(e.metadata.eventId.value, e.user.value, e.amount, e.currency, e.category.name, e.date.toLocalDate, e.comment)
          case e: FortuneSpent ⇒ Expense(e.metadata.eventId.value, e.user.value, -e.amount, e.currency, e.category.name, e.overrideDate.getOrElse(e.date.toLocalDate), e.comment)
          case e: CurrencyExchanged ⇒ protocol.CurrencyExchanged(e.metadata.eventId.value, e.user.value, e.fromAmount, e.fromCurrency, e.toAmount, e.toCurrency, e.date.toLocalDate, e.comment)
        }
        .sortBy(-_.date.toEpochDay)
  )

  def sendQuery(q: Query) =
    readBack.query(q)
        .map(kv ⇒ kv._2 match {
          case FortunesQueryResult(id, fortunes) ⇒
            kv._1 → (FrontendFortunes(id.value, fortunes): FrontendQueryResult).asJson
          case CategoriesQueryResult(id, inc, exp) ⇒
            kv._1 → (FrontendCategories(id.value, inc.map(_.name), exp.map(_.name)): FrontendQueryResult).asJson
          case r@EventLogQueryResult(id, events) ⇒
            kv._1 → (toFrontendEvents(r): FrontendQueryResult).asJson
          case MoneyBalanceQueryResult(id, balance) ⇒
            kv._1 → (FrontendMoneyBalance(id.value, balance): FrontendQueryResult).asJson
          case LiabilitiesQueryResult(id, liabilities) =>
            kv._1 → (FrontendLiabilities(id.value, liabilities): FrontendQueryResult).asJson
          case AssetsQueryResult(id, assets) =>
            kv._1 → (FrontendAssets(id.value, assets): FrontendQueryResult).asJson
          case e: AccessDenied ⇒
            kv._1 → FailedRequest(e.id.value.toString, e.error).asJson
          case e: EntityNotFound ⇒
            kv._1 → FailedRequest(e.id.value.toString, e.error).asJson
          case e: QueryFailed ⇒
            kv._1 → FailedRequest(e.id.value.toString, e.error).asJson
        })
        .recover {
          case timeout: AskTimeoutException ⇒
            RequestTimeout → FailedRequest(q.id.toString, s"Query ${q.id} timed out").asJson
        }

  val routes = {
    logRequestResult("i-have-money-read-frontend") {
      path("write_front_url") {
        get {
          complete {
            HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`), writeFrontURL))
          }
        }
      } ~
          authenticateOrRejectWithChallenge(authenticator) { userId ⇒
            path("fortunes") {
              get {
                complete {
                  sendQuery(Fortunes(QueryId(UUID.randomUUID.toString), userId))
                }
              }
            } ~
                pathPrefix("fortune" / JavaUUID.map(i ⇒ FortuneId(i.toString))) { fortuneId: FortuneId ⇒
                  get {
                    path("categories") {
                      complete {
                        sendQuery(Categories(QueryId(UUID.randomUUID.toString), userId, fortuneId))
                      }
                    } ~
                        path("balance") {
                          complete {
                            sendQuery(MoneyBalance(QueryId(UUID.randomUUID.toString), userId, fortuneId))
                          }
                        } ~
                        path("assets") {
                          complete {
                            sendQuery(Assets(QueryId(UUID.randomUUID.toString), userId, fortuneId))
                          }
                        } ~
                        path("liabilities") {
                          complete {
                            sendQuery(Liabilities(QueryId(UUID.randomUUID.toString), userId, fortuneId))
                          }
                        } ~
                        path("log") {
                          complete {
                            sendQuery(TransactionLog(QueryId(UUID.randomUUID.toString), userId, fortuneId))
                          }
                        }

                  }
                }
          } ~
          get {
            pathSingleSlash {
              getFromResource("index.html")
            }
          }
    } ~ getFromResourceDirectory("")
  }
  Http().bindAndHandle(routes, config.getString("app.host"), config.getInt("app.http-port"))
}
