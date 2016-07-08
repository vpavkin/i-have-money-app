package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.all._

object PreloaderC {

  val component = ReactComponentB[Unit]("Preloader")
    .render(_ ⇒ div(className := "loader"))
    .build

  def apply() = component()
}
