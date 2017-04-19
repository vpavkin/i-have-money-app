package ru.pavkin.ihavemoney.frontend.components

import diode.data.{Empty, Failed, Pot, Ready}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.UpdateFortuneId
import ru.pavkin.ihavemoney.protocol.HTTPError

object InitializerC {

  case class Props(router: RouterCtl[Route])
  case class State(locations: Pot[List[String]])

  class Backend($: BackendScope[Props, State]) {

    def init(pr: Props) = Callback {
      AppCircuit.subscribeU(AppCircuit.zoom(_.fortunes))((unsubscribe, fortuneId) ⇒
        fortuneId.value match {
          case Empty ⇒
            println("Empty fortuneId pot in Initializer. Reloading")
            AppCircuit.dispatch(UpdateFortuneId())
          case Ready(x) ⇒
            unsubscribe()
            if (x.isEmpty)
              pr.router.set(Route.NoFortunes).runNow()
            else
              pr.router.set(AppCircuit.state.initializerRedirectsTo.getOrElse(Route.Expenses)).runNow()
          case Failed(HTTPError(401, _, _)) ⇒
            unsubscribe()
            pr.router.set(Route.Login).runNow()
          case _ ⇒ ()
        }
      )
      println("Hello!")
      AppCircuit.dispatch(UpdateFortuneId())
    }

    def render(pr: Props, st: State) = div(st.locations match {
      case Failed(HTTPError(401, _, _)) ⇒
        PreloaderC()
      case Failed(exception) ⇒
        FatalErrorC.component(exception.getMessage)
      case _ ⇒ PreloaderC()
    })
  }

  val component = ReactComponentB[Props]("Initializer")
    .initialState(State(Pot.empty))
    .renderBackend[Backend]
    .componentDidMount($ ⇒ $.backend.init($.props))
    .build

  def apply(router: RouterCtl[Route]) = component(Props(router))
}
