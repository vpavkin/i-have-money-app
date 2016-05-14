package ru.pavkin.ihavemoney.domain.fortune

import io.funcqrs._
import io.funcqrs.behavior._
import ru.pavkin.ihavemoney.domain.errors._
import ru.pavkin.ihavemoney.domain.user.UserId

case class Fortune(id: FortuneId,
                   balances: Map[Currency, BigDecimal],
                   assets: Map[AssetId, Asset],
                   liabilities: Map[LiabilityId, Liability],
                   owner: UserId,
                   editors: Set[UserId],
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

  def changeAssetWorth(id: AssetId, newPrice: BigDecimal): Fortune =
    copy(assets = assets.updated(id,
      assets(id) match {
        case s: Stocks =>
          s.copy(stockPrice = newPrice)
        case r: RealEstate =>
          r.copy(price = newPrice)
      }
    ))

  def addLiability(id: LiabilityId, liability: Liability): Fortune =
    copy(liabilities = liabilities + (id → liability))

  def payLiabilityOff(id: LiabilityId, byAmount: BigDecimal): Fortune = {
    val liab = liabilities(id)
    if (liab.amount <= byAmount)
      copy(liabilities = liabilities - id)
    else
      copy(liabilities = liabilities.updated(
        id, liab.payOff(byAmount)
      ))
  }

  def decrease(by: Worth): Fortune =
    copy(balances = balances + (by.currency -> (amount(by.currency) - by.amount)))

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
      case cmd: Spend if this.amount(cmd.currency) < cmd.amount =>
        BalanceIsNotEnough(this.amount(cmd.currency), cmd.currency)
    }

  def cantAcquireAssetWithNotEnoughMoney = action[Fortune]
    .rejectCommand {
      case cmd: BuyAsset if !cmd.initializer && this.amount(cmd.asset.currency) < cmd.asset.price ⇒
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

  def editorsCanBuyAssets = action[Fortune]
    .handleCommand.manyEvents[BuyAsset, FortuneEvent] {
    cmd: BuyAsset =>
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
          cmd.asset.price,
          cmd.asset.currency,
          IncomeCategory("auto_generated_income"),
          initializer = true,
          metadata(cmd)),
        assetAcquired
      )
      else List(assetAcquired)
  }
    .handleEvent {
      evt: FortuneIncreased => this.increase(Worth(evt.amount, evt.currency))
    }
    .handleEvent {
      evt: AssetAcquired =>
        addAsset(evt.assetId, evt.asset)
          .decrease(evt.asset.worth)
    }

  def editorsCanSellAssets = action[Fortune]
    .handleCommand {
      cmd: SellAsset =>
        AssetSold(cmd.user, cmd.assetId, metadata(cmd), cmd.comment)
    }
    .handleEvent {
      evt: AssetSold =>
        val asset = assets(evt.assetId)
        removeAsset(evt.assetId)
          .increase(asset.worth)
    }

  def editorsCanReevaluateAssets = action[Fortune]
    .handleCommand {
      cmd: ReevaluateAsset =>
        AssetWorthChanged(cmd.user, cmd.assetId, cmd.newPrice, metadata(cmd), cmd.comment)
    }
    .handleEvent {
      evt: AssetWorthChanged =>
        changeAssetWorth(evt.assetId, evt.newAmount)
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
      evt: LiabilityTaken ⇒ addLiability(evt.liabilityId, evt.liability)
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
      evt: LiabilityPaidOff ⇒ payLiabilityOff(evt.liabilityId, evt.amount)
    }

  def editorsCanIncreaseFortune = action[Fortune]
    .handleCommand {
      cmd: ReceiveIncome => FortuneIncreased(
        cmd.user,
        cmd.amount,
        cmd.currency,
        cmd.category,
        cmd.initializer,
        metadata(cmd),
        cmd.comment)
    }
    .handleEvent {
      evt: FortuneIncreased => this.increase(Worth(evt.amount, evt.currency))
    }

  def editorsCanDecreaseFortune = action[Fortune]
    .handleCommand {
      cmd: Spend => FortuneSpent(
        cmd.user,
        cmd.amount,
        cmd.currency,
        cmd.category,
        cmd.initializer,
        metadata(cmd),
        cmd.comment)
    }
    .handleEvent {
      evt: FortuneSpent => this.decrease(Worth(evt.amount, evt.currency))
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
        cmd: CreateFortune => FortuneCreated(cmd.owner, metadata(fortuneId, cmd))
      }
      .handleEvent {
        evt: FortuneCreated => Fortune(fortuneId, Map.empty, Map.empty, Map.empty, evt.owner, Set.empty)
      }

  def behavior(fortuneId: FortuneId): Behavior[Fortune] = {

    case Uninitialized(id) => createFortune(id)

    case Initialized(fortune) =>
      fortune.unauthorizedUserCanNotAdjustFortune ++
        fortune.onlyOwnerCanAddEditors ++
        fortune.cantSendInitializationCommandsAfterInitializationIsComplete ++
        fortune.cantAcquireAssetWithNotEnoughMoney ++
        fortune.cantManipulateAssetThatDoesNotExist ++
        fortune.cantManipulateLiabilityThatDoesNotExist ++
        fortune.cantHaveNegativeBalance ++
        fortune.ownerCanAddEditors ++
        fortune.editorsCanFinishInitialization ++
        fortune.editorsCanBuyAssets ++
        fortune.editorsCanSellAssets ++
        fortune.editorsCanTakeOnLiabilities ++
        fortune.editorsCanPayOffLiabilities ++
        fortune.editorsCanReevaluateAssets ++
        fortune.editorsCanIncreaseFortune ++
        fortune.editorsCanDecreaseFortune
  }
}
