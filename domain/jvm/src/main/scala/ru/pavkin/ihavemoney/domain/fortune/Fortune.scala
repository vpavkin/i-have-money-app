package ru.pavkin.ihavemoney.domain.fortune

import java.time.LocalDate

import io.funcqrs._
import io.funcqrs.behavior._
import ru.pavkin.ihavemoney.domain.errors.{BalanceIsNotEnough, _}
import ru.pavkin.ihavemoney.domain.user.UserId

case class Fortune(
    id: FortuneId,
    balances: Map[Currency, BigDecimal],
    assets: Map[AssetId, Asset],
    liabilities: Map[LiabilityId, Liability],
    owner: UserId,
    editors: Set[UserId],
    weeklyLimits: Map[ExpenseCategory, Worth],
    monthlyLimits: Map[ExpenseCategory, Worth],
    initializationMode: Boolean = true) extends AggregateLike {

  type Id = FortuneId
  type Protocol = FortuneProtocol.type

  def canBeAdjustedBy(user: UserId): Boolean = owner == user || editors.contains(user)

  def increase(worth: Worth): Fortune =
    copy(balances = balances + (worth.currency -> (amount(worth.currency) + worth.amount)))

  def addAsset(id: AssetId, asset: Asset): Fortune =
    copy(assets = assets + (id → asset))

  def removeAsset(id: AssetId): Fortune =
    copy(assets = assets - id)

  def changeAssetPrice(id: AssetId, newPrice: BigDecimal): Fortune =
    copy(assets = assets.updated(id,
      assets(id) match {
        case s: CountedAsset ⇒
          s.copy(price = newPrice)
      }
    ))

  def addLiability(id: LiabilityId, liability: Liability): Fortune =
    copy(liabilities = liabilities + (id → liability))

  def payLiabilityOff(id: LiabilityId, byAmount: BigDecimal): Fortune = {
    val liab = liabilities(id)
    val f = if (liab.amount <= byAmount)
      copy(liabilities = liabilities - id)
    else
      copy(liabilities = liabilities.updated(
        id, liab.payOff(byAmount)
      ))
    f.decrease(liab.worth)
  }

  def decrease(by: Worth): Fortune =
    copy(balances = balances + (by.currency -> (amount(by.currency) - by.amount)))

  def exchange(from: Worth, to: Worth): Fortune = {
    require(from.currency != to.currency)
    require(this.amount(from.currency) >= from.amount)
    decrease(from).increase(to)
  }

  def worth(currency: Currency): Worth = Worth(amount(currency), currency)
  def amount(currency: Currency): BigDecimal = balances.getOrElse(currency, BigDecimal(0.0))

  def addEditor(user: UserId): Fortune =
    copy(editors = editors + user)

  import FortuneProtocol._

  def metadata(cmd: FortuneCommand): FortuneMetadata =
    Fortune.metadata(id, cmd)

  def cantSendInitializationCommandsAfterInitializationIsComplete = action[Fortune]
      .rejectCommand {
        case cmd: FortuneAdjustmentCommand if cmd.initializer && !initializationMode ⇒
          FortuneAlreadyInitialized(id)
      }

  def unauthorizedUserCanNotAdjustFortune = action[Fortune]
      .rejectCommand {
        case cmd: FortuneAdjustmentCommand if !this.canBeAdjustedBy(cmd.user) ⇒
          InsufficientAccessRights(cmd.user, this.id)
      }

  def onlyOwnerCanAddEditors = action[Fortune]
      .rejectCommand {
        case cmd: AddEditor if this.owner != cmd.user ⇒
          InsufficientAccessRights(cmd.user, this.id)
      }

  def cantHaveNegativeBalance = action[Fortune]
      .rejectCommand {
        case cmd: Spend if this.amount(cmd.currency) < cmd.amount ⇒
          BalanceIsNotEnough(this.amount(cmd.currency), cmd.currency)
        case cmd: ExchangeCurrency if this.amount(cmd.fromCurrency) < cmd.fromAmount ⇒
          BalanceIsNotEnough(this.amount(cmd.fromCurrency), cmd.fromCurrency)
      }

  def cantAdjustWithANegativeValue = action[Fortune]
      .rejectCommand {
        case cmd: Spend if cmd.amount <= 0 ⇒ NegativeAmount
        case cmd: ReceiveIncome if cmd.amount <= 0 ⇒ NegativeAmount
        case cmd: ExchangeCurrency if cmd.fromAmount <= 0 || cmd.toAmount <= 0 ⇒ NegativeAmount
        case cmd: CorrectBalances if cmd.realBalances.values.exists(_ < 0) ⇒ NegativeAmount
        case cmd: BuyAsset if cmd.asset.worth.amount < 0 ⇒ NegativeAmount
        case cmd: ReevaluateAsset if cmd.newPrice < 0 ⇒ NegativeAmount
        case cmd: TakeOnLiability if cmd.liability.worth.amount < 0 ⇒ NegativeAmount
        case cmd: PayLiabilityOff if cmd.byAmount < 0 ⇒ NegativeAmount
        case cmd: UpdateLimits if cmd.weekly.values.exists(_.amount < 0) || cmd.monthly.values.exists(_.amount < 0) ⇒ NegativeAmount
      }

  def cantAcquireAssetWithNotEnoughMoney = action[Fortune]
      .rejectCommand {
        case cmd: BuyAsset if !cmd.initializer && this.amount(cmd.asset.currency) < cmd.asset.worth.amount ⇒
          BalanceIsNotEnough(this.amount(cmd.asset.currency), cmd.asset.currency)
      }

  def cantManipulateAssetThatDoesNotExist = action[Fortune]
      .rejectCommand {
        case cmd: AssetManipulationCommand if !this.assets.contains(cmd.assetId) ⇒
          AssetNotFound(cmd.assetId)
      }

  def cantManipulateLiabilityThatDoesNotExist = action[Fortune]
      .rejectCommand {
        case cmd: LiabilityManipulationCommand if !this.liabilities.contains(cmd.liabilityId) ⇒
          LiabilityNotFound(cmd.liabilityId)
      }

  def ownerCanAddEditors = action[Fortune]
      .handleCommand {
        cmd: AddEditor ⇒ EditorAdded(cmd.editor, metadata(cmd))
      }
      .handleEvent {
        evt: EditorAdded ⇒ this.addEditor(evt.editor)
      }

  def editorsCanFinishInitialization = action[Fortune]
      .handleCommand {
        cmd: FinishInitialization ⇒ FortuneInitializationFinished(cmd.user, metadata(cmd))
      }
      .handleEvent {
        evt: FortuneInitializationFinished ⇒ copy(initializationMode = false)
      }

  def editorsCanUpdateLimits = action[Fortune]
      .handleCommand {
        cmd: UpdateLimits ⇒ LimitsUpdated(
          cmd.user,
          cmd.weekly,
          cmd.monthly,
          metadata(cmd)
        )
      }
      .handleEvent {
        evt: LimitsUpdated ⇒ this.copy(weeklyLimits = evt.weekly, monthlyLimits = evt.monthly)
      }

  def editorsCanExchangeCurrency = action[Fortune]
      .handleCommand {
        cmd: ExchangeCurrency ⇒ CurrencyExchanged(
          cmd.user,
          cmd.fromAmount, cmd.fromCurrency,
          cmd.toAmount, cmd.toCurrency,
          metadata(cmd),
          cmd.comment
        )
      }
      .handleEvent {
        evt: CurrencyExchanged ⇒ this.exchange(Worth(evt.fromAmount, evt.fromCurrency), Worth(evt.toAmount, evt.toCurrency))
      }

  def editorsCanPerformCorrections = action[Fortune]
      .handleCommand.manyEvents {
    cmd: CorrectBalances ⇒
      cmd.realBalances
          .map {
            case (curr, realAmount) ⇒ curr → (realAmount - this.amount(curr))
          }
          .filter(_._2 != BigDecimal(0))
          .map {
            case (curr, correction) ⇒
              if (correction > BigDecimal(0)) FortuneIncreased(cmd.user, correction, curr, IncomeCategory("Correction"), initializer = false, metadata(cmd))
              else FortuneSpent(cmd.user, -correction, curr, ExpenseCategory("Correction"), None, initializer = false, metadata(cmd))
          }.toList
  }
      .handleEvent {
        evt: FortuneIncreased ⇒ this.increase(Worth(evt.amount, evt.currency))
      }
      .handleEvent {
        evt: FortuneSpent ⇒ this.decrease(Worth(evt.amount, evt.currency))
      }

  def editorsCanBuyAssets = action[Fortune]
      .handleCommand.manyEvents[BuyAsset, FortuneEvent] {
    cmd: BuyAsset ⇒
      val assetId = AssetId.generate
      println(s"Creating new asset with id $assetId")
      val assetAcquired = AssetAcquired(
        cmd.user,
        assetId,
        cmd.asset,
        cmd.initializer,
        metadata(cmd),
        cmd.comment)
      if (cmd.initializer) List(
        FortuneIncreased(
          cmd.user,
          cmd.asset.worth.amount,
          cmd.asset.currency,
          IncomeCategory("Auto generated income"),
          initializer = true,
          metadata(cmd)),
        assetAcquired
      )
      else List(assetAcquired)
  }
      .handleEvent {
        evt: FortuneIncreased ⇒ this.increase(Worth(evt.amount, evt.currency))
      }
      .handleEvent {
        evt: AssetAcquired ⇒
          addAsset(evt.assetId, evt.asset)
              .decrease(evt.asset.worth)
      }

  def editorsCanSellAssets = action[Fortune]
      .handleCommand {
        cmd: SellAsset ⇒
          AssetSold(cmd.user, cmd.assetId, metadata(cmd), cmd.comment)
      }
      .handleEvent {
        evt: AssetSold ⇒
          val asset = assets(evt.assetId)
          removeAsset(evt.assetId)
              .increase(asset.worth)
      }

  def editorsCanReevaluateAssets = action[Fortune]
      .handleCommand {
        cmd: ReevaluateAsset ⇒
          AssetPriceChanged(cmd.user, cmd.assetId, cmd.newPrice, metadata(cmd), cmd.comment)
      }
      .handleEvent {
        evt: AssetPriceChanged ⇒
          changeAssetPrice(evt.assetId, evt.newPrice)
      }

  def editorsCanTakeOnLiabilities = action[Fortune]
      .handleCommand {
        cmd: TakeOnLiability ⇒
          val liabilityId = LiabilityId.generate
          println(s"Creating new liability with id $liabilityId")
          LiabilityTaken(
            cmd.user,
            liabilityId,
            cmd.liability,
            cmd.initializer,
            metadata(cmd),
            cmd.comment
          )
      }
      .handleEvent {
        evt: LiabilityTaken ⇒
          addLiability(evt.liabilityId, evt.liability)
              .increase(evt.liability.worth)
      }

  def editorsCanPayOffLiabilities = action[Fortune]
      .handleCommand {
        cmd: PayLiabilityOff ⇒ LiabilityPaidOff(
          cmd.user,
          cmd.liabilityId,
          cmd.byAmount,
          metadata(cmd),
          cmd.comment
        )
      }
      .handleEvent {
        evt: LiabilityPaidOff ⇒
          payLiabilityOff(evt.liabilityId, evt.amount)
      }

  def editorsCanIncreaseFortune = action[Fortune]
      .handleCommand {
        cmd: ReceiveIncome ⇒ FortuneIncreased(
          cmd.user,
          cmd.amount,
          cmd.currency,
          cmd.category,
          cmd.initializer,
          metadata(cmd),
          cmd.comment)
      }
      .handleEvent {
        evt: FortuneIncreased ⇒ this.increase(Worth(evt.amount, evt.currency))
      }

  def editorsCanDecreaseFortune = action[Fortune]
      .handleCommand {
        cmd: Spend ⇒ FortuneSpent(
          cmd.user,
          cmd.amount,
          cmd.currency,
          cmd.category,
          cmd.overrideDate,
          cmd.initializer,
          metadata(cmd),
          cmd.comment)
      }
      .handleEvent {
        evt: FortuneSpent ⇒ this.decrease(Worth(evt.amount, evt.currency))
      }
}

