package ru.pavkin.ihavemoney.domain.fortune

import cats.{Eq, Order}
import ru.pavkin.ihavemoney.domain.Identified
import cats.instances.string.catsKernelStdOrderForString

case class ExpenseCategory(name: String) extends AnyVal

object ExpenseCategory {
  val Total: ExpenseCategory = ExpenseCategory("Total")

  implicit val identified: Identified[ExpenseCategory] = _.name
  implicit val orderInstance: Order[ExpenseCategory] = catsKernelStdOrderForString.on(_.name)
}
