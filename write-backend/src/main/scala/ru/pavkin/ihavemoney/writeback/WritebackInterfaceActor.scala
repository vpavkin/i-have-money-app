package ru.pavkin.ihavemoney.writeback

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.FortuneCommand
import ru.pavkin.ihavemoney.domain.user.UserProtocol.UserCommand
import ru.pavkin.ihavemoney.proto.results.UnknownCommand

import scala.concurrent.ExecutionContext

class WritebackInterfaceActor(fortuneShardRegion: ActorRef,
                              userShardRegion: ActorRef)
                             (implicit val timeout: Timeout) extends Actor {
  implicit val dispatcher: ExecutionContext = context.system.dispatcher

  def receive: Receive = {
    case env: CommandEnvelope ⇒
      val origin = sender
      env.command match {
        case cmd: FortuneCommand ⇒
          (fortuneShardRegion ? env).foreach(origin ! _)
        case user: UserCommand ⇒
          (userShardRegion ? env).foreach(origin ! _)
        case other ⇒
          origin ! UnknownCommand(other.getClass.getName)
      }

  }
}
