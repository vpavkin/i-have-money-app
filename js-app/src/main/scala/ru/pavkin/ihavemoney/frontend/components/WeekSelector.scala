package ru.pavkin.ihavemoney.frontend.components

import java.time.{LocalDate, YearMonth}

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{Callback, ReactComponentB}
import ru.pavkin.ihavemoney.frontend.bootstrap.Dropdown
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.date._

import scalacss.StyleA

object WeekSelector {

  case class Props(
    selected: LocalDate,
    onChange: LocalDate => Callback,
    weeksToPast: Int = 5,
    weeksToFuture: Int = 5,
    addAttributes: Seq[TagMod] = Seq())

  def genElements(from: LocalDate, n: Int, f: LocalDate => LocalDate): List[LocalDate] =
    (1 until n).foldLeft(List(f(from))) {
      case (list, _) =>
        val next = f(list.head)
        if (next.isAfter(LocalDate.now().atStartOfWeek.plusDays(7))) list
        else next :: list
    }

  def renderWeek(w: LocalDate) =
    if (w.isPreviousWeek) s"Previous (${w.toWeekString})"
    else if (w.isCurrentWeek) s"Current (${w.toWeekString})"
    else if (w.isNextWeek) s"Next (${w.toWeekString})"
    else w.toWeekString

  def renderElement(pr: Props)(m: LocalDate) =
    li(key := m.toString, a(onClick --> pr.onChange(m), renderWeek(m)))

  val component = ReactComponentB[Props]("WeekSelector")
    .renderPC((_, p, c) =>
      Dropdown(renderWeek(p.selected), a, Some(common.context.info), addAttributes = p.addAttributes)(
        (genElements(p.selected, p.weeksToPast, _.minusDays(7)) ++
          genElements(p.selected, p.weeksToFuture, _.plusDays(7)).reverse)
          .map(renderElement(p))
      )
    ).build

  def apply(
    selected: LocalDate,
    onChange: LocalDate => Callback,
    weeksToPast: Int = 5,
    weeksToFuture: Int = 5,
    addAttributes: Seq[TagMod] = Seq()) =
    component(Props(selected, onChange, weeksToPast, weeksToFuture, addAttributes))
}
