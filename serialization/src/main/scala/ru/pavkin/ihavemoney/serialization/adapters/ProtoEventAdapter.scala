package ru.pavkin.ihavemoney.serialization.adapters

import akka.persistence.journal.{EventAdapter, EventSeq}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.proto.events._
import ru.pavkin.ihavemoney.serialization.ProtobufSuite.syntax._
import ru.pavkin.ihavemoney.serialization.implicits._

class ProtoEventAdapter extends EventAdapter with DomainEventTagAdapter with FortuneProtobufAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case e: FortuneEvent ⇒ e match {
      case m: FortuneCreated ⇒ tag(m.encode, m.metadata)
      case m: EditorAdded ⇒ tag(m.encode, m.metadata)
      case m: FortuneIncreased ⇒ tag(m.encode, m.metadata)
      case m: FortuneSpent ⇒ tag(m.encode, m.metadata)
      case m: FortuneInitializationFinished ⇒ tag(m.encode, m.metadata)
      case m: AssetAcquired ⇒ tag(m.encode, m.metadata)
      case m: AssetSold ⇒ tag(m.encode, m.metadata)
      case m: AssetPriceChanged ⇒ tag(m.encode, m.metadata)
      case m: LiabilityTaken ⇒ tag(m.encode, m.metadata)
      case m: LiabilityPaidOff ⇒ tag(m.encode, m.metadata)
      case m: CurrencyExchanged ⇒ tag(m.encode, m.metadata)
      case m: LimitsUpdated ⇒ tag(m.encode, m.metadata)
      case m: TransactionCancelled ⇒ tag(m.encode, m.metadata)
    }
    case e: UserEvent ⇒ e match {
      case m: UserCreated ⇒ tag(m.encode, m.metadata)
      case m: UserConfirmed ⇒ tag(m.encode, m.metadata)
      case m: ConfirmationEmailSent ⇒ tag(m.encode, m.metadata)
      case m: UserLoggedIn ⇒ tag(m.encode, m.metadata)
      case m: UserFailedToLogIn ⇒ tag(m.encode, m.metadata)
    }
    case _ ⇒ event
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = EventSeq.single(deserialize(event))
}
