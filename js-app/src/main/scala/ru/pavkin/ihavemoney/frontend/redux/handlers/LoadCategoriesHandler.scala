package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.data.{Pot, PotAction}
import diode.{ActionHandler, ModelRW}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadCategories
import ru.pavkin.ihavemoney.frontend.redux.model.Categories

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class LoadCategoriesHandler[M](modelRW: ModelRW[M, Pot[Categories]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: LoadCategories =>
      val reloadF = a.effectEither(api.getCategories)(identity(_))
      a.handleWith(this, reloadF)(PotAction.handler())
  }
}


