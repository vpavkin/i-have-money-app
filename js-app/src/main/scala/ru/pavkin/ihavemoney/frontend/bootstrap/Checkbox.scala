package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.{Callback, _}
import japgolly.scalajs.react.vdom.all._

object Checkbox {

  def onChecked(onCheckedChange: Boolean ⇒ Callback)(e: ReactEventI): Callback =
    onCheckedChange(e.target.checked)

  def apply(onCheckedChange: Boolean ⇒ Callback, isChecked: Boolean, children: TagMod*) = div(className := "checkbox",
    label(
      input.checkbox(onChange ==> onChecked(onCheckedChange)), value := isChecked, checked := isChecked, children
    )
  )
}
