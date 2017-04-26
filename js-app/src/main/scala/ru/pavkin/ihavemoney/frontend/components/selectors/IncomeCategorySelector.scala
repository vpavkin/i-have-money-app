package ru.pavkin.ihavemoney.frontend.components.selectors

import ru.pavkin.ihavemoney.domain.fortune.IncomeCategory
import cats.Order.catsKernelOrderingForOrder

object IncomeCategorySelector extends StrictSimpleSelector[IncomeCategory] {
  override def sortItems(pr: IncomeCategorySelector.TheProps)(all: List[IncomeCategory]): List[IncomeCategory] =
    all.sorted

}
