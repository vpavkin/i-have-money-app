package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoggedIn, LoggedOut}
import ru.pavkin.ihavemoney.protocol.Auth

class AuthHandler[M](modelRW: ModelRW[M, Option[Auth]]) extends ActionHandler(modelRW) {

  override def handle = {
    case action: LoggedIn =>
      AppCircuit.saveAuthToLocalStorage(action.auth)
      updated(Some(action.auth))
    case LoggedOut ⇒
      AppCircuit.clearAuth()
      updated(None)
  }
}
