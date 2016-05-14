package ru.pavkin.ihavemoney.domain.fortune

import java.util.UUID

sealed trait Liability {
  def name: String
  def amount: BigDecimal
  def currency: Currency
  def worth: Worth = Worth(amount, currency)

  def payOff(byAmount: BigDecimal): Liability
}

case class NoInterestDebt(name: String,
                          amount: BigDecimal,
                          currency: Currency) extends Liability {
  def payOff(byAmount: BigDecimal): Liability = copy(amount = amount - byAmount)
}

case class Loan(name: String,
                amount: BigDecimal,
                currency: Currency,
                interestRate: BigDecimal) extends Liability {
  def payOff(byAmount: BigDecimal): Liability = copy(amount = amount - byAmount)
}

case class LiabilityId(value: UUID) extends AnyVal
object LiabilityId {
  def generate: LiabilityId = LiabilityId(UUID.randomUUID)
}
