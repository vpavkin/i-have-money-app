package ru.pavkin.ihavemoney.domain.fortune

import cats.{Eq, Order}
import ru.pavkin.ihavemoney.domain.Identified
import cats.instances.string.catsKernelStdOrderForString

case class IncomeCategory(name: String) extends AnyVal

object IncomeCategory {
  implicit val identified: Identified[IncomeCategory] = _.name

  implicit val orderInstance: Order[IncomeCategory] = catsKernelStdOrderForString.on(_.name.toLowerCase)
}
