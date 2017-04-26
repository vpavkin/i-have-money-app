package ru.pavkin.ihavemoney.frontend.components.selectors

import ru.pavkin.ihavemoney.domain.fortune.ExpenseCategory
import cats.Order.catsKernelOrderingForOrder

object ExpenseCategorySelector extends StrictSimpleSelector[ExpenseCategory]{
  override def sortItems(pr: ExpenseCategorySelector.TheProps)(all: List[ExpenseCategory]): List[ExpenseCategory] =
    all.sorted

}
