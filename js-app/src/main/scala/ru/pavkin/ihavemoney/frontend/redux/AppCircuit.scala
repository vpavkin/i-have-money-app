package ru.pavkin.ihavemoney.frontend.redux

import diode.react.ReactConnector
import diode.{Circuit, ModelR}
import io.circe.parser._
import io.circe.syntax._
import japgolly.scalajs.react.ReactElement
import org.scalajs.dom
import ru.pavkin.ihavemoney.frontend.redux.actions.{HideModal, ShowModal}
import ru.pavkin.ihavemoney.frontend.redux.handlers.{AuthHandler, InitializerRedirectsToHandler, LoadAssetsHandler, LoadBalancesHandler, LoadLiabilitiesHandler, LoadTransactionLogHandler, ModalHandler, SendRequestHandler, UpdateFortuneIdHandler}
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.protocol.Auth

object AppCircuit extends Circuit[RootModel] with ReactConnector[RootModel] {

  type Unsubscriber = () ⇒ Unit

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
    new ModalHandler(zoomRW(_.modal)((m, v) => m.copy(modal = v))),
    new SendRequestHandler(zoomRW(_.activeRequest)((m, v) => m.copy(activeRequest = v))),
    new InitializerRedirectsToHandler(zoomRW(_.initializerRedirectsTo)((m, v) => m.copy(initializerRedirectsTo = v)))
  )

  override def initialModel = RootModel(None)

  def subscribeU[T](cursor: ModelR[RootModel, T])(listener: (Unsubscriber, ModelR[RootModel, T]) => Unit): Unit = {
    var unsubscribe: () ⇒ Unit = () ⇒ ()
    unsubscribe = subscribe(cursor)(m ⇒ listener(unsubscribe, m))
  }

  def state = zoom(identity).value
  def stateCursor = zoom(identity(_))
  def auth = state.auth
  def fortunes = state.fortunes.get
  def fortune = fortunes.head

  def showModal(modal: ReactElement) = dispatch(ShowModal(modal))
  def hideModal() = dispatch(HideModal)
}
