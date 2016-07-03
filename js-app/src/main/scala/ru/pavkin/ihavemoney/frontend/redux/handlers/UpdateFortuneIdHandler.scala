package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import diode.data.{Pot, PotAction}
import ru.pavkin.ihavemoney.domain.fortune.FortuneInfo
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.UpdateFortuneId

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateFortuneIdHandler[M](modelRW: ModelRW[M, Pot[List[FortuneInfo]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case action: UpdateFortuneId =>
      val reloadF = action.effectXor(api.fortunes)(identity)
      action.handleWith(this, reloadF)(PotAction.handler())
  }
}
