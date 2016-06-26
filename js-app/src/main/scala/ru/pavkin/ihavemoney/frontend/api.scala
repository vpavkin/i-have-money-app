package ru.pavkin.ihavemoney.frontend

import cats.data.Xor
import io.circe.syntax._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.BaseUrl
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency}
import ru.pavkin.ihavemoney.frontend.ajax.AjaxExtensions._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.protocol._
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
    def fortune = writeFrontBaseUrl / "fortune"
    def addIncome(fortuneId: String) = fortune / fortuneId / "income"
    def addExpense(fortuneId: String) = fortune / fortuneId / "spend"
    def assets(fortuneId: String) = fortune / fortuneId / "assets"

    def readFortune = readFrontBaseUrl / "fortune"
    def getFortunes = readFrontBaseUrl / "fortunes"
    def getBalances(fortuneId: String) = readFortune / fortuneId / "balance"
    def getTransactionLog(fortuneId: String) = readFortune / fortuneId / "log"
  }

  def authHeader = "Authorization" → s"Bearer ${AppCircuit.auth.map(_.token).getOrElse("")}"

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

  def createFortune: Future[Xor[RequestError, Unit]] =
    postEmpty(routes.fortune.value, headers = Map(authHeader))
      .map(_.map(_ ⇒ ()))

  def addIncome(amount: BigDecimal,
                currency: Currency,
                category: String,
                initializer: Boolean = false,
                comment: Option[String])
               (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addIncome(AppCircuit.fortune).value,
      ReceiveIncomeRequest(amount, currency, category, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addExpense(amount: BigDecimal,
                 currency: Currency,
                 category: String,
                 initializer: Boolean = false,
                 comment: Option[String])
                (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addExpense(AppCircuit.fortune).value,
      SpendRequest(amount, currency, category, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def buyAsset(asset: Asset,
               initializer: Boolean = false,
               comment: Option[String])
              (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.assets(AppCircuit.fortune).value,
      BuyAssetRequest(asset, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def getBalances(implicit ec: ExecutionContext): Future[RequestError Xor Map[Currency, BigDecimal]] =
    expect[FrontendQueryResult](get(routes.getBalances(AppCircuit.fortune).value, headers = Map(authHeader)))
      .map(_.flatMap {
        case FrontendMoneyBalance(_, balances) ⇒ Xor.Right(balances)
        case _ ⇒ Xor.Left(OtherError("Unexpected response"))
      })

  def getTransactionLog(implicit ec: ExecutionContext): Future[RequestError Xor List[Transaction]] =
    expect[FrontendQueryResult](get(routes.getTransactionLog(AppCircuit.fortune).value, headers = Map(authHeader)))
      .map(_.flatMap {
        case FrontendTransactions(_, transactions) ⇒ Xor.Right(transactions)
        case _ ⇒ Xor.Left(OtherError("Unexpected response"))
      })
}
