package ru.pavkin.ihavemoney.frontend.redux

import diode.react.ReactConnector
import diode.{Circuit, ModelR, ModelRO}
import io.circe.parser._
import io.circe.syntax._
import japgolly.scalajs.react.ReactElement
import org.scalajs.dom
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRate, FortuneInfo}
import ru.pavkin.ihavemoney.frontend.redux.actions.{HideModal, ShowModal}
import ru.pavkin.ihavemoney.frontend.redux.handlers._
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.protocol.Auth
import cats.syntax.eq._

object AppCircuit extends Circuit[RootModel] with ReactConnector[RootModel] with CircuitHelpers[RootModel] {

  val LS_KEY: String = "i-have-money-auth"
  def tryGetAuthFromLocalStorage: Option[Auth] =
    decode[Auth](dom.window.localStorage.getItem(LS_KEY)).toOption

  def saveAuthToLocalStorage(auth: Auth): Unit =
    dom.window.localStorage.setItem(LS_KEY, auth.asJson.toString)

  def clearAuth(): Unit =
    dom.window.localStorage.removeItem(LS_KEY)

  protected def actionHandler: HandlerFunction = composeHandlers(
    new AuthHandler(zoomRW(_.auth)((m, v) => m.copy(auth = v))),
    new UpdateFortuneIdHandler(zoomRW(_.fortunes)((m, v) => m.copy(fortunes = v))),
    new LoadBalancesHandler(zoomRW(_.balances)((m, v) => m.copy(balances = v))),
    new LoadAssetsHandler(zoomRW(_.assets)((m, v) => m.copy(assets = v))),
    new LoadLiabilitiesHandler(zoomRW(_.liabilities)((m, v) => m.copy(liabilities = v))),
    new LoadTransactionLogHandler(zoomRW(_.log)((m, v) => m.copy(log = v))),
    new LoadCategoriesHandler(zoomRW(_.categories)((m, v) => m.copy(categories = v))),
    new ModalHandler(zoomRW(_.modal)((m, v) => m.copy(modal = v))),
    new SendRequestHandler(zoomRW(_.activeRequest)((m, v) => m.copy(activeRequest = v))),
    new InitializerRedirectsToHandler(zoomRW(_.initializerRedirectsTo)((m, v) => m.copy(initializerRedirectsTo = v))),

    new TransactionLogUIStateHandler(zoomRW(_.transactionLogUIState)((m, v) => m.copy(transactionLogUIState = v)))
  )

  override def initialModel = RootModel(None)

  def auth: Option[Auth] = state.auth
  def fortunes: List[FortuneInfo] = state.fortunes.get
  def fortune: FortuneInfo = fortunes.head
  def fortuneId: String = fortune.id
  def exchangeRates: List[ExchangeRate] = state.exchangeRates.get

  def exchange(amount: BigDecimal, from: Currency, to: Currency): BigDecimal =
    exchangeRates.find(r => r.from === from && r.to === to).map(_.rate * amount)
      .orElse(exchangeRates.find(r =>
        r.from === to && r.to === from
      ).map(amount / _.rate)).get

}
