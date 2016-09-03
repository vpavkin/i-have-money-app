package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate
import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.Currency

sealed trait Event {
  def id: UUID
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
    id: UUID,
    user: String,
    amount: BigDecimal,
    currency: Currency,
    category: String,
    date: LocalDate,
    comment: Option[String]) extends Transaction

case class Expense(
    id: UUID,
    user: String,
    amount: BigDecimal,
    currency: Currency,
    category: String,
    date: LocalDate,
    comment: Option[String]) extends Transaction

case class CurrencyExchanged(
    id: UUID,
    user: String,
    fromAmount: BigDecimal,
    fromCurrency: Currency,
    toAmount: BigDecimal,
    toCurrency: Currency,
    date: LocalDate,
    comment: Option[String]) extends Event

