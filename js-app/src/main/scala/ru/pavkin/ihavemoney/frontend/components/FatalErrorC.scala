package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.all._

object FatalErrorC {

  val component = ReactComponentB[String]("FatalError")
    .render_P(p ⇒ div(
      h1(s"Fatal error occured. Try to refresh the page."),
      h1(s"Error: $p")
    ))
    .build
}
