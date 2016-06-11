package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LogIn
import ru.pavkin.ihavemoney.frontend.redux.model.RootModel
import ru.pavkin.ihavemoney.frontend.{Route, api}
import ru.pavkin.ihavemoney.protocol.Auth

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object PreloaderComponent {

  case class Props(router: RouterCtl[Route], proxy: ModelProxy[RootModel])

  class Backend($: BackendScope[Props, Unit]) {

    def loadFortune(router: RouterCtl[Route], auth: Auth) = Callback.future(
      api.fortunes.map {
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(l) ⇒
          Callback {
            // todo: dispatch to circuit
            // AppCircuit.dispatch(LogIn(auth))
          }.flatMap(_ ⇒
            if (l.isEmpty)
              Callback.alert("Show button to add fortune")
            else
              router.set(Route.AddTransactions)
          )
      }
    )

    def render(router: Props) =
      h1("Loading...")

  }

  val component = ReactComponentB[Props]("Preloader")
    .renderBackend[Backend]
    .componentDidMount($ ⇒
      $.props.proxy().auth
        .orElse(AppCircuit.tryGetAuthFromLocalStorage) match {
        case Some(a) ⇒
          AppCircuit.dispatch(LogIn(a))
          $.backend.loadFortune($.props.router, a)
        // doesn't work without delay
        case None ⇒ $.props.router.set(Route.Login).delayMs(100).void
      })
    .build
}
