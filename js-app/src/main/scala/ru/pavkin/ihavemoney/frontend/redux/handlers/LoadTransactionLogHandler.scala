package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadEventLog
import ru.pavkin.ihavemoney.protocol.Event

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadTransactionLogHandler[M](modelRW: ModelRW[M, Pot[List[Event]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadEventLog =>
      val reloadF = a.effectXor(api.getEventLog)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


