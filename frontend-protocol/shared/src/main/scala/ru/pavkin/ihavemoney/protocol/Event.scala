package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate
import java.util.UUID

import ru.pavkin.ihavemoney.domain.Named
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory, IncomeCategory}

sealed trait Event {
  def id: UUID
  def date: LocalDate
}

sealed abstract class Transaction[Cat: Named] extends Event {
  def user: String
  def amount: BigDecimal
  def currency: Currency
  def category: Cat
  def comment: Option[String]
}

case class Income(
  id: UUID,
  user: String,
  amount: BigDecimal,
  currency: Currency,
  category: IncomeCategory,
  date: LocalDate,
  comment: Option[String]) extends Transaction[IncomeCategory]

case class Expense(
  id: UUID,
  user: String,
  amount: BigDecimal,
  currency: Currency,
  category: ExpenseCategory,
  date: LocalDate,
  comment: Option[String]) extends Transaction[ExpenseCategory]

case class CurrencyExchanged(
  id: UUID,
  user: String,
  fromAmount: BigDecimal,
  fromCurrency: Currency,
  toAmount: BigDecimal,
  toCurrency: Currency,
  date: LocalDate,
  comment: Option[String]) extends Event

