package ru.pavkin.ihavemoney.frontend

import java.time.{LocalDate, Year}
import java.util.UUID

import io.circe.Decoder
import io.circe.syntax._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.BaseUrl
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.frontend.ajax.AjaxExtensions._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.protocol._
import ru.pavkin.ihavemoney.protocol.readfront._
import ru.pavkin.ihavemoney.protocol.writefront._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object api {

  val readFrontBaseUrl: BaseUrl = BaseUrl.fromWindowOrigin_/
  var writeFrontBaseUrl: BaseUrl = BaseUrl("")

  get((readFrontBaseUrl / "write_front_url").value).foreach {
    case Right(url) ⇒ writeFrontBaseUrl = BaseUrl(url).endWith_/
    case Left(error) ⇒ Callback.alert(s"Failed to obtain writeback url: $error").runNow
  }

  object routes {
    def login: BaseUrl = writeFrontBaseUrl / "logIn"
    def register: BaseUrl = writeFrontBaseUrl / "signIn"
    def fortune: BaseUrl = writeFrontBaseUrl / "fortune"
    def editors(fortuneId: String): BaseUrl = fortune / fortuneId / "editors"
    def limit(fortuneId: String): BaseUrl = fortune / fortuneId / "limit"
    def cancel(fortuneId: String): BaseUrl = fortune / fortuneId / "cancel"
    def finishInit(fortuneId: String): BaseUrl = fortune / fortuneId / "finish-initialization"
    def addIncome(fortuneId: String): BaseUrl = fortune / fortuneId / "income"
    def addExpense(fortuneId: String): BaseUrl = fortune / fortuneId / "spend"
    def exchange(fortuneId: String): BaseUrl = fortune / fortuneId / "exchange"
    def correct(fortuneId: String): BaseUrl = fortune / fortuneId / "correct"
    def assets(fortuneId: String): BaseUrl = fortune / fortuneId / "assets"

    def readFortune: BaseUrl = readFrontBaseUrl / "fortune"
    def getFortunes: BaseUrl = readFrontBaseUrl / "fortunes"
    def getBalances(fortuneId: String): BaseUrl = readFortune / fortuneId / "balance"
    def getEventLog(fortuneId: String, year: Year): BaseUrl = readFortune / fortuneId / "log" / year.getValue.toString
    def getAssets(fortuneId: String): BaseUrl = readFortune / fortuneId / "assets"
    def getLiabilities(fortuneId: String): BaseUrl = readFortune / fortuneId / "liabilities"
    def getCategories(fortuneId: String): BaseUrl = readFortune / fortuneId / "categories"

    def generateReport(fortuneId: String, year: Year): BaseUrl = readFortune / fortuneId / "reports" / "yearly" / year.toString
  }

  def authHeader: (String, String) = "Authorization" → s"Bearer ${AppCircuit.auth.map(_.token).getOrElse("")}"

  def login(
    email: String,
    password: String)(
    implicit ec: ExecutionContext): Future[Either[RequestError, Auth]] =
    expect[CommandProcessedWithResult[Auth]](
      postJson(routes.login.value, LogInRequest(email, password))
    ).map(_.map(_.result))

  def register(email: String, password: String, displayName: String)(implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.register.value, CreateUserRequest(email, displayName, password)).map(_.map(_ ⇒ ()))

  def createFortune: Future[Either[RequestError, Unit]] =
    postEmpty(routes.fortune.value, headers = Map(authHeader))
      .map(_.map(_ ⇒ ()))

  def addIncome(
    amount: BigDecimal,
    currency: Currency,
    category: IncomeCategory,
    initializer: Boolean = false,
    comment: Option[String])
    (implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.addIncome(AppCircuit.fortuneId).value,
      ReceiveIncomeRequest(amount, currency, category, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addExpense(
    amount: BigDecimal,
    currency: Currency,
    category: ExpenseCategory,
    date: LocalDate,
    initializer: Boolean = false,
    comment: Option[String])
    (implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.addExpense(AppCircuit.fortuneId).value,
      SpendRequest(amount, currency, category, date, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def correct(
    newAmount: BigDecimal,
    newCurrency: Currency,
    comment: Option[String] = None)
    (implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.correct(AppCircuit.fortuneId).value,
      CorrectBalancesRequest(Map(newCurrency → newAmount), comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def exchange(
    fromAmount: BigDecimal,
    fromCurrency: Currency,
    toAmount: BigDecimal,
    toCurrency: Currency,
    comment: Option[String])
    (implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.exchange(AppCircuit.fortuneId).value,
      ExchangeCurrencyRequest(fromAmount, fromCurrency, toAmount, toCurrency, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def buyAsset(
    asset: Asset,
    initializer: Boolean = false,
    comment: Option[String])
    (implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.assets(AppCircuit.fortuneId).value,
      BuyAssetRequest(asset, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def updateLimits(
    weekly: Map[ExpenseCategory, Worth],
    monthly: Map[ExpenseCategory, Worth])(implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.limit(AppCircuit.fortuneId).value,
      UpdateLimitsRequest(weekly, monthly),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def cancelTransaction(id: UUID)(implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.cancel(AppCircuit.fortuneId).value,
      CancelTransactionRequest(id),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addEditor(email: String)(implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postJson(routes.editors(AppCircuit.fortuneId).value,
      AddEditorRequest(email),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def finishInitialization(implicit ec: ExecutionContext): Future[Either[RequestError, Unit]] =
    postEmpty(routes.finishInit(AppCircuit.fortuneId).value,
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  // queries

  def query[A](
    route: BaseUrl,
    extractor: PartialFunction[FrontendQueryResult, A])(
    implicit ec: ExecutionContext): Future[Either[RequestError, A]] = expect[FrontendQueryResult](
    get(route.value, headers = Map(authHeader))
  ).map(_.flatMap { res ⇒
    extractor.andThen(Right(_)).applyOrElse(res, (_: FrontendQueryResult) ⇒ Left(OtherError("Unexpected response")))
  })

  def fortunes(implicit ec: ExecutionContext): Future[Either[RequestError, List[FortuneInfo]]] = query(routes.getFortunes, {
    case FrontendFortunes(_, fortunes) ⇒ fortunes
  })

  def getCategories(implicit ec: ExecutionContext): Future[RequestError Either Categories] =
    query(routes.getCategories(AppCircuit.fortuneId), {
      case FrontendCategories(_, income, expenses) ⇒ Categories(income, expenses)
    })

  def getBalances(implicit ec: ExecutionContext): Future[RequestError Either Map[Currency, BigDecimal]] =
    query(routes.getBalances(AppCircuit.fortuneId), {
      case FrontendMoneyBalance(_, balances) ⇒ balances
    })

  def getAssets(implicit ec: ExecutionContext): Future[RequestError Either Map[String, Asset]] =
    query(routes.getAssets(AppCircuit.fortuneId), {
      case FrontendAssets(_, assets) ⇒ assets
    })

  def getLiabilities(implicit ec: ExecutionContext): Future[RequestError Either Map[String, Liability]] =
    query(routes.getLiabilities(AppCircuit.fortuneId), {
      case FrontendLiabilities(_, liabilities) ⇒ liabilities
    })

  def getEventLog(year: Year)(implicit ec: ExecutionContext): Future[RequestError Either List[Event]] =
    query(routes.getEventLog(AppCircuit.fortuneId, year), {
      case FrontendEvents(_, transactions) ⇒ transactions
    })

  // reports

  def generateYearlyReport(year: Year)(implicit ec: ExecutionContext): Future[RequestError Either String] =
    get(routes.generateReport(AppCircuit.fortuneId, year).value, headers = Map(authHeader))

}
