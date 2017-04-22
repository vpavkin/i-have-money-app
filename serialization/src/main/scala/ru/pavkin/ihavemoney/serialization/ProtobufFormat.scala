package ru.pavkin.ihavemoney.serialization

import com.trueaccord.scalapb.GeneratedMessage
import ru.pavkin.ihavemoney.serialization.derivation.Protocol

/**
  * Typeclass that provides a serialization isomorphism for arbitrary type D and
  * some protobuf wrapper class P
  *
  * Decoding is performed under ```Either[Throwable, _]``` effect
  *
  * @tparam D domain model
  * @tparam P protobuf representation
  */
abstract class ProtobufFormat[D, P <: GeneratedMessage] {
  def encode(domainEntity: D): P
  def decode(protobufMessage: P): Protocol.Try.Effect[D]
}

object ProtobufFormat {

  object syntax {
    implicit class DomainEntityOps[D, P <: GeneratedMessage](
      m: D)(
      implicit ev: ProtobufFormat[D, P]) {
      def encode: P = ev.encode(m)
    }

    implicit class ProtobufMessageOps[D, P <: GeneratedMessage](
      p: P)(
      implicit ev: ProtobufFormat[D, P]) {
      def decode: Protocol.Try.Effect[D] = ev.decode(p)
    }
  }

  /**
    * Derives [[ProtobufFormat]] from [[ru.pavkin.ihavemoney.serialization.derivation.Protocol.Try]] instance
    */
  def fromProtocol[D, P <: GeneratedMessage](
    implicit protocol: Protocol.Try[D, P]): ProtobufFormat[D, P] = new ProtobufFormat[D, P] {
    def encode(m: D): P = protocol.serialize(m)
    def decode(p: P): Protocol.Try.Effect[D] = protocol.deserialize(p)
  }
}
