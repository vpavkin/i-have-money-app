package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadAssets, LoadBalances}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadAssetsHandler[M](modelRW: ModelRW[M, Pot[Map[String, Asset]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadAssets =>
      val reloadF = a.effectXor(api.getAssets)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


