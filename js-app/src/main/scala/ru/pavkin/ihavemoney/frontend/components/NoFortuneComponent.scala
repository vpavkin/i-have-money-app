package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.{Route, api}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scalacss.ScalaCssReact._

object NoFortuneComponent {

  type Props = RouterCtl[Route]

  def onButtonClick(router: Props) = Callback.future(
    api.createFortune.map {
      case Xor.Left(e) ⇒ Callback.alert(e.getMessage)
      case Xor.Right(_) ⇒ router.set(Route.Preloader).delayMs(2000).void
    }
  ).void

  val component = ReactComponentB[Props]("NoFortune")
    .render_P(router ⇒ div(centered, button(bs.buttonOpt(bs.common.success), bs.buttonLG,
      onClick --> onButtonClick(router), "Create a Fortune"
    )))
    .build
}
