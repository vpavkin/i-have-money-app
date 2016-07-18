package ru.pavkin.utils

import java.time.{LocalDate, YearMonth}

import strings.syntax._

object date {

  private def p(i: Int): String = i.toString.padLeft(2, '0')

  implicit class LocalDateUtilityOps(m: LocalDate) {
    def ddmmyyyy: String = s"${p(m.getDayOfMonth)}-${p(m.getMonthValue)}-${m.getYear}"
    def dayOfWeekName = m.getDayOfWeek.name.toLowerCase.capitalize
    def toFullString = s"$dayOfWeekName, $ddmmyyyy"
  }

  implicit class YearMonthUtilityOps(m: YearMonth) {
    def month = m.getMonthValue
    def year = m.getYear

    def mmyyyy: String = f"$month%02d" + "-" + f"$year%04d"

    def previous = m.minusMonths(1)
    def next = m.plusMonths(1)
    def numberOfDays: Int = m.lengthOfMonth()
  }
}
