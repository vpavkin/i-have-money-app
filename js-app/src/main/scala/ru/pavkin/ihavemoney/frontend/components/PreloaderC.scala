package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.all._

object PreloaderC {

  case class Props(addMod: TagMod*)

  val component = ReactComponentB[Props]("Preloader")
      .render_P(p ⇒ div(className := "loader", p.addMod))
      .build

  def apply(addMod: TagMod*) = component(Props(addMod))
}
