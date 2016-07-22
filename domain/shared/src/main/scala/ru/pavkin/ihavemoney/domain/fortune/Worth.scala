package ru.pavkin.ihavemoney.domain.fortune

import cats.Eq

case class Worth(amount: BigDecimal, currency: Currency) {
  def +(other: Worth): Worth = {
    require(other.currency == currency, "Can't combine different currencies")
    copy(amount = amount + other.amount)
  }
  def *(by: BigDecimal): Worth =
    copy(amount = amount * by)

  override def toString: String = f"$amount%1.2f ${currency.code}"

  def amountStr = f"$amount%1.2f"
  def toPrettyString: String = f"${amountStr.stripSuffix(".00")} ${currency.code}"
}

object Worth {
  implicit val eq: Eq[Worth] = new Eq[Worth] {
    def eqv(x: Worth, y: Worth): Boolean = x.amount.equals(y.amount) && x.currency.equals(y.currency)
  }
  implicit val ord: Ordering[Worth] = Ordering.by(_.amount)

  def unsafeFrom(amount: BigDecimal, currency: String) = Worth(amount, Currency.unsafeFromCode(currency))
}
