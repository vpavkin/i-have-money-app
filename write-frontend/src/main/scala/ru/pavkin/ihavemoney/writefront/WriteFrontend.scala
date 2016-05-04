package ru.pavkin.ihavemoney.writefront

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.CirceSupport
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.{ExpenseCategory, IncomeCategory, ReceiveIncome, Spend}
import ru.pavkin.ihavemoney.protocol.writefront._
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import akka.http.scaladsl.model.StatusCodes._
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.domain.unexpected
import ru.pavkin.ihavemoney.writefront.auth.JWTTokenFactory

import scala.concurrent.duration._

object WriteFrontend extends App with CirceSupport with CorsDirectives {

  implicit val system = ActorSystem("IHaveMoneyWriteFront")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(30.seconds)

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val writeBack = new WriteBackClusterClient(system)

  val tokenFactory: JWTTokenFactory = new JWTTokenFactory(config.getString("app.secret-key"))

  val routes: Route =
    cors(CorsSettings.defaultSettings.copy(allowCredentials = false)) {
      logRequestResult("i-have-money-write-frontend") {
        pathPrefix("fortune" / Segment) { fortuneId ⇒
          (path("income") & post & entity(as[ReceiveIncomeRequest])) { req ⇒
            complete {
              writeBack.sendCommandAndIgnoreResult(fortuneId, ReceiveIncome(
                req.amount,
                req.currency,
                IncomeCategory(req.category),
                req.comment
              ))
            }
          } ~ (path("spend") & post & entity(as[SpendRequest])) { req ⇒
            complete {
              writeBack.sendCommandAndIgnoreResult(fortuneId, Spend(
                req.amount,
                req.currency,
                ExpenseCategory(req.category),
                req.comment
              ))
            }
          }
        } ~
          (path("signIn") & post & entity(as[CreateUserRequest])) { req ⇒
            complete {
              writeBack.sendCommandAndIgnoreResult(req.email, CreateUser(req.password, req.displayName))
            }
          } ~
          (path("logIn") & post & entity(as[LogInRequest])) { req ⇒
            complete {
              val command = LoginUser(req.password)
              writeBack.sendCommand(req.email, command)((evt: UserEvent) ⇒ evt match {
                case e: UserLoggedIn ⇒ OK → RequestResult.success(command.id.value.toString, tokenFactory.issue(req.email))
                case e: UserFailedToLogIn ⇒ Unauthorized → RequestResult.failure(
                  command.id.value.toString,
                  "Login failed: Invalid password"
                )
                case _ ⇒ unexpected
              })
            }
          } ~
          (path("confirmEmail") & post & entity(as[ConfirmEmailRequest])) { req ⇒
            complete {
              writeBack.sendCommandAndIgnoreResult(req.email, ConfirmEmail(req.confirmationCode))
            }
          } ~
          (path("resendConfirmationEmail") & post & entity(as[ResendConfirmationEmailRequest])) { req ⇒
            complete {
              writeBack.sendCommandAndIgnoreResult(req.email, ResendConfirmationEmail)
            }
          } ~
          complete(NotFound)

      }
    }

  Http().bindAndHandle(routes, config.getString("app.host"), config.getInt("app.http-port"))
}
