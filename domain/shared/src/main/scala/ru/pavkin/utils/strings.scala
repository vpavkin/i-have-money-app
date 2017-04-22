package ru.pavkin.utils

import scala.language.implicitConversions

object strings {

  def padLeft(string: String, toLength: Int, pad: Char): String =
    if (string.length >= toLength) string
    else "".padTo(toLength - string.length, pad) + string

  def shorten(string: String, toLength: Int, ellipsis: String): String =
    if (string.length <= toLength) string
    else string.take(toLength - ellipsis.length) + ellipsis

  object syntax {
    final implicit class StringUtilOps(val s: String) extends AnyVal {
      def padLeft(toLength: Int, pad: Char): String = strings.padLeft(s, toLength, pad)
      def shorten(toLength: Int, ellipsis: String = "..."): String = strings.shorten(s, toLength, ellipsis)
      def noneIfEmpty: Option[String] = if (s.isEmpty) None else Some(s)
      // e.g. New Employee Name => new-employee-name
      def cssClass: String = s.replaceAll("\\s+", "-").toLowerCase()
    }

    final implicit class IntUtilOps(val i: Int) extends AnyVal {
      def padLeftZeroes(toLength: Int): String = strings.padLeft(i.toString, toLength, '0')
    }

    final implicit class StringOptionOps(val s: Option[String]) extends AnyVal {
      def orEmpty: String = s.getOrElse("")
    }
  }
}
