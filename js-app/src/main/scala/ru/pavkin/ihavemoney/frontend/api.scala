package ru.pavkin.ihavemoney.frontend

import cats.data.Xor
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
    def editors(fortuneId: String) = fortune / fortuneId / "editors"
    def limit(fortuneId: String) = fortune / fortuneId / "limit"
    def finishInit(fortuneId: String) = fortune / fortuneId / "finish-initialization"
    def addIncome(fortuneId: String) = fortune / fortuneId / "income"
    def addExpense(fortuneId: String) = fortune / fortuneId / "spend"
    def exchange(fortuneId: String) = fortune / fortuneId / "exchange"
    def correct(fortuneId: String) = fortune / fortuneId / "correct"
    def assets(fortuneId: String) = fortune / fortuneId / "assets"

    def readFortune = readFrontBaseUrl / "fortune"
    def getFortunes = readFrontBaseUrl / "fortunes"
    def getBalances(fortuneId: String) = readFortune / fortuneId / "balance"
    def getEventLog(fortuneId: String) = readFortune / fortuneId / "log"
    def getAssets(fortuneId: String) = readFortune / fortuneId / "assets"
    def getLiabilities(fortuneId: String) = readFortune / fortuneId / "liabilities"
    def getCategories(fortuneId: String) = readFortune / fortuneId / "categories"
  }

  def authHeader = "Authorization" → s"Bearer ${AppCircuit.auth.map(_.token).getOrElse("")}"

  def login(email: String, password: String)(implicit ec: ExecutionContext): Future[Xor[RequestError, Auth]] = expect[CommandProcessedWithResult[Auth]](
    postJson(routes.login.value, LogInRequest(email, password))
  ).map(_.map(_.result))

  def register(email: String, password: String, displayName: String)(implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.register.value, CreateUserRequest(email, displayName, password)).map(_.map(_ ⇒ ()))

  def createFortune: Future[Xor[RequestError, Unit]] =
    postEmpty(routes.fortune.value, headers = Map(authHeader))
        .map(_.map(_ ⇒ ()))

  def addIncome(
      amount: BigDecimal,
      currency: Currency,
      category: String,
      initializer: Boolean = false,
      comment: Option[String])
      (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addIncome(AppCircuit.fortuneId).value,
      ReceiveIncomeRequest(amount, currency, category, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addExpense(
      amount: BigDecimal,
      currency: Currency,
      category: String,
      initializer: Boolean = false,
      comment: Option[String])
      (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.addExpense(AppCircuit.fortuneId).value,
      SpendRequest(amount, currency, category, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def correct(
      newAmount: BigDecimal,
      newCurrency: Currency,
      comment: Option[String] = None)
      (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
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
      (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.exchange(AppCircuit.fortuneId).value,
      ExchangeCurrencyRequest(fromAmount, fromCurrency, toAmount, toCurrency, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def buyAsset(
      asset: Asset,
      initializer: Boolean = false,
      comment: Option[String])
      (implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.assets(AppCircuit.fortuneId).value,
      BuyAssetRequest(asset, initializer, comment),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def updateLimits(weekly: Map[ExpenseCategory, Worth], monthly: Map[ExpenseCategory, Worth])(implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.limit(AppCircuit.fortuneId).value,
      UpdateLimitsRequest(weekly, monthly),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def addEditor(email: String)(implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postJson(routes.editors(AppCircuit.fortuneId).value,
      AddEditorRequest(email),
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  def finishInitialization(implicit ec: ExecutionContext): Future[Xor[RequestError, Unit]] =
    postEmpty(routes.finishInit(AppCircuit.fortuneId).value,
      headers = Map(authHeader)
    ).map(_.map(_ ⇒ ()))

  // queries

  def query[A](route: BaseUrl, extractor: PartialFunction[FrontendQueryResult, A])
      (implicit ec: ExecutionContext): Future[Xor[RequestError, A]] = expect[FrontendQueryResult](
    get(route.value, headers = Map(authHeader))
  ).map(_.flatMap { res ⇒
    extractor.andThen(Xor.Right(_)).applyOrElse(res, (_: FrontendQueryResult) ⇒ Xor.Left(OtherError("Unexpected response")))
  })

  def fortunes(implicit ec: ExecutionContext): Future[Xor[RequestError, List[FortuneInfo]]] = query(routes.getFortunes, {
    case FrontendFortunes(_, fortunes) ⇒ fortunes
  })

  def getCategories(implicit ec: ExecutionContext): Future[RequestError Xor Categories] =
    query(routes.getCategories(AppCircuit.fortuneId), {
      case FrontendCategories(_, income, expenses) ⇒ Categories(income, expenses)
    })

  def getBalances(implicit ec: ExecutionContext): Future[RequestError Xor Map[Currency, BigDecimal]] =
    query(routes.getBalances(AppCircuit.fortuneId), {
      case FrontendMoneyBalance(_, balances) ⇒ balances
    })

  def getAssets(implicit ec: ExecutionContext): Future[RequestError Xor Map[String, Asset]] =
    query(routes.getAssets(AppCircuit.fortuneId), {
      case FrontendAssets(_, assets) ⇒ assets
    })

  def getLiabilities(implicit ec: ExecutionContext): Future[RequestError Xor Map[String, Liability]] =
    query(routes.getLiabilities(AppCircuit.fortuneId), {
      case FrontendLiabilities(_, liabilities) ⇒ liabilities
    })

  def getEventLog(implicit ec: ExecutionContext): Future[RequestError Xor List[Event]] =
    query(routes.getEventLog(AppCircuit.fortuneId), {
      case FrontendEvents(_, transactions) ⇒ transactions
    })
}
