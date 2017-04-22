package ru.pavkin.ihavemoney.domain.fortune

import ru.pavkin.ihavemoney.domain._
import enumeratum.EnumEntry.CapitalWords
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait Currency extends EnumEntry with CapitalWords {
  def code: String = entryName
  def sign: String
}

object Currency extends Enum[Currency] with RegularEnumInstances[Currency] {

  case object USD extends Currency {
    def sign: String = "$"
  }
  case object RUR extends Currency {
    def sign: String = "\u20BD"
  }
  case object EUR extends Currency {
    def sign: String = "€"
  }

  override implicit val namedInstance = new Named[Currency] {
    override def name(t: Currency): String = t.sign
  }

  def values: immutable.IndexedSeq[Currency] = findValues

  def isCurrency(code: String): Boolean = withNameOption(code).isDefined
}

