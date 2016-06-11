package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import diode.data.Pot
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{FortuneObtained, LogIn, LogOut}
import ru.pavkin.ihavemoney.protocol.Auth

class FortuneObtainedHandler[M](modelRW: ModelRW[M, Option[String]]) extends ActionHandler(modelRW) {

  override def handle = {
    case action: FortuneObtained =>
      updated(Some(action.fortuneId))
  }
}
