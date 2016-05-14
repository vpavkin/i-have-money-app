package ru.pavkin.ihavemoney.domain.fortune

import java.time.OffsetDateTime
import java.util.UUID

import io.funcqrs._
import ru.pavkin.ihavemoney.domain.user.UserId

case class FortuneId(value: String) extends AggregateId
object FortuneId {
  def fromString(aggregateId: String): FortuneId = FortuneId(aggregateId)
  def generate(): FortuneId = FortuneId(UUID.randomUUID().toString)
}

object FortuneProtocol extends ProtocolLike {

  case class ExpenseCategory(name: String)
  case class IncomeCategory(name: String)

  /*-------------------Commands---------------------*/
  sealed trait FortuneCommand extends ProtocolCommand

  case class CreateFortune(owner: UserId) extends FortuneCommand
  case class AddEditor(user: UserId, editor: UserId) extends FortuneCommand

  sealed trait FortuneAdjustmentCommand extends FortuneCommand {
    def user: UserId
    def initializer: Boolean
  }

  case class FinishInitialization(user: UserId) extends FortuneAdjustmentCommand {
    def initializer = true
  }

  case class Spend(user: UserId,
                   amount: BigDecimal,
                   currency: Currency,
                   category: ExpenseCategory,
                   initializer: Boolean = false,
                   comment: Option[String] = None) extends FortuneAdjustmentCommand

  case class ReceiveIncome(user: UserId,
                           amount: BigDecimal,
                           currency: Currency,
                           category: IncomeCategory,
                           initializer: Boolean = false,
                           comment: Option[String] = None) extends FortuneAdjustmentCommand

  /*-------------------Events---------------------*/
  sealed trait FortuneEvent extends ProtocolEvent with MetadataFacet[FortuneMetadata]

  case class FortuneCreated(owner: UserId,
                            metadata: FortuneMetadata) extends FortuneEvent

  case class EditorAdded(editor: UserId,
                         metadata: FortuneMetadata) extends FortuneEvent

  case class FortuneIncreased(user: UserId,
                              amount: BigDecimal,
                              currency: Currency,
                              category: IncomeCategory,
                              metadata: FortuneMetadata,
                              initializer: Boolean = false,
                              comment: Option[String] = None) extends FortuneEvent
  case class FortuneSpent(user: UserId,
                          amount: BigDecimal,
                          currency: Currency,
                          category: ExpenseCategory,
                          metadata: FortuneMetadata,
                          initializer: Boolean = false,
                          comment: Option[String] = None) extends FortuneEvent

  case class FortuneInitializationFinished(user: UserId,
                                           metadata: FortuneMetadata) extends FortuneEvent

  /*-------------------Metadata---------------------*/
  case class FortuneMetadata(aggregateId: FortuneId,
                             commandId: CommandId,
                             eventId: EventId = EventId(),
                             date: OffsetDateTime = OffsetDateTime.now(),
                             tags: Set[Tag] = Set()) extends Metadata with JavaTime {
    type Id = FortuneId
  }
}
