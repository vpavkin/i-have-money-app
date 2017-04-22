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

  val centered: StyleA = style(textAlign.center)
  val alignedRight: StyleA = style(textAlign.right)
  val rightMargin: StyleA = style(marginRight(15 px))
  val bottomMargin: StyleA = style(marginBottom(5 px))
  val topMargin: (Int) => StyleA = styleF(Domain.ofRange(5 to 100))(i ⇒ styleS(marginTop(i.px)))

  val increasedFontSize: StyleA = style(
    fontSize(math.ceil(fontSizeBase * 1.4).toInt px)
  )

  val navbarTwoLineItem: StyleA = style(
    paddingTop(13 px).important,
    paddingBottom(13 px).important
  )
  val loggedInAsNavbarItem: StyleA = style(
    backgroundColor(common.colorPrimary).important
  )
  val horizontalFormStaticControl: StyleA = common.formControlStatic + style(paddingBottom(0 px))

  val addonMainInput: StyleA = style(
    height(48 px)
  )
  val inputCurrencyAddon: StyleA = style(
    marginLeft(-1 px),
    borderBottomLeftRadius(0 px),
    borderTopLeftRadius(0 px)
  )
  val logNegAmount: StyleA = style(
    fontWeight.bold,
    color(common.colorDanger)
  )
  val logPosAmount: StyleA = style(
    fontWeight.bold,
    color(common.colorSuccess)
  )

  val commonFooter: StyleA = style(
    fontSize(math.ceil(fontSizeBase * 0.7).toInt.px),
    bottom(0.px),
    width(100.%%),
    height(14.px),
    paddingRight(5.px),
    textAlign.right
  )
}
