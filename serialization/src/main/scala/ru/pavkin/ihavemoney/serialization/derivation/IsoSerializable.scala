package ru.pavkin.ihavemoney.serialization.derivation

import cats.Traverse
import cats.syntax.functor._
import shapeless.{::, Generic, HList, Lazy}

trait IsoSerializable[S, R] { self =>
  def serialize(t: S): R
  def deserialize(t: R): S

  /**
    * Creates a new Protocol from [[S]] to [[R2]] by using supplied invariant mapping functions
    * on the representation values of the Protocol
    *
    * @param to   convert from R to R2
    * @param from convert from R2 to R
    */
  def inmapR[R2](to: R => R2, from: R2 => R): IsoSerializable[S, R2] = new IsoSerializable[S, R2] {
    def serialize(model: S): R2 = to(self.serialize(model))
    def deserialize(repr: R2): S = self.deserialize(from(repr))
  }
}

object IsoSerializable extends IsoSerializableImplicits {
  object syntax {
    implicit class IsoSerializableOps[T](t: T) {
      def serialize[R](implicit IS: IsoSerializable[T, R]): R = IS.serialize(t)
      def deserialize[S](implicit IS: IsoSerializable[S, T]): S = IS.deserialize(t)
    }
  }

  def withString[T](to: T ⇒ String, from: String ⇒ T) = new IsoSerializable[T, String] {
    def serialize(t: T): String = to(t)
    def deserialize(t: String): T = from(t)
  }
}

trait IsoSerializableImplicits extends LowLevelIsoSerializableImplicits {

  implicit def mapSerializable[KS, VS, KR, VR](
    implicit IK: IsoSerializable[KS, KR],
    IV: IsoSerializable[VS, VR]): IsoSerializable[Map[KS, VS], Map[KR, VR]] =
    new IsoSerializable[Map[KS, VS], Map[KR, VR]] {
      def serialize(t: Map[KS, VS]): Map[KR, VR] = t.map { case (k, v) ⇒ IK.serialize(k) -> IV.serialize(v) }
      def deserialize(t: Map[KR, VR]): Map[KS, VS] = t.map { case (k, v) ⇒ IK.deserialize(k) -> IV.deserialize(v) }
    }

  implicit def identitySerializable[T]: IsoSerializable[T, T] = new IsoSerializable[T, T] {
    def serialize(t: T): T = t
    def deserialize(t: T): T = t
  }

  implicit def traverseSerializable[M, R, L[_] : Traverse](implicit P: IsoSerializable[M, R]) =
    new IsoSerializable[L[M], L[R]] {
      def serialize(model: L[M]): L[R] = model.map(P.serialize)
      def deserialize(repr: L[R]): L[M] = repr.map(P.deserialize)
    }
}

trait LowLevelIsoSerializableImplicits {
  implicit def hlistSerializable[SH, ST <: HList, RH, RT <: HList](
    implicit ISH: IsoSerializable[SH, RH],
    IST: IsoSerializable[ST, RT]) =
    new IsoSerializable[SH :: ST, RH :: RT] {
      def serialize(t: SH :: ST): RH :: RT = ISH.serialize(t.head) :: IST.serialize(t.tail)
      def deserialize(t: RH :: RT): SH :: ST = ISH.deserialize(t.head) :: IST.deserialize(t.tail)
    }

  implicit def genSerializable[S, SRepr <: HList, T, TRepr <: HList](
    implicit SG: Generic.Aux[S, SRepr],
    TG: Generic.Aux[T, TRepr],
    IS: Lazy[IsoSerializable[SRepr, TRepr]]): IsoSerializable[S, T] =
    new IsoSerializable[S, T] {
      def serialize(t: S): T = TG.from(IS.value.serialize(SG.to(t)))
      def deserialize(t: T): S = SG.from(IS.value.deserialize(TG.to(t)))
    }
}
