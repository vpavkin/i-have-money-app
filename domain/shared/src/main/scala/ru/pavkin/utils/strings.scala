package ru.pavkin.utils

import scala.language.implicitConversions

object strings {

  def padLeft(string: String, toLength: Int, pad: Char): String =
    if (string.length >= toLength) string
    else "".padTo(toLength - string.length, pad) + string

  final class StringUtilOps(val s: String) {
    def padLeft(toLength: Int, pad: Char) = strings.padLeft(s, toLength, pad)
  }

  final class IntUtilOps(val i: Int) {
    def padLeftZeroes(toLength: Int) = strings.padLeft(i.toString, toLength, '0')
  }

  object syntax {
    implicit def toStringsSyntax(l: String): StringUtilOps = new StringUtilOps(l)
    implicit def toIntSyntax(l: Int): IntUtilOps = new IntUtilOps(l)
  }
}
