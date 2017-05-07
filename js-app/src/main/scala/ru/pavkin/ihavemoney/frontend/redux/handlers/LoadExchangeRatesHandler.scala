package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.domain.fortune.ExchangeRates
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadExchangeRates}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadExchangeRatesHandler[M](modelRW: ModelRW[M, Pot[ExchangeRates]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadExchangeRates =>
      val reloadF = a.effectEither(api.getExchangeRates)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


