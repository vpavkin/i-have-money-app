package ru.pavkin.ihavemoney.frontend.components

import java.time.{LocalDate, Year, YearMonth}

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory, FortuneInfo, Worth}
import ru.pavkin.ihavemoney.frontend.bootstrap.{Icon, Panel}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadEventLog
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{Event, Expense}
import ru.pavkin.utils.date._

import scala.math.BigDecimal.RoundingMode
import scalacss.ScalaCssReact._

object StatsViewC {

  case class Props(
      fortune: FortuneInfo,
      log: ModelProxy[Pot[List[Event]]])

  case class State(
      week: LocalDate,
      weekCurAgg: Currency,
      month: YearMonth,
      monthCurAgg: Currency,
      year: Year)

  class Backend($: BackendScope[Props, State]) {

    def loadTransactionLog(pr: Props) = Callback {
      AppCircuit.dispatch(LoadEventLog())
    }

    def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def renderAmount(a: BigDecimal): String =
      a.setScale(2, RoundingMode.HALF_UP).toString()

    def convertLimit(l: Worth, currency: Currency): BigDecimal =
      if (l.currency == currency) l.amount
      else AppCircuit.exchange(l.amount, l.currency, currency)

    def render(pr: Props, st: State) = div(
      pr.log().renderEmpty(PreloaderC()),
      pr.log().renderPending(_ => div(PreloaderC())),
      pr.log().renderReady { log ⇒
        val monthlyExpensesByCategory = log
            .filter(e => e.date.getMonth == st.month.getMonth && e.date.getYear == st.month.getYear)
            .collect { case t: Expense => t }
            .groupBy(_.category)

        val montlyExpensesAggregated =
          monthlyExpensesByCategory
              .mapValues(_.map {
                case e if e.currency == st.monthCurAgg => -e.amount
                case e if e.currency != st.monthCurAgg => AppCircuit.exchange(-e.amount, e.currency, st.monthCurAgg)
              }.sum)

        val weeklyExpensesByCategory = log
            .filter(e => {
              println(s"${st.week.toEpochDay} ${e.date.toEpochDay} ${st.week.plusDays(6).toEpochDay}")
              e.date.toEpochDay >= st.week.toEpochDay && e.date.toEpochDay <= st.week.plusDays(6).toEpochDay
            })
            .collect { case t: Expense => t }
            .groupBy(_.category)

        val weeklyExpensesAggregated =
          weeklyExpensesByCategory
              .mapValues(_.map {
                case e if e.currency == st.weekCurAgg => -e.amount
                case e if e.currency != st.weekCurAgg => AppCircuit.exchange(-e.amount, e.currency, st.weekCurAgg)
              }.sum)
        div(common.container,
          div(grid.columnAll(GRID_SIZE / 2),
            Panel(Some(div(
              h3(common.panelTitle, "Weekly stats",
                CurrencySelector(st.weekCurAgg, onChange = c => $.modState(_.copy(weekCurAgg = c)), addStyles = Seq(common.pullRight)),
                span(" "),
                WeekSelector(st.week, onChange = w => $.modState(_.copy(week = w)), addStyles = Seq(common.pullRight, rightMargin))
              ))),
              common.context.default,
              table(className := "table table-striped table-hover table-condensed",
                thead(tr(th("Category"), th("Expense"), th("Limit"), th("+/-"))),
                tbody(
                  weeklyExpensesAggregated.map {
                    case (cat, exp) ⇒
                      val limitOpt = pr.fortune.weeklyLimits.get(ExpenseCategory(cat)).map(convertLimit(_, st.weekCurAgg))
                      val overExp = limitOpt.map(_ - exp)
                      tr(
                        key := cat,
                        td(
                          if (overExp.exists(_ < 0)) span(className := "text-danger", Icon.exclamationTriangle, " ")
                          else if (overExp.exists(_ < limitOpt.getOrElse(BigDecimal(0.0)) / 10)) span(className := "text-warning", Icon.exclamationTriangle, " ")
                          else EmptyTag,
                          cat
                        ),
                        td(logNegAmount, renderAmount(exp) + st.weekCurAgg.sign),
                        td(limitOpt.map(l => renderAmount(l) + st.weekCurAgg.sign).getOrElse(" - "): String),
                        td(overExp.map(amountStyle), overExp.map(o => renderAmount(o) + st.weekCurAgg.sign).getOrElse(" - "): String)
                      )
                  }
                )
              )
            )
          ),
          div(grid.columnAll(GRID_SIZE / 2),
            Panel(Some(div(
              h3(common.panelTitle, "Monthly stats",
                CurrencySelector(st.monthCurAgg, onChange = c => $.modState(_.copy(monthCurAgg = c)), addStyles = Seq(common.pullRight)),
                span(" "),
                YearMonthSelector(st.month, onChange = ym => $.modState(_.copy(month = ym)), addStyles = Seq(common.pullRight, rightMargin))
              ))),
              common.context.default,
              table(className := "table table-striped table-hover table-condensed",
                thead(tr(th("Category"), th("Expense"), th("Limit"), th("+/-"))),
                tbody(
                  montlyExpensesAggregated.map {
                    case (cat, exp) ⇒
                      val limitOpt = pr.fortune.monthlyLimits.get(ExpenseCategory(cat)).map(convertLimit(_, st.monthCurAgg))
                      val overExp = limitOpt.map(_ - exp)
                      tr(
                        key := cat,
                        td(
                          if (overExp.exists(_ < 0)) span(className := "text-danger", Icon.exclamationTriangle, " ")
                          else if (overExp.exists(_ < limitOpt.getOrElse(BigDecimal(0.0)) / 10)) span(className := "text-warning", Icon.exclamationTriangle, " ")
                          else EmptyTag,
                          cat
                        ),
                        td(logNegAmount, renderAmount(exp) + st.monthCurAgg.sign),
                        td(limitOpt.map(l => renderAmount(l) + st.monthCurAgg.sign).getOrElse(" - "): String),
                        td(overExp.map(amountStyle), overExp.map(o => renderAmount(o) + st.monthCurAgg.sign).getOrElse(" - "): String)
                      )
                  }
                )
              )
            )
          )
        )
      }
    )
  }

  val component = ReactComponentB[Props]("StatsComponent")
      .initialState(State(LocalDate.now().atStartOfWeek, Currency.EUR, YearMonth.now, Currency.EUR, Year.now))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.loadTransactionLog(s.props))
      .build
}
