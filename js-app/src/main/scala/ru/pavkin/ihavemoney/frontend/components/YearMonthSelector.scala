package ru.pavkin.ihavemoney.frontend.components

import java.time.YearMonth

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{Callback, ReactComponentB}
import ru.pavkin.ihavemoney.frontend.bootstrap.Dropdown

import scalacss.StyleA
import ru.pavkin.utils.date._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

object YearMonthSelector {

  case class Props(
      selected: YearMonth,
      onChange: YearMonth => Callback,
      monthsToPast: Int = 12,
      monthsToFuture: Int = 12,
      addStyles: Seq[StyleA] = Seq())

  def genElements(from: YearMonth, n: Int, f: YearMonth => YearMonth): List[YearMonth] =
    (1 until n).foldLeft(List(f(from))) {
      case (list, _) => f(list.head) :: list
    }

  def renderElement(pr: Props)(m: YearMonth) =
    li(key := m.toString, a(onClick --> pr.onChange(m), m.mmyyyy))

  val component = ReactComponentB[Props]("YearMonthSelector")
      .renderPC((_, p, c) =>
        Dropdown(p.selected.mmyyyy, div, Some(common.context.info), addStyles = p.addStyles)(
          (genElements(p.selected, p.monthsToPast, _.previous) ++
              genElements(p.selected, p.monthsToFuture, _.next).reverse)
              .map(renderElement(p))
        )
      ).build

  def apply(
      selected: YearMonth,
      onChange: YearMonth => Callback,
      monthsToPast: Int = 12,
      monthsToFuture: Int = 12,
      addStyles: Seq[StyleA] = Seq()) =
    component(Props(selected, onChange, monthsToPast, monthsToFuture, addStyles))
}
