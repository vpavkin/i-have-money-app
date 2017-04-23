package ru.pavkin.ihavemoney.readfront.reporting

import java.time.Month
import java.util.UUID

import cats.syntax.eq._
import cats.Order.catsKernelOrderingForOrder
import com.norbitltd.spoiwo.model.Height._
import com.norbitltd.spoiwo.model._
import com.norbitltd.spoiwo.model.enums.{CellFill, CellHorizontalAlignment}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.{FortuneEvent, FortuneIncreased, FortuneSpent}
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRates}
import ru.pavkin.utils.strings.syntax._

class YearlyReportBuilder(reportsDir: String) {


  def build(transactions: List[FortuneEvent]): String = {

    val filePath = s"$reportsDir${UUID.randomUUID()}.xlsx"
    YearlyReportBuilder.Case(transactions, Currency.USD).workbook.saveAsXlsx(filePath)
    filePath
  }
}

object YearlyReportBuilder {

  private val headerStyle = CellStyle(
    fillPattern = CellFill.Solid,
    fillForegroundColor = Color.Black,
    horizontalAlignment = CellHorizontalAlignment.Center,
    font = Font(fontName = "Georgia", height = 14.points, bold = true, color = Color.White))

  private val defaultStyle = CellStyle(font = Font(fontName = "Georgia", height = 12.points))

  private val dateFormat = CellDataFormat("dd.mm.yyyy")
  private val percentageFormat = CellDataFormat("0.00%")
  private def moneyFormat(currency: Currency) = CellDataFormat(s"###0.00${currency.sign}")

  private def incomeStyle(currency: Currency) =
    CellStyle(font = Font(color = Color.Green), dataFormat = moneyFormat(currency))
  private def expenseStyle(currency: Currency) =
    CellStyle(font = Font(color = Color.Red), dataFormat = moneyFormat(currency))
  private def amountStyle(amount: BigDecimal, currency: Currency) =
    if (amount < 0) expenseStyle(currency)
    else incomeStyle(currency)

  case class Case(transactions: List[FortuneEvent], baseCurrency: Currency) {

    private def transactionsToRows: List[Row] = transactions
      .sortBy(_.date.toLocalDate.toEpochDay)
      .collect {
        case e: FortuneIncreased ⇒
          Row().withCells(
            Cell(e.date.toLocalDate, style = CellStyle(dataFormat = dateFormat)),
            Cell(e.category.name),
            Cell(e.amount, style = incomeStyle(e.currency)),
            Cell(e.currency.code),
            Cell(e.comment.orEmpty)
          )
        case e: FortuneSpent ⇒
          Row().withCells(
            Cell(e.expenseDate, style = CellStyle(dataFormat = dateFormat)),
            Cell(e.category.name),
            Cell(-e.amount, style = expenseStyle(e.currency)),
            Cell(e.currency.code),
            Cell(e.comment.orEmpty)
          )
      }

    private def currencyRows: List[Row] = Currency.values.map(c =>
      Row().withCells(
        Cell(c.code),
        Cell(
          ExchangeRates.Default.findRate(baseCurrency, c).get,
          style = CellStyle(dataFormat = moneyFormat(c)))
      )
    ).toList

    def buildPerformanceSheet(base: Currency): Sheet = {
      val normalized = if (performance.baseCurrency === base) performance else performance.convertTo(base)

      val totalRows = List(
        Row().withCells(
          Cell("Total income"),
          Cell(normalized.totalIncome, style = incomeStyle(base))),

        Row().withCells(
          Cell("Total expenses"),
          Cell(normalized.totalExpenses, style = expenseStyle(base))),

        Row().withCells(
          Cell("Net income"),
          Cell("=B1-B2", style = amountStyle(normalized.netIncome, base))),

        Row().withCells(
          Cell("Margin"),
          Cell("=B3/B1", style = CellStyle(dataFormat = percentageFormat))),

        Row().withCells(
          Cell("Average per month"),
          Cell("=B2/12", style = expenseStyle(base))),

        Row().withCells(
          Cell("Average per week"),
          Cell("=B2/52", style = expenseStyle(base))),

        Row().withCells(
          Cell("Average per day"),
          Cell("=B2/365", style = expenseStyle(base)))
      )

      val cashFlowRows = Currency.values.map(c =>
        Row().withCells(
          Cell(s"Cash flow [${c.code}]"),
          Cell(normalized.cashChanges(c), style = amountStyle(normalized.cashChanges(c), c)))
      )

      val perCategoryRows =
        Row().withCells(Cell(s"Total per category")) ::
          normalized.totalPerCategory.underlying.toList.sortBy(_._1).map { case (c, a) =>
            Row().withCells(
              Cell(c.name, style = defaultStyle),
              Cell(a, style = expenseStyle(base)),
              Cell(a / normalized.totalExpenses, style = CellStyle(dataFormat = percentageFormat))
            )
          }

      val allCats = normalized.monthlyExpenses.flatMap(_.underlying.keys).distinct.sorted

      val monthlyPerCategoryRows =
        Row(style = defaultStyle)
          .withCells(Cell("#") :: Month.values().map(_.name().toLowerCase.capitalize).map(Cell(_)).toList) ::
          allCats.map(cat =>
            Row(style = defaultStyle).withCells(Cell(cat.name) ::
              (0 to 11).map(normalized.monthlyExpenses)
                .map(_ (cat)).map(Cell(_, style = CellStyle(dataFormat = moneyFormat(base)))).toList
            )
          )

      val weeklyPerCategoryRows =
        Row(style = defaultStyle)
          .withCells(Cell("#") :: (0 to 51).map(i => Cell(i + 1)).toList) ::
          allCats.map(cat =>
            Row(style = defaultStyle).withCells(Cell(cat.name) ::
              (0 to 51).map(normalized.weeklyExpenses)
                .map(_ (cat)).map(Cell(_, style = CellStyle(dataFormat = moneyFormat(base)))).toList
            )
          )

      Sheet(name = s"Performance (${base.code})", style = defaultStyle)
        .withRows(List(
          totalRows,
          List(Row.Empty),
          cashFlowRows,
          List(Row.Empty),
          perCategoryRows,
          List(Row.Empty),
          monthlyPerCategoryRows,
          List(Row.Empty),
          weeklyPerCategoryRows
        ).flatten)
        .withColumns(
          Column(index = 0, autoSized = true, style = headerStyle) ::
            (1 until 52).map(i => Column(index = i, autoSized = true)).toList
        )
    }

    val performance: YearlyPerformance = YearlyPerformance.fold(baseCurrency, transactions)

    val transactionsSheet: Sheet = Sheet(name = "Transactions", style = defaultStyle)
      .withRows(
        Row(style = headerStyle).withCellValues("Date", "Category", "Income/Expense", "Currency", "Comment") ::
          transactionsToRows
      )
      .withColumns(
        (0 until 5).map(i => Column(index = i, autoSized = true)).toList
      )

    val currenciesSheet: Sheet = Sheet(name = "Currencies", style = defaultStyle)
      .withRows(
        Row(style = headerStyle).withCellValues("Currency", s"For 1 ${baseCurrency.code}") ::
          currencyRows
      )
      .withColumns(
        (0 until 5).map(i => Column(index = i, autoSized = true)).toList
      )

    val performanceSheets: List[Sheet] = Currency.values.map(buildPerformanceSheet).toList

    val workbook = Workbook(transactionsSheet :: (performanceSheets :+ currenciesSheet): _*)
  }

}
