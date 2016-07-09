package ru.pavkin.ihavemoney.readback.sources

import akka.NotUsed
import akka.actor.ActorContext
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.scaladsl.Source
import io.funcqrs.Tag
import io.funcqrs.akka.EventsSourceProvider
import ru.pavkin.ihavemoney.serialization.adapters.FortuneProtobufAdapter

class CurrentFortuneTagEventSourceProvider(tag: Tag) extends EventsSourceProvider with FortuneProtobufAdapter {

  def source(offset: Long)(implicit context: ActorContext): Source[EventEnvelope, NotUsed] = {

    val journal = PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    journal.currentEventsByTag(tag.value, offset)
        .map(e ⇒ e.copy(event = deserialize(e.event)))
  }

}
