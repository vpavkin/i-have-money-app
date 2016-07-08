package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode._
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.frontend.redux.actions.SetInitializerRedirect

class InitializerRedirectsToHandler[M](modelRW: ModelRW[M, Option[Route]]) extends ActionHandler(modelRW) {

  override def handle = {
    case a: SetInitializerRedirect =>
      println(s"Initializer redirect is set to ${a.route}")
      updated(Some(a.route))
  }
}
