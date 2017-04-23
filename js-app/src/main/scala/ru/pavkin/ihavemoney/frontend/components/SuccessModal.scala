package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, ModalBackend}
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.ScalaCssReact._

object SuccessModal {

  case class Props(content: ReactElement)

  class Backend(scope: BackendScope[Props, Unit]) extends ModalBackend[Props, Unit](scope) {

    override def onClose: Callback = Callback.empty

    override def renderHeader(p: Props, s: Unit)(hide: Callback): TagMod =
      span(Button.close(hide), h2(strong("Request succeeded!", common.text(common.textStyle.success))))

    override def renderBody(pr: Props, s: Unit)(hide: Callback): TagMod = div(
      pr.content
    )

    override def renderFooter(p: Props, s: Unit)(hide: Callback): TagMod =
      Button(hide >> onClose, common.context.success,
        addStyles = Seq(common.buttonLarge),
        addAttributes = Seq(id := "success-modal-ok"))("OK")
  }

  val component = ReactComponentB[Props]("Success Modal")
    .renderBackend[Backend]
    .componentDidMount(_.backend.show())
    .build

  def apply(content: ReactElement) = component(Props(content))

}
