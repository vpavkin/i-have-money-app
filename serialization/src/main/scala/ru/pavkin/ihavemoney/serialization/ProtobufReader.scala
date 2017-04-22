package ru.pavkin.ihavemoney.serialization

import com.trueaccord.scalapb.GeneratedMessage
import ru.pavkin.ihavemoney.serialization.ProtobufFormat.syntax._

trait ProtobufReader {

  def decodeOrThrow[D, P <: GeneratedMessage](
    proto: P)(
    implicit P: ProtobufFormat[D, P]): D =
    proto.decode match {
      case Left(error) =>
        val message = s"Couldn't decode protobuf event $proto"
        throw new Exception(message, error)
      case Right(domainEvent) => domainEvent
    }

}
