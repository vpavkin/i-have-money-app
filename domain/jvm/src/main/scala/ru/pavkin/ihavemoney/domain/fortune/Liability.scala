package ru.pavkin.ihavemoney.domain.fortune

import java.util.UUID

sealed trait Liability {
  def name: String
  def amount: BigDecimal
  def currency: Currency
  def worth: Worth = Worth(amount, currency)
}

case class NoInterestDebt(name: String,
                          amount: BigDecimal,
                          currency: Currency) extends Liability

case class Loan(name: String,
                amount: BigDecimal,
                currency: Currency,
                interestRate: BigDecimal) extends Liability

case class LiabilityId(value: UUID) extends AnyVal
