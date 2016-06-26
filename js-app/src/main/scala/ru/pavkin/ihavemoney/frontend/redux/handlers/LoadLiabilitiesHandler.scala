package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Liability}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadLiabilities

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadLiabilitiesHandler[M](modelRW: ModelRW[M, Pot[Map[String, Liability]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadLiabilities =>
      val reloadF = a.effectXor(api.getLiabilities)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


