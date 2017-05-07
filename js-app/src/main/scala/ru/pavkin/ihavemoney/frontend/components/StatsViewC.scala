package ru.pavkin.ihavemoney.frontend.components

import java.time.{LocalDate, Year, YearMonth}

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.frontend.bootstrap.{Icon, Panel}
import ru.pavkin.ihavemoney.frontend.components.selectors.CurrencySelector
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog, LoadExchangeRates}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{Event, Expense, Income}
import ru.pavkin.utils.date._
import cats.syntax.eq._
import cats.instances.int._
import cats.instances.string._

import scalacss.ScalaCssReact._
import cats.Order.catsKernelOrderingForOrder
import diode.ActionBatch

import scala.util.{Failure, Success, Try}

object StatsViewC {

  case class Props(
    fortune: FortuneInfo,
    exchangeRates: ModelProxy[Pot[ExchangeRates]],
    categories: ModelProxy[Pot[Categories]],
    log: ModelProxy[Pot[List[Event]]]) extends RemoteDataProps[Props] {

    def loadData: Callback =
      AppCircuit.dispatchCB(ActionBatch(
        LoadEventLog(Year.now),
        LoadCategories(),
        LoadExchangeRates()
      ))

    def shouldReload(nextProps: Props): Boolean = fortune.id =!= nextProps.fortune.id

    def renderContext = for {
      rates <- exchangeRates()
      cats <- categories()
      l <- log()
    } yield (rates, cats, l)
  }

  case class State(
    week: LocalDate,
    weekCurAgg: Currency,
    month: YearMonth,
    monthCurAgg: Currency,
    year: Year,
    yearCurAgg: Currency)

  class Backend($: BackendScope[Props, State]) {

    private def convertLimit(rates: ExchangeRates, l: Worth, currency: Currency): BigDecimal =
      rates.exchangeUnsafe(l.amount, l.currency, currency)

    private def renderStatsUnsafe(
      st: State,
      fortune: FortuneInfo,
      rates: ExchangeRates,
      categories: Categories,
      log: List[Event]) = {
      val yearTransactions = log.filter(_.date.getYear === st.year.getValue)

      val yearExpensesByCategory = yearTransactions
        .collect { case t: Expense => t }
        .groupBy(_.category)

      val yearExpensesAggregated =
        yearExpensesByCategory
          .mapValues(_.map(e => rates.exchangeUnsafe(-e.amount, e.currency, st.yearCurAgg)).sum)

      val yearWithTotal = yearExpensesAggregated.toList.sortBy(_._1) :+ (ExpenseCategory.Total -> yearExpensesAggregated.values.sum)

      val yearIncomeAggregated = yearTransactions
        .collect { case t: Income => t }
        .map(e => rates.exchangeUnsafe(e.amount, e.currency, st.yearCurAgg)).sum

      // month data

      val monthlyTransactions = log.filter(e => e.date.getMonth === st.month.getMonth && e.date.getYear === st.month.getYear)

      val monthlyExpensesByCategory =
        monthlyTransactions
          .collect { case t: Expense => t }
          .groupBy(_.category)

      val monthlyExpensesAggregated =
        monthlyExpensesByCategory
          .mapValues(_.map(e => rates.exchangeUnsafe(-e.amount, e.currency, st.monthCurAgg)).sum)

      val monthlyWithTotal = monthlyExpensesAggregated.toList.sortBy(_._1) :+ (ExpenseCategory.Total -> monthlyExpensesAggregated.values.sum)

      val monthlyIncomeAggregated = monthlyTransactions
        .collect { case t: Income => t }
        .map(e => rates.exchangeUnsafe(e.amount, e.currency, st.monthCurAgg)).sum

      val weeklyExpensesByCategory = log
        .filter(e => e.date.toEpochDay >= st.week.toEpochDay && e.date.toEpochDay <= st.week.plusDays(6).toEpochDay)
        .collect { case t: Expense => t }
        .groupBy(_.category)

      val weeklyExpensesAggregated =
        weeklyExpensesByCategory
          .mapValues(_.map(e => rates.exchangeUnsafe(-e.amount, e.currency, st.weekCurAgg)).sum)

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
                  categories.expenseWithTotal.map { cat =>
                    val exp = weeklyWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                    val limitOpt = fortune.weeklyLimitsWithTotal.get(cat).map(convertLimit(rates, _, st.weekCurAgg))
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
                  }
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
                  categories.expenseWithTotal.map { cat =>
                    val exp = yearWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                    tr(
                      key := cat.name,
                      td(
                        if (cat === ExpenseCategory.Total) strong(cat.name) else cat.name
                      ),
                      td(if (exp > 0) logNegAmount else EmptyTag, renderAmount(exp) + st.yearCurAgg.sign)
                    )
                  }
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
                categories.expenseWithTotal.map { cat =>
                  val exp = monthlyWithTotal.toMap.getOrElse(cat, BigDecimal(0.0))
                  val limitOpt = fortune.monthlyLimitsWithTotal.get(cat).map(convertLimit(rates, _, st.monthCurAgg))
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
                }
              )
            ),
            hr(),
            h4("Total income: ", span(logPosAmount, renderAmount(monthlyIncomeAggregated) + st.monthCurAgg.sign))
          )
        )
      )
    }

    def render(pr: Props, st: State) = div(
      pr.renderContext.renderEmpty(PreloaderC()),
      pr.renderContext.renderPending(_ => div(PreloaderC())),
      pr.renderContext.renderReady { case (rates, categories, log) ⇒
        val renderResult = Try(renderStatsUnsafe(st, pr.fortune, rates, categories, log))
        renderResult match {
          case Failure(exception) => div(s"Error: ${exception.getMessage}")
          case Success(markup) => markup
        }
      }
    )
  }

  val component = ReactComponentB[Props]("StatsComponent")
    .initialState(State(LocalDate.now().atStartOfWeek, Currency.EUR, YearMonth.now, Currency.EUR, Year.now, Currency.EUR))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.props.loadData)
    .build
}
