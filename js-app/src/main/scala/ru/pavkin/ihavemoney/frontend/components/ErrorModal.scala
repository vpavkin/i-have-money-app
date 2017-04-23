package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, ModalBackend}
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.ScalaCssReact._

object ErrorModal {

  case class Props(exception: Throwable)

  class Backend(scope: BackendScope[Props, Unit]) extends ModalBackend[Props, Unit](scope) {

    override def onClose: Callback = Callback.empty

    override def renderHeader(p: Props, s: Unit)(hide: Callback): TagMod =
      span(Button.close(hide), h2(strong("Error!", common.text(common.textStyle.danger))))

    override def renderBody(pr: Props, s: Unit)(hide: Callback): TagMod = div(
      div(id := "application-error-modal-error-text", increasedFontSize, pr.exception.getMessage)
    )

    override def renderFooter(p: Props, s: Unit)(hide: Callback): TagMod =
      Button(hide >> onClose, common.context.danger, addStyles = Seq(common.buttonLarge))("OK")
  }

  val component = ReactComponentB[Props]("Error Modal")
    .renderBackend[Backend]
    .componentDidMount(_.backend.show())
    .build

  def apply(exception: Throwable) = component(Props(exception))

}
