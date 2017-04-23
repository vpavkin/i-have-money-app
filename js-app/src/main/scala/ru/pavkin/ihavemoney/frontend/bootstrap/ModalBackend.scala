package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.all._
import org.querki.jquery.JQueryEventObject

import scala.scalajs.js
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

abstract class ModalBackend[Props, State]($: BackendScope[Props, State]) {

  def onClose: Callback
  def renderHeader(p: Props, s: State)(hide: Callback): TagMod
  def renderBody(p: Props, s: State)(hide: Callback): TagMod
  def renderFooter(p: Props, s: State)(hide: Callback): TagMod

  def hide = Callback {
    BootstrapJQuery.hideModal($.getDOMNode())
  }

  private def close(e: JQueryEventObject): js.Any =
    onClose.runNow()

  def show(backdrop: Boolean = true, keyboard: Boolean = true) = Callback {
    BootstrapJQuery.showModal($.getDOMNode(), close, backdrop, keyboard)
  }

  def render(props: Props, state: State) = {
    val m = common.modal
    div(m.modal, m.fade, role := "dialog", aria.hidden := true,
      div(m.dialog,
        div(m.content,
          div(m.header, renderHeader(props, state)(hide)),
          div(m.body, renderBody(props, state)(hide)),
          div(m.footer, renderFooter(props, state)(hide))
        )
      )
    )
  }
}
