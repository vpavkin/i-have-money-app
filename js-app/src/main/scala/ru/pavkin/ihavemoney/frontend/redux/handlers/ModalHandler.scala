package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import japgolly.scalajs.react.ReactElement
import ru.pavkin.ihavemoney.frontend.redux.actions.{HideModal, ShowModal}

class ModalHandler[M](modelRW: ModelRW[M, Option[ReactElement]]) extends ActionHandler(modelRW) {

  override def handle = {
    case action: ShowModal =>
      updated(Some(action.modal))
    case HideModal ⇒
      updated(None)
  }
}
