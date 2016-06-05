package ru.pavkin.ihavemoney.frontend.redux

import diode.Circuit
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import ru.pavkin.ihavemoney.frontend.redux.handlers.AuthHandler
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.protocol.Auth

object AppCircuit extends Circuit[RootModel] {

  val LS_KEY: String = "i-have-money-auth"
  def tryGetAuthFromLocalStorage: Option[Auth] = decode[Auth](dom.window.localStorage.getItem(LS_KEY)).toOption
  def saveAuthToLocalStorage(auth: Auth): Unit = dom.window.localStorage.setItem(LS_KEY, auth.asJson.toString)

  protected def actionHandler: HandlerFunction = composeHandlers(
    new AuthHandler(zoomRW(_.auth)((m, v) => m.copy(auth = v)))
  )

  override def initialModel = RootModel(tryGetAuthFromLocalStorage)
}
