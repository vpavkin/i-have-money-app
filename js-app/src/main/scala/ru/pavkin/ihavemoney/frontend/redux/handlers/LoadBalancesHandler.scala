package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.{ActionHandler, ModelRW}
import diode.data.{Pot, PotAction}
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadBalances
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadBalancesHandler[M](modelRW: ModelRW[M, Pot[Map[Currency, BigDecimal]]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadBalances =>
      val reloadF = a.effectEither(api.getBalances)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


