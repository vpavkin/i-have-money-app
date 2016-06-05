package ru.pavkin.ihavemoney.frontend.styles

import scalacss.Defaults._
import scala.language.postfixOps

object Global extends StyleSheet.Inline {

  import dsl._

  style(unsafeRoot("body")(
    paddingTop(80.px))
  )

  val GRID_SIZE = 12

  val bs = new BootstrapStyles(GRID_SIZE)

  val buttonMarginRight = style(marginRight(15 px))

  val centered = style(textAlign.center)
  val alignedRight = style(textAlign.right)
}
