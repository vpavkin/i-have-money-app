package ru.pavkin.utils

import java.time.LocalDate
import strings.syntax._

object date {

  private def p(i: Int): String = i.toString.padLeft(2, '0')

  implicit class LocalDateUtilityOps(m: LocalDate) {
    def ddmmyyyy: String = s"${p(m.getDayOfMonth)}-${p(m.getMonthValue)}-${m.getYear}"
    def dayOfWeekName = m.getDayOfWeek.name.toLowerCase.capitalize
    def toFullString = s"$dayOfWeekName, $ddmmyyyy"
  }
}
