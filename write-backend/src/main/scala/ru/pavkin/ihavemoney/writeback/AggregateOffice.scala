package ru.pavkin.ihavemoney.writeback

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import io.funcqrs.AggregateLike
import io.funcqrs.akka.backend.AkkaBackend
import ru.pavkin.ihavemoney.domain._
import ru.pavkin.ihavemoney.domain.errors.DomainError
import ru.pavkin.ihavemoney.proto.results.{InvalidCommand, UnexpectedFailure, UnknownCommand}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class AggregateOffice[T <: AggregateLike : ClassTag, C <: T#Protocol#ProtocolCommand : ClassTag]
(backend: AkkaBackend, idFactory: String ⇒ T#Id)
(implicit val timeout: Timeout) extends Actor with ActorLogging {

  implicit val dispatcher: ExecutionContext = context.system.dispatcher

  def receive: Receive = {
    case CommandEnvelope(id, command) ⇒
      val origin = sender
      command match {
        case c: C ⇒
          val aggregate = backend.aggregateRef[T](idFactory(id))
          (aggregate ? c).onComplete {
            case Success(events) ⇒ origin ! events
            case Failure(e: DomainError) ⇒ origin ! InvalidCommand(c.id.value.toString, e.message)
            case Failure(e) ⇒ origin ! UnexpectedFailure(c.id.value.toString, e.getMessage)
          }
        case other ⇒ sender ! UnknownCommand(other.getClass.getName)
      }
  }
}
