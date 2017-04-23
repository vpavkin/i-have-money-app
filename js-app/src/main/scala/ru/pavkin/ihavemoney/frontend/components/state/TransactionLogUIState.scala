package ru.pavkin.ihavemoney.frontend.components.state

import java.time.YearMonth

import ru.pavkin.ihavemoney.domain.fortune.ExpenseCategory

case class TransactionLogUIState(
  filterByCategory: Boolean,
  category: ExpenseCategory,
  filterByMonth: Boolean,
  month: YearMonth,
  textFilter: String)

object TransactionLogUIState {
  val Default = TransactionLogUIState(
    filterByCategory = false,
    category = ExpenseCategory("Groceries"),
    filterByMonth = false,
    month = YearMonth.now(),
    textFilter = "")
}
