package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import diode.data._
import ru.pavkin.ihavemoney.frontend.redux.actions.SendRequest

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class SendRequestHandler[M](modelRW: ModelRW[M, Pot[Unit]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: SendRequest =>
      val reloadF = a.effectXor(a.command)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}
