package ru.pavkin.ihavemoney.domain

import shapeless.labelled.FieldType
import shapeless.{HList, LabelledGeneric, ::, Witness}
import simulacrum.typeclass

import scala.language.implicitConversions

@typeclass trait Named[T] {
  def name(t: T): String
}

object Named {

  val nameWitness = Witness.`'name`

  implicit def nameForProductWithNameField[T, Repr](
    implicit G: LabelledGeneric.Aux[T, Repr],
    N: Named[Repr]): Named[T] = (t: T) => N.name(G.to(t))

  implicit def hListHasNamedIfItsHeadIsName[T <: HList]: Named[FieldType[nameWitness.T, String] :: T] =
    (t: FieldType[nameWitness.T, String] :: T) => t.head

  implicit def canAppendAnythingToHListWithNamedAndGetNamed[H, T <: HList](
    implicit N: Named[T]): Named[H :: T] = (t: H :: T) => N.name(t.tail)

}
