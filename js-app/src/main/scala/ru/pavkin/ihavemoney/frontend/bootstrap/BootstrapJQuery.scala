package ru.pavkin.ihavemoney.frontend.bootstrap

import org.querki.jquery.{$, JQuery, JQueryEventObject}
import org.scalajs.dom

import scala.language.implicitConversions
import scala.scalajs.js

@js.native
trait BootstrapJQuery extends JQuery {
  def tab(options: js.Any): BootstrapJQuery = js.native
  def carousel(options: js.Any): BootstrapJQuery = js.native
  def modal(action: String): BootstrapJQuery = js.native
  def modal(options: js.Any): BootstrapJQuery = js.native
  def tooltip(options: js.Any): BootstrapJQuery = js.native
  def popover(options: js.Any): BootstrapJQuery = js.native
  def collapse(action: String): BootstrapJQuery = js.native
  def collapse(options: js.Any): BootstrapJQuery = js.native
}

object BootstrapJQuery {
  implicit def bootstrapJquery(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]

  def showModal(element: dom.Element,
                onHiddenHandler: JQueryEventObject ⇒ js.Any = _ ⇒ js.undefined,
                backdrop: Boolean = true,
                keyboard: Boolean = true): Unit =
    $(element)
      .modal(js.Dynamic.literal(backdrop = backdrop, keyboard = keyboard, show = true))
      .on("hidden.bs.modal", null, null, onHiddenHandler)

  def hideModal(element: dom.Element): Unit =
    $(element).modal("hide")

  def collapse(element: dom.Element): Unit =
    $(element).collapse("hide")
}
