package ru.pavkin.ihavemoney.domain.fortune

import java.time.{LocalDate, OffsetDateTime}
import java.util.UUID

import io.funcqrs._
import ru.pavkin.ihavemoney.domain.user.UserId

case class FortuneId(value: String) extends AggregateId
object FortuneId {
  def fromString(aggregateId: String): FortuneId = FortuneId(aggregateId)
  def generate: FortuneId = FortuneId(UUID.randomUUID().toString)
}

object FortuneProtocol extends ProtocolLike {

  /*-------------------Commands---------------------*/
  sealed trait FortuneCommand extends ProtocolCommand with CommandIdFacet {
    def user: UserId
  }

  case class CreateFortune(
    id: CommandId,
    owner: UserId) extends FortuneCommand {
    def user: UserId = owner
  }
  case class AddEditor(
    id: CommandId,
    user: UserId, editor: UserId) extends FortuneCommand

  sealed trait FortuneAdjustmentCommand extends FortuneCommand {
    def initializer: Boolean
  }

  case class FinishInitialization(
    id: CommandId,
    user: UserId) extends FortuneAdjustmentCommand {
    def initializer = true
  }

  sealed trait InitializedFortuneAdjustmentCommand extends FortuneAdjustmentCommand {
    def initializer = false
  }

  case class Spend(
    id: CommandId,
    user: UserId,
    amount: BigDecimal,
    currency: Currency,
    category: ExpenseCategory,
    overrideDate: Option[LocalDate] = None,
    initializer: Boolean = false,
    comment: Option[String] = None) extends FortuneAdjustmentCommand

  case class ReceiveIncome(
    id: CommandId,
    user: UserId,
    amount: BigDecimal,
    currency: Currency,
    category: IncomeCategory,
    initializer: Boolean = false,
    comment: Option[String] = None) extends FortuneAdjustmentCommand

  case class ExchangeCurrency(
    id: CommandId,
    user: UserId,
    fromAmount: BigDecimal,
    fromCurrency: Currency,
    toAmount: BigDecimal,
    toCurrency: Currency,
    comment: Option[String] = None) extends InitializedFortuneAdjustmentCommand {
    require(fromCurrency != toCurrency)
  }

  // issues correction events with a special category
  case class CorrectBalances(
    id: CommandId,
    user: UserId,
    realBalances: Map[Currency, BigDecimal],
    comment: Option[String] = None) extends InitializedFortuneAdjustmentCommand

  case class BuyAsset(
    id: CommandId,
    user: UserId,
    asset: Asset,
    initializer: Boolean = false,
    comment: Option[String] = None) extends FortuneAdjustmentCommand

  sealed trait AssetManipulationCommand extends InitializedFortuneAdjustmentCommand {
    def assetId: AssetId
  }
  case class SellAsset(
    id: CommandId,
    user: UserId,
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
  case class ReevaluateAsset(
    id: CommandId,
    user: UserId,
    assetId: AssetId,
    newPrice: BigDecimal,
    comment: Option[String] = None) extends AssetManipulationCommand

  case class TakeOnLiability(
    id: CommandId,
    user: UserId,
    liability: Liability,
    initializer: Boolean = false,
    comment: Option[String] = None) extends FortuneAdjustmentCommand

  sealed trait LiabilityManipulationCommand extends InitializedFortuneAdjustmentCommand {
    def liabilityId: LiabilityId
  }

  case class PayLiabilityOff(
    id: CommandId,
    user: UserId,
    liabilityId: LiabilityId,
    byAmount: BigDecimal,
    comment: Option[String] = None) extends LiabilityManipulationCommand

  case class UpdateLimits(
    id: CommandId,
    user: UserId,
    weekly: Map[ExpenseCategory, Worth],
    monthly: Map[ExpenseCategory, Worth]) extends FortuneCommand

  case class CancelTransaction(
    id: CommandId,
    user: UserId,
    transactionId: UUID) extends FortuneCommand

  /*-------------------Events---------------------*/
  sealed trait FortuneEvent extends ProtocolEvent with MetadataFacet[FortuneMetadata]

  case class FortuneCreated(
    owner: UserId,
    metadata: FortuneMetadata) extends FortuneEvent

  case class EditorAdded(
    editor: UserId,
    metadata: FortuneMetadata) extends FortuneEvent

  case class FortuneIncreased(
    user: UserId,
    amount: BigDecimal,
    currency: Currency,
    category: IncomeCategory,
    initializer: Boolean = false,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class FortuneSpent(
    user: UserId,
    amount: BigDecimal,
    currency: Currency,
    category: ExpenseCategory,
    overrideDate: Option[LocalDate] = None,
    initializer: Boolean = false,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class CurrencyExchanged(
    user: UserId,
    fromAmount: BigDecimal,
    fromCurrency: Currency,
    toAmount: BigDecimal,
    toCurrency: Currency,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent {
    require(fromCurrency != toCurrency)
  }

  case class FortuneInitializationFinished(
    user: UserId,
    metadata: FortuneMetadata) extends FortuneEvent

  case class AssetAcquired(
    user: UserId,
    assetId: AssetId,
    asset: Asset,
    initializer: Boolean = false,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class AssetSold(
    user: UserId,
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
  case class AssetPriceChanged(
    user: UserId,
    assetId: AssetId,
    newPrice: BigDecimal,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class LiabilityTaken(
    user: UserId,
    liabilityId: LiabilityId,
    liability: Liability,
    initializer: Boolean = false,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class LiabilityPaidOff(
    user: UserId,
    liabilityId: LiabilityId,
    amount: BigDecimal,
    metadata: FortuneMetadata,
    comment: Option[String] = None) extends FortuneEvent

  case class LimitsUpdated(
    user: UserId,
    weekly: Map[ExpenseCategory, Worth],
    monthly: Map[ExpenseCategory, Worth],
    metadata: FortuneMetadata) extends FortuneEvent

  case class TransactionCancelled(
    user: UserId,
    transactionId: UUID,
    metadata: FortuneMetadata) extends FortuneEvent

  /*-------------------Metadata---------------------*/
  case class FortuneMetadata(
    aggregateId: FortuneId,
    commandId: CommandId,
    eventId: EventId = EventId(),
    date: OffsetDateTime = OffsetDateTime.now(),
    tags: Set[Tag] = Set()) extends Metadata with JavaTime {
    type Id = FortuneId
  }
}
