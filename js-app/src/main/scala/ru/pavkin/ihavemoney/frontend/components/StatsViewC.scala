package ru.pavkin.ihavemoney.frontend.components

import java.time.{LocalDate, Year, YearMonth}

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory, FortuneInfo, Worth}
import ru.pavkin.ihavemoney.frontend.bootstrap.{Icon, Panel}
import ru.pavkin.ihavemoney.frontend.components.selectors.CurrencySelector
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{Event, Expense, Income}
import ru.pavkin.utils.date._
import cats.syntax.eq._
import cats.instances.int._
import cats.instances.string._
import scala.math.BigDecimal.RoundingMode
import scalacss.ScalaCssReact._
import cats.Order.catsKernelOrderingForOrder

object StatsViewC {

  case class Props(
    fortune: FortuneInfo,
    categories: ModelProxy[Pot[Categories]],
    log: ModelProxy[Pot[List[Event]]])

  case class State(
    week: LocalDate,
    weekCurAgg: Currency,
    month: YearMonth,
    monthCurAgg: Currency,
    year: Year,
    yearCurAgg: Currency)

  class Backend($: BackendScope[Props, State]) {

    def loadData(pr: Props) = Callback {
      AppCircuit.dispatch(LoadEventLog())
      AppCircuit.dispatch(LoadCategories())
    }

    private def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    private def renderAmount(a: BigDecimal): String =
      a.setScale(2, RoundingMode.HALF_UP).toString()

    private def convertLimit(l: Worth, currency: Currency): BigDecimal =
      if (l.currency === currency) l.amount
      else AppCircuit.exchange(l.amount, l.currency, currency)

