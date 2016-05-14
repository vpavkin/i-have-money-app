package ru.pavkin.ihavemoney.domain.fortune

import java.util.UUID

sealed trait Asset {
  def name: String
  def price: BigDecimal
  def currency: Currency
  def worth: Worth = Worth(price, currency)
}

case class Stocks(stockName: String,
                  stockPrice: BigDecimal,
                  stockCurrency: Currency,
                  count: BigDecimal) extends Asset {
  def name: String = s"$stockName (x$count)"
  def price: BigDecimal = stockPrice * count
  def currency: Currency = stockCurrency
}

case class RealEstate(name: String,
                      price: BigDecimal,
                      currency: Currency) extends Asset

case class AssetId(value: UUID) extends AnyVal
object AssetId {
  def generate: AssetId = AssetId(UUID.randomUUID)
}
