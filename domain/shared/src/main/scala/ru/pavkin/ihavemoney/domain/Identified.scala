package ru.pavkin.ihavemoney.domain

import java.util.UUID

import ru.pavkin.utils.strings.syntax._
import shapeless.{::, Generic, HList, HNil, Lazy}
import simulacrum.typeclass

import scala.language.implicitConversions

// Proves that entity of type T can be uniquely identified among other T's by a string.
@typeclass trait Identified[T] {
  def uid(t: T): String
}

object Identified {

  // implicit prove for products of shape (SomeId(UUID), ...)
  implicit def productWithIdHead[T, Head, Tail <: HList](
    implicit G1: Lazy[Generic.Aux[T, Head :: Tail]],
    G2: Lazy[Generic.Aux[Head, UUID :: HNil]]): Identified[T] =
    (t: T) => G2.value.to(G1.value.to(t).head).head.toString

  // derive from Named instance (Named instance should guarantee name uniqueness)
  def fromNamed[T](implicit N: Named[T]): Identified[T] =
    (t: T) => N.name(t).cssClass
}
