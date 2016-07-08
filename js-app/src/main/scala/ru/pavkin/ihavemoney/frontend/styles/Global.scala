package ru.pavkin.ihavemoney.frontend.styles

import scalacss.Defaults._
import scala.language.postfixOps

object Global extends StyleSheet.Inline {

  import dsl._

  style(unsafeRoot("body")(
    paddingTop(80.px))
  )

  val GRID_SIZE = 12
  val fontSizeBase = 12

  val common = new Base()
  val grid = new Grid(GRID_SIZE)

  val centered = style(textAlign.center)
  val alignedRight = style(textAlign.right)
  val rightMargin = style(marginRight(15 px))
  val bottomMargin = style(marginBottom(5 px))
  val topMargin = styleF(Domain.ofRange(5 to 100))(i ⇒ styleS(marginTop(i.px)))

  val increasedFontSize = style(
    fontSize(math.ceil(fontSizeBase * 1.4).toInt px)
  )

  val navbarTwoLineItem = style(
    paddingTop(13 px).important,
    paddingBottom(13 px).important
  )
  val loggedInAsNavbarItem = style(
    backgroundColor(common.colorPrimary).important
  )
  val horizontalFormStaticControl = common.formControlStatic + style(paddingBottom(0 px))

  val addonMainInput = style(
    height(48 px)
  )
  val inputCurrencyAddon = style(
    marginLeft(-1 px),
    borderBottomLeftRadius(0 px),
    borderTopLeftRadius(0 px)
  )
  val logNegAmount = style(
    fontWeight.bold,
    color(common.colorDanger)
  )
  val logPosAmount = style(
    fontWeight.bold,
    color(common.colorSuccess)
  )
}
