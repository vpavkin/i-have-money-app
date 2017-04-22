package ru.pavkin.ihavemoney.domain

import cats._
import cats.instances.string._
import enumeratum.EnumEntry
import enumeratum.values.ValueEnumEntry

trait RegularEnumInstances[E <: EnumEntry] {

  implicit val namedInstance: Named[E] = (t: E) => t.entryName

  implicit val identifiedInstance: Identified[E] = (t: E) => t.entryName

  implicit val orderInstance: Order[E] = (x: E, y: E) => Order[String].compare(x.entryName, y.entryName)
}

trait ValueEnumInstances[V, E <: ValueEnumEntry[V]] {

  implicit val identifiedInstance: Identified[E] = (t: E) => t.value.toString

  implicit def orderInstance(implicit O: Order[V]): Order[E] = (x: E, y: E) => O.compare(x.value, y.value)
}

