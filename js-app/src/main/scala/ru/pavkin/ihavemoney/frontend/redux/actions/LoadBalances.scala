package ru.pavkin.ihavemoney.frontend.redux.actions

import diode.data.{Pot, PotAction}
import ru.pavkin.ihavemoney.domain.fortune.Currency

case class LoadBalances(potResult: Pot[Map[Currency, BigDecimal]] = Pot.empty)
  extends PotAction[Map[Currency, BigDecimal], LoadBalances] {
  def next(newResult: Pot[Map[Currency, BigDecimal]]): LoadBalances = copy(potResult = newResult)
}
