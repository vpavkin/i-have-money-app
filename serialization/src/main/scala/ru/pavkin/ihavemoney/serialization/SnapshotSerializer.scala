package ru.pavkin.ihavemoney.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.serialization.BaseSerializer
import ru.pavkin.ihavemoney.domain.fortune.Fortune
import ru.pavkin.ihavemoney.proto.snapshots.PBFortune
import ru.pavkin.ihavemoney.serialization.ProtobufSuite.syntax._
import ru.pavkin.ihavemoney.serialization.implicits._

class SnapshotSerializer(val system: ExtendedActorSystem)
  extends BaseSerializer {

  lazy val logger: LoggingAdapter = Logging.getLogger(system, this)

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case fortune: Fortune =>
      fortune.encode.toByteArray
    case _ =>
      throw new RuntimeException(s"No serializer found for ${o.getClass.getName}")
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val L = classOf[Fortune]
    manifest match {
      case Some(L) =>
        fortuneSuite.decode(PBFortune.parseFrom(bytes))
      case Some(m) =>
        throw new RuntimeException(s"No deserializer found for ${m.getName}")
      case None =>
        throw new RuntimeException("No manifest found.")
    }
  }
}
