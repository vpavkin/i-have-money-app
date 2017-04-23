package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.{ActionHandler, ActionResult, ModelRW}
import ru.pavkin.ihavemoney.frontend.components.state.TransactionLogUIState
import ru.pavkin.ihavemoney.frontend.redux.actions.SetTransactionLogUIState


class TransactionLogUIStateHandler[M](modelRW: ModelRW[M, TransactionLogUIState])
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case a: SetTransactionLogUIState =>
      updated(a.uiState)

  }

}
