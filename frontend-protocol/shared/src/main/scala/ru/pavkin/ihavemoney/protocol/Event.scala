package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate

import ru.pavkin.ihavemoney.domain.fortune.Currency

sealed trait Event {
  def date: LocalDate
}

sealed trait Transaction extends Event {
  def user: String
  def amount: BigDecimal
  def currency: Currency
  def category: String
  def comment: Option[String]
}

case class Income(
    user: String,
    amount: BigDecimal,
    currency: Currency,
    category: String,
    date: LocalDate,
    comment: Option[String]) extends Transaction

case class Expense(
    user: String,
    amount: BigDecimal,
    currency: Currency,
    category: String,
    date: LocalDate,
    comment: Option[String]) extends Transaction

case class CurrencyExchanged(
    user: String,
    fromAmount: BigDecimal,
    fromCurrency: Currency,
    toAmount: BigDecimal,
    toCurrency: Currency,
    date: LocalDate,
    comment: Option[String]) extends Event

