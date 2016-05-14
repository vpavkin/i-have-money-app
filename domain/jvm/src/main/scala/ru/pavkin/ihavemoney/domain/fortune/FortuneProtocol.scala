package ru.pavkin.ihavemoney.domain.fortune

import java.time.OffsetDateTime
import java.util.UUID

import io.funcqrs._
import ru.pavkin.ihavemoney.domain.user.UserId

case class FortuneId(value: String) extends AggregateId
object FortuneId {
  def fromString(aggregateId: String): FortuneId = FortuneId(aggregateId)
  def generate: FortuneId = FortuneId(UUID.randomUUID().toString)
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

  sealed trait InitializedFortuneAdjustmentCommand extends FortuneAdjustmentCommand {
    def initializer = false
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

  case class BuyAsset(user: UserId,
                      asset: Asset,
                      initializer: Boolean = false,
                      comment: Option[String] = None) extends FortuneAdjustmentCommand

  sealed trait AssetManipulationCommand extends InitializedFortuneAdjustmentCommand {
    def assetId: AssetId
  }
  case class SellAsset(user: UserId,
                       assetId: AssetId,
                       comment: Option[String] = None) extends AssetManipulationCommand
  // todo: implement later
  //  case class BuyMoreStocks(user: UserId,
  //                           assetId: AssetId,
  //                           count: BigDecimal,
  //                           comment: Option[String] = None) extends AssetManipulationCommand
  //
  //  case class SellSomeStocks(user: UserId,
  //                            assetId: AssetId,
  //                            count: BigDecimal,
  //                            comment: Option[String] = None) extends AssetManipulationCommand

  /* Reevaluate per-stock worth for stocks, whole asset worth otherwise*/
  case class ReevaluateAsset(user: UserId,
                             assetId: AssetId,
                             newPrice: BigDecimal,
                             comment: Option[String] = None) extends AssetManipulationCommand

  case class TakeOnLiability(user: UserId,
                             liability: Liability,
                             initializer: Boolean = false,
                             comment: Option[String] = None) extends FortuneAdjustmentCommand

  sealed trait LiabilityManipulationCommand extends InitializedFortuneAdjustmentCommand {
    def liabilityId: LiabilityId
  }

  case class PayLiabilityOff(user: UserId,
                             liabilityId: LiabilityId,
                             byAmount: BigDecimal,
                             comment: Option[String] = None) extends LiabilityManipulationCommand

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
                              initializer: Boolean = false,
                              metadata: FortuneMetadata,
                              comment: Option[String] = None) extends FortuneEvent
  case class FortuneSpent(user: UserId,
                          amount: BigDecimal,
                          currency: Currency,
                          category: ExpenseCategory,
                          initializer: Boolean = false,
                          metadata: FortuneMetadata,
                          comment: Option[String] = None) extends FortuneEvent

  case class FortuneInitializationFinished(user: UserId,
                                           metadata: FortuneMetadata) extends FortuneEvent

  case class AssetAcquired(user: UserId,
                           assetId: AssetId,
                           asset: Asset,
                           initializer: Boolean = false,
                           metadata: FortuneMetadata,
                           comment: Option[String] = None) extends FortuneEvent

  case class AssetSold(user: UserId,
                       assetId: AssetId,
                       metadata: FortuneMetadata,
                       comment: Option[String] = None) extends FortuneEvent

  // todo: implement later
  //  case class AdditionalStocksAcquired(user: UserId,
  //                                      assetId: AssetId,
  //                                      count: BigDecimal,
  //                                      metadata: FortuneMetadata,
  //                                      comment: Option[String] = None) extends FortuneEvent
  //
  //  case class StocksPartiallySold(user: UserId,
  //                                 assetId: AssetId,
  //                                 count: BigDecimal,
  //                                 metadata: FortuneMetadata,
  //                                 comment: Option[String] = None) extends FortuneEvent

  /* Reevaluate per-stock worth for stocks, whole asset worth otherwise*/
  case class AssetWorthChanged(user: UserId,
                               assetId: AssetId,
                               newAmount: BigDecimal,
                               metadata: FortuneMetadata,
                               comment: Option[String] = None) extends FortuneEvent

  case class LiabilityTaken(user: UserId,
                            liabilityId: LiabilityId,
                            liability: Liability,
                            initializer: Boolean = false,
                            metadata: FortuneMetadata,
                            comment: Option[String] = None) extends FortuneEvent

  case class LiabilityPaidOff(user: UserId,
                              liabilityId: LiabilityId,
                              amount: BigDecimal,
                              metadata: FortuneMetadata,
                              comment: Option[String] = None) extends FortuneEvent

  /*-------------------Metadata---------------------*/
  case class FortuneMetadata(aggregateId: FortuneId,
                             commandId: CommandId,
                             eventId: EventId = EventId(),
                             date: OffsetDateTime = OffsetDateTime.now(),
                             tags: Set[Tag] = Set()) extends Metadata with JavaTime {
    type Id = FortuneId
  }
}
