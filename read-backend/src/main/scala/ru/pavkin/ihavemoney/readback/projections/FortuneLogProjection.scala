package ru.pavkin.ihavemoney.readback.projections

import java.time.Year

import akka.actor.ActorContext
import akka.stream.{ActorMaterializer, Materializer}
import io.funcqrs.akka.EventsSourceProvider
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import cats.syntax.eq._
import cats.instances.int._

class FortuneLogProjection(
  val id: FortuneId,
  override val source: EventsSourceProvider,
  year: Year)(
  override implicit val actorContext: ActorContext) extends CollectProjection[FortuneEvent] {

  override implicit val materializer: Materializer = ActorMaterializer()

  def collector: PartialFunction[Any, FortuneEvent] = {
    case e: FortuneIncreased if e.date.getYear === year.getValue && e.aggregateId === id && !e.initializer ⇒ e
    case e: FortuneSpent if e.date.getYear === year.getValue && e.aggregateId === id && !e.initializer ⇒ e
    case e: CurrencyExchanged if e.date.getYear === year.getValue && e.aggregateId === id ⇒ e
    case e: TransactionCancelled if e.aggregateId === id => e
  }

}
