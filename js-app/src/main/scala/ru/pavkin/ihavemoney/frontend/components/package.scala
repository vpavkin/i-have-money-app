package ru.pavkin.ihavemoney.frontend

import ru.pavkin.ihavemoney.frontend.styles.Global.{logNegAmount, logPosAmount}

package object components {

  def amountStyle(amount: BigDecimal): scalacss.Defaults.StyleA =
    if (amount >= 0) logPosAmount
    else logNegAmount

}
