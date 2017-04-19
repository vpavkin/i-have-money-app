package ru.pavkin.ihavemoney.writefront

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.pattern.ask
import akka.util.Timeout
import io.circe.syntax._
import io.circe.{Encoder, Json}
import io.funcqrs.{AggregateId, CommandIdFacet, DomainCommand}
import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.proto.results.{InvalidCommand, UnexpectedFailure, UnknownCommand}
import ru.pavkin.ihavemoney.protocol.writefront._
import ru.pavkin.ihavemoney.protocol.{CommandProcessed, FailedRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class WriteBackClusterClient(system: ActorSystem) {

  private val writeBackendClient: ActorRef = system.actorOf(
    ClusterClient.props(ClusterClientSettings(system)),
    "writeBackendClient"
  )

  private val standardPF: PartialFunction[Any, (StatusCode, Json)] = {
    case UnknownCommand(c) ⇒
      InternalServerError → FailedRequest("", s"Unknown command $c").asJson
    case InvalidCommand(id, reason) ⇒
      BadRequest → FailedRequest(id, reason).asJson
    case UnexpectedFailure(id, reason) ⇒
      InternalServerError → FailedRequest(id, reason).asJson
  }

  private def send[Id <: AggregateId](aggregateId: Id, command: DomainCommand)
    (implicit timeout: Timeout): Future[Any] =
    writeBackendClient ? ClusterClient.Send("/user/interface", CommandEnvelope(aggregateId.value, command), localAffinity = true)

  def sendCommand[E: ClassTag, Id <: AggregateId](aggregateId: Id, command: DomainCommand)
    (eventHandler: E ⇒ (StatusCode, Json))
    (implicit ec: ExecutionContext, timeout: Timeout): Future[(StatusCode, Json)] =
    send(aggregateId, command).collect {
      standardPF.orElse {
        case (evt: E) :: Nil ⇒
          eventHandler(evt)
      }
    }

  def sendCommandAndIgnoreResult[Id <: AggregateId](
    aggregateId: Id,
    command: DomainCommand with CommandIdFacet)(
    implicit ec: ExecutionContext,
    timeout: Timeout): Future[(StatusCode, Json)] =
    send(aggregateId, command).collect(
      standardPF.orElse {
        case head :: tail ⇒
          OK → CommandProcessed(command.id.value).asJson
      }
    )
}
