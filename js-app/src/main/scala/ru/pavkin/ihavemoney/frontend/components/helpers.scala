package ru.pavkin.ihavemoney.frontend.components

import java.time.LocalDate

import ru.pavkin.utils.date.LocalDateParser

import scala.util.Try

object helpers {

  def parseDate(s: String): Try[LocalDate] = LocalDateParser.fromYYYYMMDD(s)
  def isValidDate(s: String) = parseDate(s).isSuccess
}