object Fortune {

  import FortuneProtocol._

  val tag = Tags.aggregateTag("fortune")

  def metadata(fortuneId: FortuneId, cmd: FortuneCommand) = {
    FortuneMetadata(fortuneId, cmd.id, tags = Set(tag))
  }

  def createFortune(fortuneId: FortuneId) =
    actions[Fortune]
        .handleCommand {
          cmd: CreateFortune ⇒ FortuneCreated(cmd.owner, metadata(fortuneId, cmd))
        }
        .handleEvent {
          evt: FortuneCreated ⇒ Fortune(fortuneId, Map.empty, Map.empty, Map.empty, evt.owner, Set.empty, Map.empty, Map.empty)
        }

  def behavior(fortuneId: FortuneId): Behavior[Fortune] = {

    case Uninitialized(id) ⇒ createFortune(id)

    case Initialized(fortune) ⇒
      fortune.unauthorizedUserCanNotAdjustFortune ++
          fortune.onlyOwnerCanAddEditors ++
          fortune.cantSendInitializationCommandsAfterInitializationIsComplete ++
          fortune.cantAcquireAssetWithNotEnoughMoney ++
          fortune.cantManipulateAssetThatDoesNotExist ++
          fortune.cantManipulateLiabilityThatDoesNotExist ++
          fortune.cantHaveNegativeBalance ++
          fortune.cantAdjustWithANegativeValue ++
          fortune.ownerCanAddEditors ++
          fortune.editorsCanFinishInitialization ++
          fortune.editorsCanUpdateLimits ++
          fortune.editorsCanBuyAssets ++
          fortune.editorsCanSellAssets ++
          fortune.editorsCanTakeOnLiabilities ++
          fortune.editorsCanPayOffLiabilities ++
          fortune.editorsCanReevaluateAssets ++
          fortune.editorsCanPerformCorrections ++
          fortune.editorsCanExchangeCurrency ++
          fortune.editorsCanIncreaseFortune ++
          fortune.editorsCanDecreaseFortune
  }
}
