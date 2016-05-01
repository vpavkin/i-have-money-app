package ru.pavkin.ihavemoney.writefront

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.pattern.ask
import akka.util.Timeout
import io.circe.{Encoder, Json}
import io.funcqrs.DomainCommand
import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.proto.results.{InvalidCommand, UnexpectedFailure, UnknownCommand}
import ru.pavkin.ihavemoney.protocol.writefront._
import io.circe.syntax._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class WriteBackClusterClient(system: ActorSystem) {

  private val writeBackendClient: ActorRef = system.actorOf(
    ClusterClient.props(ClusterClientSettings(system)),
    "writeBackendClient"
  )

  private val standardPF: PartialFunction[Any, (StatusCode, Json)] = {
    case UnknownCommand(c) ⇒
      InternalServerError → RequestResult.failure("unassigned", s"Unknown command $c").asJson
    case InvalidCommand(id, reason) ⇒
      BadRequest → RequestResult.failure(id, reason).asJson
    case UnexpectedFailure(id, reason) ⇒
      InternalServerError → RequestResult.failure(id, reason).asJson
  }

  private def send(aggregateId: String, command: DomainCommand)
                  (implicit timeout: Timeout): Future[Any] =
    writeBackendClient ? ClusterClient.Send("/user/interface", CommandEnvelope(aggregateId, command), localAffinity = true)

  def sendCommand[E: ClassTag, R: Encoder](aggregateId: String, command: DomainCommand)
                                          (eventHandler: E ⇒ (StatusCode, RequestResult[R]))
                                          (implicit ec: ExecutionContext, timeout: Timeout): Future[(StatusCode, Json)] =
    send(aggregateId, command).collect {
      standardPF.orElse {
        case (evt: E) :: Nil ⇒
          val (code, res) = eventHandler(evt)
          code → res.copy(commandId = command.id.value.toString).asJson
      }
    }

  def sendCommandAndIgnoreResult(aggregateId: String, command: DomainCommand)
                                (implicit ec: ExecutionContext, timeout: Timeout): Future[(StatusCode, Json)] =
    send(aggregateId, command).collect(
      standardPF.orElse {
        case head :: tail ⇒
          OK → RequestResult.justSuccess(command.id.value.toString).asJson
      }
    )
}
