package ru.pavkin.ihavemoney.serialization

import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import shapeless.{Generic, HList}

trait ProtobufSuite[M, PB <: GeneratedMessage with Message[PB]] {
  def encode(m: M): PB
  def decode(p: PB): M
  def companion: GeneratedMessageCompanion[PB]
  def protobufFromBytes(bytes: Array[Byte]): PB = companion.parseFrom(bytes)
  def fromBytes(bytes: Array[Byte]): M = decode(protobufFromBytes(bytes))
  def toBytes(message: M): Array[Byte] = encode(message).toByteArray
}

object ProtobufSuite {

  object syntax {
    implicit class MessageOps[M, PB <: GeneratedMessage with Message[PB]](m: M)(implicit ev: ProtobufSuite[M, PB]) {
      def encode: PB = ev.encode(m)
      def toBytes: Array[Byte] = ev.toBytes(m)
    }

    implicit class ProtobufOps[M, PB <: GeneratedMessage with Message[PB]](p: PB)(implicit ev: ProtobufSuite[M, PB]) {
      def decode: M = ev.decode(p)
    }
  }

  def auto[M, PB <: GeneratedMessage with Message[PB]] = new Helper[M, PB]

  class Helper[M, PB <: GeneratedMessage with Message[PB]] {
    def identicallyShaped[MRepr <: HList, PBRepr <: HList](comp: GeneratedMessageCompanion[PB])
                                                          (implicit L1: Generic.Aux[M, MRepr],
                                                           L2: Generic.Aux[PB, PBRepr],
                                                           EQ1: MRepr =:= PBRepr,
                                                           EQ2: PBRepr =:= MRepr) =
      new ProtobufSuite[M, PB] {
        def encode(m: M): PB = L2.from(EQ1(L1.to(m)))
        def decode(p: PB): M = L1.from(EQ2(L2.to(p)))
        def companion: GeneratedMessageCompanion[PB] = comp
      }

    def hlist[MRepr <: HList, PBRepr <: HList](to: MRepr ⇒ PBRepr,
                                               from: PBRepr ⇒ MRepr,
                                               comp: GeneratedMessageCompanion[PB])
                                              (implicit L1: Generic.Aux[M, MRepr],
                                               L2: Generic.Aux[PB, PBRepr]) =
      new ProtobufSuite[M, PB] {
        def encode(m: M): PB = L2.from(to(L1.to(m)))
        def decode(p: PB): M = L1.from(from(L2.to(p)))
        def companion: GeneratedMessageCompanion[PB] = comp
      }
  }

}
