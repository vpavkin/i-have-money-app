package ru.pavkin.ihavemoney.frontend.redux

import diode.Circuit
import diode.react.ReactConnector
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import ru.pavkin.ihavemoney.frontend.redux.actions.FortuneObtained
import ru.pavkin.ihavemoney.frontend.redux.handlers.{AuthHandler, FortuneObtainedHandler, LoadBalancesHandler}
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.protocol.Auth

object AppCircuit extends Circuit[RootModel] with ReactConnector[RootModel] {

  val LS_KEY: String = "i-have-money-auth"
  def tryGetAuthFromLocalStorage: Option[Auth] =
    decode[Auth](dom.window.localStorage.getItem(LS_KEY)).toOption

  def saveAuthToLocalStorage(auth: Auth): Unit =
    dom.window.localStorage.setItem(LS_KEY, auth.asJson.toString)

  protected def actionHandler: HandlerFunction = composeHandlers(
    new AuthHandler(zoomRW(_.auth)((m, v) => m.copy(auth = v))),
    new FortuneObtainedHandler(zoomRW(_.fortuneId)((m, v) => m.copy(fortuneId = v))),
    new LoadBalancesHandler(zoomRW(_.balances)((m, v) => m.copy(balances = v)))
  )

  override def initialModel = RootModel(None)

  def currentState = zoom(identity).value
  def auth = currentState.auth
  def fortune = currentState.fortuneId.get
}
