package ru.pavkin.ihavemoney.frontend.components


import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.frontend.{Route, api}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object NoFortuneC {

  type Props = RouterCtl[Route]
  type LoadingState = Boolean

  class Backend($: BackendScope[Props, LoadingState]) {

    def onButtonClick(router: Props) = Callback.future(
      api.createFortune.map {
        case Left(e) ⇒ Callback.alert(e.getMessage)
        case Right(_) ⇒
          // Callback(window.open(api.readFrontBaseUrl.value, "_self")).delayMs(2000).void
          $.setState(true) >> router.set(Route.Initializer).delayMs(2000).void
      }
    ).void

    def render(router: Props, isLoading: LoadingState) =
      if (isLoading)
        PreloaderC()
      else
        div(centered, button(common.buttonOpt(common.context.success), common.buttonLarge,
          onClick --> onButtonClick(router), "Create a Fortune"
        )): ReactElement
  }
  val component = ReactComponentB[Props]("NoFortune")
    .initialState(false)
    .renderBackend[Backend]
    .build
}
