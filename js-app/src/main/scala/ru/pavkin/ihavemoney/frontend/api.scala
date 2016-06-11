package ru.pavkin.ihavemoney.frontend

import cats.data.Xor
import io.circe.syntax._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.BaseUrl
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.ajax.AjaxExtensions._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.protocol.{Auth, CommandProcessedWithResult, OtherError, RequestError}
import ru.pavkin.ihavemoney.protocol.readfront._
import ru.pavkin.ihavemoney.protocol.writefront._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object api {

  val readFrontBaseUrl = BaseUrl.fromWindowOrigin_/
  var writeFrontBaseUrl: BaseUrl = BaseUrl("")

  get((readFrontBaseUrl / "write_front_url").value).foreach {
    case Xor.Right(url) ⇒ writeFrontBaseUrl = BaseUrl(url).endWith_/
    case Xor.Left(error) ⇒ Callback.alert(s"Failed to obtain writeback url: $error").runNow
  }

  object routes {
    def login = writeFrontBaseUrl / "logIn"
    def register = writeFrontBaseUrl / "signIn"
    def addIncome(fortuneId: String) = writeFrontBaseUrl / "fortune" / fortuneId / "income"
    def addExpense(fortuneId: String) = writeFrontBaseUrl / "fortune" / fortuneId / "spend"
    def getBalances(fortuneId: String) = readFrontBaseUrl / "balance" / fortuneId
    def getFortunes = readFrontBaseUrl / "fortunes"
  }

  def authHeader = "Authorization" → s"Bearer ${AppCircuit.currentState.auth.map(_.token).getOrElse("")}"

  def fortunes(implicit ec: ExecutionContext): Future[Xor[RequestError, List[String]]] = expect[FrontendQueryResult](
    get(routes.getFortunes.value, headers = Map(authHeader)))
    .map(_.flatMap {
      case FrontendFortunes(_, fortunes) ⇒ Xor.Right(fortunes)
      case _ ⇒ Xor.Left(OtherError("Unexpected response"))
    })

  def login(email: String, password: String)(implicit ec: ExecutionContext): Future[Xor[RequestError, Auth]] = expect[CommandProcessedWithResult[Auth]](
    postJson(routes.login.value, LogInRequest(email, password))
  ).map(_.map(_.result))

  def register(email: String, password: String, displayName: String)(implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.register.value, CreateUserRequest(email, displayName, password)).map(_.map(_ ⇒ ()))

  def addIncome(id: String,
                amount: BigDecimal,
                currency: Currency,
                category: String,
                initializer: Boolean = false,
                comment: Option[String])
               (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addIncome(id).value,
      ReceiveIncomeRequest(amount, currency, category, initializer, comment).asJson.toString(),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addExpense(id: String,
                 amount: BigDecimal,
                 currency: Currency,
                 category: String,
                 initializer: Boolean = false,
                 comment: Option[String])
                (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addExpense(id).value,
      ReceiveIncomeRequest(amount, currency, category, initializer, comment).asJson.toString(),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def getBalances(id: String)(implicit ec: ExecutionContext): Future[RequestError Xor Map[String, BigDecimal]] =
    expect[FrontendQueryResult](get(routes.getBalances(id).value, headers = Map(authHeader)))
      .map(_.flatMap {
        case FrontendMoneyBalance(_, balances) ⇒ Xor.Right(balances)
        case _ ⇒ Xor.Left(OtherError("Unexpected response"))
      })
}
