package ru.pavkin.ihavemoney.domain.fortune

case class FortuneInfo(
    id: String,
    owner: String,
    editors: Set[String],
    weeklyLimits: Map[ExpenseCategory, Worth],
    monthlyLimits: Map[ExpenseCategory, Worth],
    initializationMode: Boolean) {

  private def limitsWithTotal(limits: Map[ExpenseCategory, Worth]) =
    limits + (
        ExpenseCategory.Total ->
            Worth(limits.values.map(_.amount).sum, limits.values.headOption.map(_.currency).getOrElse(Currency.EUR))
        )

  def weeklyLimitsWithTotal: Map[ExpenseCategory, Worth] = limitsWithTotal(weeklyLimits)
  def monthlyLimitsWithTotal: Map[ExpenseCategory, Worth] = limitsWithTotal(monthlyLimits)
}