    def render(pr: Props, st: State) = div(
      pr.log().renderEmpty(PreloaderC()),
      pr.log().renderPending(_ => div(PreloaderC())),
      pr.log().renderReady { log ⇒

        // year data
        val yearTransactions = log.filter(_.date.getYear === st.year.getValue)

        val yearExpensesByCategory = yearTransactions
          .collect { case t: Expense => t }
          .groupBy(_.category)

        val yearExpensesAggregated =
          yearExpensesByCategory
            .mapValues(_.map {
              case e if e.currency === st.yearCurAgg => -e.amount
              case e if e.currency =!= st.yearCurAgg => AppCircuit.exchange(-e.amount, e.currency, st.yearCurAgg)
            }.sum)

        val yearWithTotal = yearExpensesAggregated.toList.sortBy(_._1) :+ (ExpenseCategory.Total -> yearExpensesAggregated.values.sum)

        val yearIncomeAggregated = yearTransactions
          .collect { case t: Income => t }
          .map {
            case e if e.currency === st.yearCurAgg => e.amount
            case e if e.currency =!= st.yearCurAgg => AppCircuit.exchange(e.amount, e.currency, st.yearCurAgg)
          }.sum

        // month data

        val monthlyTransactions = log.filter(e => e.date.getMonth == st.month.getMonth && e.date.getYear === st.month.getYear)

        val monthlyExpensesByCategory =
          monthlyTransactions
            .collect { case t: Expense => t }
            .groupBy(_.category)

        val monthlyExpensesAggregated =
          monthlyExpensesByCategory
            .mapValues(_.map {
              case e if e.currency === st.monthCurAgg => -e.amount
              case e if e.currency =!= st.monthCurAgg => AppCircuit.exchange(-e.amount, e.currency, st.monthCurAgg)
            }.sum)

        val monthlyWithTotal = monthlyExpensesAggregated.toList.sortBy(_._1) :+ (ExpenseCategory.Total -> monthlyExpensesAggregated.values.sum)

        val monthlyIncomeAggregated = monthlyTransactions
          .collect { case t: Income => t }
          .map {
            case e if e.currency === st.monthCurAgg => e.amount
            case e if e.currency =!= st.monthCurAgg => AppCircuit.exchange(e.amount, e.currency, st.monthCurAgg)
          }.sum

        val weeklyExpensesByCategory = log
          .filter(e => e.date.toEpochDay >= st.week.toEpochDay && e.date.toEpochDay <= st.week.plusDays(6).toEpochDay)
          .collect { case t: Expense => t }
          .groupBy(_.category)

        val weeklyExpensesAggregated =
          weeklyExpensesByCategory
            .mapValues(_.map {
              case e if e.currency === st.weekCurAgg => -e.amount
              case e if e.currency =!= st.weekCurAgg => AppCircuit.exchange(-e.amount, e.currency, st.weekCurAgg)
            }.sum)

        val weeklyWithTotal = weeklyExpensesAggregated.toList.sortBy(_._1) :+ (ExpenseCategory.Total -> weeklyExpensesAggregated.values.sum)

        div(common.container,
          div(grid.columnAll(GRID_SIZE / 2),
            div(
              Panel(Some(div(
                div(common.panelTitle,
                  h4("Weekly stats", common.pullLeft),
                  CurrencySelector(
                    st.weekCurAgg,
                    onChange = c => $.modState(_.copy(weekCurAgg = c)),
                    Currency.values.toList,
                    style = common.context.info,
                    addAttributes = Seq(common.pullRight)),
                  span(" "),
                  div(common.pullRight,
                    WeekSelector(
                      st.week,
                      onChange = w => $.modState(_.copy(week = w)),
                      addAttributes = Seq(common.pullRight, rightMargin))
                  ),
                  div(common.clearBoth)
                ))),
                common.context.default,
                table(className := "table table-striped table-hover table-condensed",
                  thead(tr(th("Category"), th("Expense"), th("Limit"), th("+/-"))),
                  tbody(
                    pr.categories().renderReady(cats => cats.expenseWithTotal.map { cat =>
                      val exp = weeklyWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                      val limitOpt = pr.fortune.weeklyLimitsWithTotal.get(cat).map(convertLimit(_, st.weekCurAgg))
                      val overExp = limitOpt.map(_ - exp)
                      tr(
                        key := cat.name,
                        td(
                          if (overExp.exists(_ < 0)) span(className := "text-danger", Icon.exclamationTriangle, " ")
                          else if (overExp.exists(_ < limitOpt.getOrElse(BigDecimal(0.0)) / 10))
                            span(className := "text-warning", Icon.exclamationTriangle, " ")
                          else EmptyTag,
                          if (cat === ExpenseCategory.Total) strong(cat.name) else cat.name
                        ),
                        td(if (exp > 0) logNegAmount else EmptyTag, renderAmount(exp) + st.weekCurAgg.sign),
                        td(limitOpt.map(l => renderAmount(l) + st.weekCurAgg.sign).getOrElse(" - "): String),
                        td(overExp.map(amountStyle), overExp.map(o => renderAmount(o) + st.weekCurAgg.sign).getOrElse(" - "): String)
                      )
                    })
                  )
                )
              )
            ),
            div(
              Panel(Some(div(
                div(common.panelTitle,
                  h4("Current year stats", common.pullLeft),
                  CurrencySelector(
                    st.yearCurAgg,
                    onChange = c => $.modState(_.copy(yearCurAgg = c)),
                    Currency.values.toList,
                    style = common.context.info,
                    addAttributes = Seq(common.pullRight)),
                  div(common.clearBoth)
                ))),
                common.context.default,
                table(className := "table table-striped table-hover table-condensed",
                  thead(tr(th("Category"), th("Expense"))),
                  tbody(
                    pr.categories().renderReady(cats => cats.expenseWithTotal.map { cat =>
                      val exp = yearWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                      tr(
                        key := cat.name,
                        td(
                          if (cat === ExpenseCategory.Total) strong(cat.name) else cat.name
                        ),
                        td(if (exp > 0) logNegAmount else EmptyTag, renderAmount(exp) + st.yearCurAgg.sign)
                      )
                    })
                  )
                ),
                hr(),
                h4("Total income: ", span(logPosAmount, renderAmount(yearIncomeAggregated) + st.yearCurAgg.sign))
              )
            )
          ),
          div(grid.columnAll(GRID_SIZE / 2),
            Panel(Some(div(
              div(common.panelTitle,
                h4("Monthly stats", common.pullLeft),
                CurrencySelector(
                  st.monthCurAgg,
                  onChange = c => $.modState(_.copy(monthCurAgg = c)),
                  Currency.values.toList,
                  style = common.context.info,
                  addAttributes = Seq(common.pullRight)),
                span(" "),
                div(common.pullRight,
                  YearMonthSelector(
                    st.month,
                    onChange = ym => $.modState(_.copy(month = ym)),
                    addAttributes = Seq(common.pullRight, rightMargin))
                ),
                div(common.clearBoth)
              ))),
              common.context.default,
              table(className := "table table-striped table-hover table-condensed",
                thead(tr(th("Category"), th("Expense"), th("Limit"), th("+/-"))),
                tbody(
                  pr.categories().renderReady(cats => cats.expenseWithTotal.map { cat =>
                    val exp = monthlyWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                    val limitOpt = pr.fortune.monthlyLimitsWithTotal.get(cat).map(convertLimit(_, st.monthCurAgg))
                    val overExp = limitOpt.map(_ - exp)
                    tr(
                      key := cat.name,
                      td(
                        if (overExp.exists(_ < 0)) span(className := "text-danger", Icon.exclamationTriangle, " ")
                        else if (overExp.exists(_ < limitOpt.getOrElse(BigDecimal(0.0)) / 10))
                          span(className := "text-warning", Icon.exclamationTriangle, " ")
                        else EmptyTag,
                        if (cat === ExpenseCategory.Total) strong(cat.name) else cat.name
                      ),
                      td(if (exp > 0) logNegAmount else EmptyTag, renderAmount(exp) + st.monthCurAgg.sign),
                      td(limitOpt.map(l => renderAmount(l) + st.monthCurAgg.sign).getOrElse(" - "): String),
                      td(overExp.map(amountStyle), overExp.map(o => renderAmount(o) + st.monthCurAgg.sign).getOrElse(" - "): String)
                    )
                  })
                )
              ),
              hr(),
              h4("Total income: ", span(logPosAmount, renderAmount(monthlyIncomeAggregated) + st.monthCurAgg.sign))
            )
          )
        )
      }
    )
  }

  val component = ReactComponentB[Props]("StatsComponent")
    .initialState(State(LocalDate.now().atStartOfWeek, Currency.EUR, YearMonth.now, Currency.EUR, Year.now, Currency.EUR))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.loadData(s.props))
    .build
}
