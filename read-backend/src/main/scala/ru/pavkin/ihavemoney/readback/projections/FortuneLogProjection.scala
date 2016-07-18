package ru.pavkin.ihavemoney.readback.projections

import akka.actor.ActorContext
import akka.stream.{ActorMaterializer, Materializer}
import io.funcqrs.akka.EventsSourceProvider
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.{CurrencyExchanged, FortuneEvent, FortuneIncreased, FortuneSpent}

class FortuneLogProjection(val id: FortuneId, override val source: EventsSourceProvider)
    (override implicit val actorContext: ActorContext) extends CollectProjection[FortuneEvent] {


  override implicit val materializer: Materializer = ActorMaterializer()

  def collector: PartialFunction[Any, FortuneEvent] = {
    case e: FortuneIncreased if e.aggregateId == id && !e.initializer ⇒ e
    case e: FortuneSpent if e.aggregateId == id && !e.initializer ⇒ e
    case e: CurrencyExchanged if e.aggregateId == id ⇒ e
  }

}
