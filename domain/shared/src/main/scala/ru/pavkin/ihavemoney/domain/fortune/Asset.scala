package ru.pavkin.ihavemoney.domain.fortune

import java.util.UUID

sealed trait Asset {
  def name: String
  def count: BigDecimal
  def price: BigDecimal
  def currency: Currency
  def worth: Worth = Worth(price * count, currency)

  def reevaluate(newPrice: BigDecimal): Asset
}

case class CountedAsset(
    name: String,
    price: BigDecimal,
    currency: Currency,
    count: BigDecimal) extends Asset {
  def reevaluate(newPrice: BigDecimal): Asset = copy(price = newPrice)
}

case class AssetId(value: UUID) extends AnyVal
object AssetId {
  def generate: AssetId = AssetId(UUID.randomUUID)
}
