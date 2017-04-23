package ru.pavkin.ihavemoney.readfront.reporting

import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRates}

case class YearlyPerformance(
  baseCurrency: Currency,
  totalIncome: BigDecimal,
  totalExpenses: BigDecimal,
  totalPerCategory: CategorizedExpenses,
  monthlyExpenses: List[CategorizedExpenses],
  weeklyExpenses: List[CategorizedExpenses],
  cashChanges: Map[Currency, BigDecimal]
) {
  def netIncome: BigDecimal = totalIncome - totalExpenses
  def margin: BigDecimal = netIncome / totalIncome
  def averageExpensesPerMonth: BigDecimal = totalExpenses / BigDecimal(12.0)

  def convertTo(other: Currency): YearlyPerformance = {
    def exchange(i: BigDecimal) = ExchangeRates.Default.exchange(i, baseCurrency, other).get
    copy(
      baseCurrency = other,
      totalIncome = exchange(totalIncome),
      totalExpenses = exchange(totalExpenses),
      totalPerCategory = totalPerCategory.mapAmounts(exchange),
      monthlyExpenses = monthlyExpenses.map(_.mapAmounts(exchange)),
      weeklyExpenses = weeklyExpenses.map(_.mapAmounts(exchange))
    )
  }

  def addIncome(currency: Currency, amount: BigDecimal): YearlyPerformance = {
    val normalized = normalize(currency, amount)
    copy(
      totalIncome = totalIncome + normalized,
      cashChanges = cashChanges.updated(currency, cashChanges(currency) + amount)
    )
  }

  def addExpense(e: FortuneSpent): YearlyPerformance = {
    val normalized = normalize(e.currency, e.amount)
    val monthIndex = e.expenseDate.getMonthValue - 1
    val weekIndex = math.min(e.expenseDate.getDayOfYear / 7, 51)
    copy(
      totalExpenses = totalExpenses + normalized,
      cashChanges = cashChanges.updated(e.currency, cashChanges(e.currency) - e.amount),
      totalPerCategory = totalPerCategory.addExpense(e.category, normalized),
      monthlyExpenses = monthlyExpenses.updated(
        monthIndex, monthlyExpenses(monthIndex).addExpense(e.category, normalized)),
      weeklyExpenses = weeklyExpenses.updated(
        weekIndex, weeklyExpenses(weekIndex).addExpense(e.category, normalized))
    )
  }

  def addExchange(e: CurrencyExchanged): YearlyPerformance = {
    copy(
      cashChanges = cashChanges
        .updated(e.fromCurrency, cashChanges(e.fromCurrency) - e.fromAmount)
        .updated(e.toCurrency, cashChanges(e.toCurrency) + e.toAmount)
    )
  }

  private def normalize(currency: Currency, amount: BigDecimal): BigDecimal =
    ExchangeRates.Default.exchange(amount, currency, baseCurrency).getOrElse(0)
}


object YearlyPerformance {

  def Initial(baseCurrency: Currency) = YearlyPerformance(baseCurrency, 0, 0,
    CategorizedExpenses.empty,
    List.fill(12)(CategorizedExpenses.empty),
    List.fill(52)(CategorizedExpenses.empty),
    Currency.values.map(_ -> BigDecimal(0.0)).toMap)

  def fold(baseCurrency: Currency, events: List[FortuneEvent]): YearlyPerformance =
    events.foldLeft(YearlyPerformance.Initial(baseCurrency)) {
      case (performance, event) => event match {
        case e: FortuneIncreased => performance.addIncome(e.currency, e.amount)
        case e: FortuneSpent => performance.addExpense(e)
        case e: CurrencyExchanged => performance.addExchange(e)
        case _ => performance
      }
    }
}
