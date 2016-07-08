package ru.pavkin.ihavemoney.readback.projections

import akka.actor.ActorContext
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import io.funcqrs.akka.EventsSourceProvider

import scala.concurrent.{ExecutionContext, Future}

trait CollectProjection[E] {

  implicit def materializer: Materializer
  implicit def actorContext: ActorContext

  def source: EventsSourceProvider
  def collector: PartialFunction[Any, E]
  def run(implicit ec: ExecutionContext): Future[List[E]] = source.source(0)
    .map(_.event)
    .collect(collector)
    .runWith(Sink.seq).map(_.toList)
}
