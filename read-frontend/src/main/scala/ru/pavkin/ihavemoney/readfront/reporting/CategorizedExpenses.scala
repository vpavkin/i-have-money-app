package ru.pavkin.ihavemoney.readfront.reporting

import ru.pavkin.ihavemoney.domain.fortune.ExpenseCategory

case class CategorizedExpenses(underlying: Map[ExpenseCategory, BigDecimal]) extends AnyVal {

  def apply(category: ExpenseCategory): BigDecimal = underlying.getOrElse(category, BigDecimal(0.0))

  def addExpense(category: ExpenseCategory, amount: BigDecimal): CategorizedExpenses =
    CategorizedExpenses(underlying.updated(category, underlying.getOrElse(category, BigDecimal(0.0)) + amount))

  def mapAmounts(transformer: BigDecimal => BigDecimal) = CategorizedExpenses(underlying.mapValues(transformer))
}

object CategorizedExpenses {
  val empty = CategorizedExpenses(Map.empty)
}
