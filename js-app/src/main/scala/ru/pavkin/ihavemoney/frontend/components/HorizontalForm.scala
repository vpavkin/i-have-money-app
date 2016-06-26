package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.vdom.ReactTagOf
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.Label
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

object HorizontalForm {

  val LABEL_WIDTH = 2

  object Label {
    def apply(text: String): ReactTagOf[Label] = Label(text, "")
    def apply(text: String, forInput: String): ReactTagOf[Label] =
      label(className := "control-label", grid.columnMD(LABEL_WIDTH), htmlFor := forInput, text)
  }

  val input = grid.columnMD(GRID_SIZE - LABEL_WIDTH)
  def shortInput(shortenBy: Int) = grid.columnMD(GRID_SIZE - LABEL_WIDTH - shortenBy)
}
