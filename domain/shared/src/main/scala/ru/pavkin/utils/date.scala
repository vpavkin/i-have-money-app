package ru.pavkin.utils

import java.time.{LocalDate, Month, Year, YearMonth}

import cats.Eq
import strings.syntax._

import scala.util.{Failure, Try}

object date {

  implicit val eqYear: Eq[Year] = Eq.fromUniversalEquals
  implicit val eqYearMonth: Eq[YearMonth] = Eq.fromUniversalEquals
  implicit val eqMonth: Eq[Month] = Eq.fromUniversalEquals

  private def p(i: Int): String = i.toString.padLeft(2, '0')

  implicit class LocalDateUtilityOps(m: LocalDate) {
    def ddmmyyyy: String = s"${p(m.getDayOfMonth)}-${p(m.getMonthValue)}-${m.getYear}"
    def yyyymmdd: String = s"${m.getYear}-${p(m.getMonthValue)}-${p(m.getDayOfMonth)}"
    def dayOfWeekName: String = m.getDayOfWeek.name.toLowerCase.capitalize
    def toFullString = s"$dayOfWeekName, $ddmmyyyy"

    def atStartOfWeek: LocalDate = m.minusDays(m.getDayOfWeek.getValue - 1L)

    def isCurrentWeek: Boolean = atStartOfWeek == LocalDate.now().atStartOfWeek
    def isPreviousWeek: Boolean = atStartOfWeek == LocalDate.now().atStartOfWeek.minusDays(7)
    def isNextWeek: Boolean = atStartOfWeek == LocalDate.now().atStartOfWeek.plusDays(7)

    def toWeekString: String = {
      val end = m.plusDays(6)
      s"${m.getDayOfMonth} ${m.getMonth.name.toLowerCase.capitalize} — ${end.getDayOfMonth} ${end.getMonth.name.toLowerCase.capitalize} ${end.getYear}"
    }
  }

  implicit class YearMonthUtilityOps(m: YearMonth) {
    def month: Int = m.getMonthValue
    def year: Int = m.getYear

    def mmyyyy: String = f"$month%02d" + "-" + f"$year%04d"

    def previous: YearMonth = m.minusMonths(1)
    def next: YearMonth = m.plusMonths(1)
    def numberOfDays: Int = m.lengthOfMonth()
  }

  object LocalDateParser {

    def fromYYYYMMDD(s: String): Try[LocalDate] = s.split("-").toList match {
      case year :: month :: day :: Nil =>
        Try(LocalDate.of(year.toInt, month.toInt, day.toInt))
      case _ => Failure(new Exception("Invalid string supplied, expected YYYY-MM-DD"))
    }

    def fromDDMMYYY(s: String): Try[LocalDate] = s.split("-").toList match {
      case day :: month :: year :: Nil =>
        Try(LocalDate.of(year.toInt, month.toInt, day.toInt))
      case _ => Failure(new Exception("Invalid string supplied, expected DD-MM-YYYY"))
    }
  }
}
