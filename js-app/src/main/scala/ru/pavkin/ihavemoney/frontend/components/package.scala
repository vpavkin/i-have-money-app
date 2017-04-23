package ru.pavkin.ihavemoney.frontend

import ru.pavkin.ihavemoney.frontend.styles.Global.{logNegAmount, logPosAmount}

import scala.math.BigDecimal.RoundingMode

package object components {

  def amountStyle(amount: BigDecimal): scalacss.Defaults.StyleA =
    if (amount >= 0) logPosAmount
    else logNegAmount

  def renderAmount(a: BigDecimal): String = a.setScale(2, RoundingMode.HALF_UP).toString()
}
