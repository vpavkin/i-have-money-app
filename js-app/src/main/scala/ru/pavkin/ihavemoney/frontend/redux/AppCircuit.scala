package ru.pavkin.ihavemoney.frontend.redux

import cats.syntax.eq._
import diode.Circuit
import diode.react.ReactConnector
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRate, ExchangeRates, FortuneInfo}
import ru.pavkin.ihavemoney.frontend.redux.handlers._
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.protocol.Auth

object AppCircuit extends Circuit[RootModel] with ReactConnector[RootModel] with CircuitHelpers[RootModel] {

  val LS_KEY: String = "i-have-money-auth"
  def tryGetAuthFromLocalStorage: Option[Auth] =
    decode[Auth](dom.window.localStorage.getItem(LS_KEY)).toOption

  def saveAuthToLocalStorage(auth: Auth): Unit =
    dom.window.localStorage.setItem(LS_KEY, auth.asJson.toString)

  def clearAuth(): Unit =
    dom.window.localStorage.removeItem(LS_KEY)

  protected def actionHandler: HandlerFunction = composeHandlers(
    new AuthHandler(zoomTo(_.auth)),
    new UpdateFortuneIdHandler(zoomTo(_.fortunes)),
    new LoadBalancesHandler(zoomTo(_.balances)),
    new LoadAssetsHandler(zoomTo(_.assets)),
    new LoadLiabilitiesHandler(zoomTo(_.liabilities)),
    new TransactionLogHandler(zoomTo(_.log)),
    new LoadCategoriesHandler(zoomTo(_.categories)),
    new LoadExchangeRatesHandler(zoomTo(_.exchangeRates)),
    new ModalHandler(zoomTo(_.modal)),
    new SendRequestHandler(zoomTo(_.activeRequest)),
    new InitializerRedirectsToHandler(zoomTo(_.initializerRedirectsTo)),

    new TransactionLogUIStateHandler(zoomTo(_.transactionLogUIState)),

    new YearlyReportsHandler(zoomRW(_ => ())((m, v) => m))
  )

  override def initialModel = RootModel(None)

  def auth: Option[Auth] = state.auth
  def fortunes: List[FortuneInfo] = state.fortunes.get
  def fortune: FortuneInfo = fortunes.head
  def fortuneId: String = fortune.id
}
