package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadTransactionLog
import ru.pavkin.ihavemoney.protocol.Transaction

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadTransactionLogHandler[M](modelRW: ModelRW[M, Pot[List[Transaction]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadTransactionLog =>
      val reloadF = a.effectXor(api.getTransactionLog)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


