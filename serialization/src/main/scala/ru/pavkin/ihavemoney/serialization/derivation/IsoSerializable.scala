package ru.pavkin.ihavemoney.serialization.derivation

import shapeless.{::, Generic, HList, Lazy}

trait IsoSerializable[S, R] {
  def serialize(t: S): R
  def deserialize(t: R): S
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

  implicit def mapSerializable[KS, VS, KR, VR](implicit IK: IsoSerializable[KS, KR],
                                               IV: IsoSerializable[VS, VR]): IsoSerializable[Map[KS, VS], Map[KR, VR]] =
    new IsoSerializable[Map[KS, VS], Map[KR, VR]] {
      def serialize(t: Map[KS, VS]): Map[KR, VR] = t.map { case (k, v) ⇒ IK.serialize(k) -> IV.serialize(v) }
      def deserialize(t: Map[KR, VR]): Map[KS, VS] = t.map { case (k, v) ⇒ IK.deserialize(k) -> IV.deserialize(v) }
    }

  implicit def identitySerializable[T]: IsoSerializable[T, T] = new IsoSerializable[T, T] {
    def serialize(t: T): T = t
    def deserialize(t: T): T = t
  }
}

trait LowLevelIsoSerializableImplicits {
  implicit def hlistSerializable[SH, ST <: HList, RH, RT <: HList](implicit ISH: IsoSerializable[SH, RH],
                                                                   IST: IsoSerializable[ST, RT]) =
    new IsoSerializable[SH :: ST, RH :: RT] {
      def serialize(t: SH :: ST): RH :: RT = ISH.serialize(t.head) :: IST.serialize(t.tail)
      def deserialize(t: RH :: RT): SH :: ST = ISH.deserialize(t.head) :: IST.deserialize(t.tail)
    }

  implicit def genSerializable[S, SRepr <: HList, T, TRepr <: HList](implicit SG: Generic.Aux[S, SRepr],
                                                                     TG: Generic.Aux[T, TRepr],
                                                                     IS: Lazy[IsoSerializable[SRepr, TRepr]]): IsoSerializable[S, T] =
    new IsoSerializable[S, T] {
      def serialize(t: S): T = TG.from(IS.value.serialize(SG.to(t)))
      def deserialize(t: T): S = SG.from(IS.value.deserialize(TG.to(t)))
    }
}
