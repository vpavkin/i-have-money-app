package ru.pavkin.ihavemoney.frontend.redux.actions

import diode.data.{Pot, PotAction}
import ru.pavkin.ihavemoney.protocol.Transaction

case class LoadTransactionLog(potResult: Pot[List[Transaction]] = Pot.empty)
  extends PotAction[List[Transaction], LoadTransactionLog] {
  def next(newResult: Pot[List[Transaction]]): LoadTransactionLog = copy(potResult = newResult)
}
