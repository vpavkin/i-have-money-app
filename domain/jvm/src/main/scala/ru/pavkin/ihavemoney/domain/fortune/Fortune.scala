package ru.pavkin.ihavemoney.domain.fortune

import io.funcqrs._
import io.funcqrs.behavior._
import ru.pavkin.ihavemoney.domain.errors.{BalanceIsNotEnough, InsufficientAccessRights}
import ru.pavkin.ihavemoney.domain.user.UserId

case class Fortune(id: FortuneId,
                   balances: Map[Currency, BigDecimal],
                   owner: UserId,
                   editors: Set[UserId]) extends AggregateLike {

  type Id = FortuneId
  type Protocol = FortuneProtocol.type

  def canBeAdjustedBy(user: UserId): Boolean = owner == user || editors.contains(user)

  def increase(worth: Worth): Fortune =
    copy(balances = balances + (worth.currency -> (amount(worth.currency) + worth.amount)))

  def decrease(by: Worth): Fortune =
    copy(balances = balances + (by.currency -> (amount(by.currency) - by.amount)))

  def worth(currency: Currency): Worth = Worth(amount(currency), currency)
  def amount(currency: Currency): BigDecimal = balances.getOrElse(currency, BigDecimal(0.0))

  def addEditor(user: UserId): Fortune =
    copy(editors = editors + user)

  import FortuneProtocol._

  def metadata(cmd: FortuneCommand): FortuneMetadata =
    Fortune.metadata(id, cmd)

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

  def ownerCanAddEditors = action[Fortune]
    .handleCommand {
      cmd: AddEditor ⇒ EditorAdded(cmd.editor, metadata(cmd))
    }
    .handleEvent {
      evt: EditorAdded ⇒ this.addEditor(evt.editor)
    }

  def editorsCanIncreaseFortune = action[Fortune]
    .handleCommand {
      cmd: ReceiveIncome => FortuneIncreased(
        cmd.user,
        cmd.amount,
        cmd.currency,
        cmd.category,
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
        evt: FortuneCreated => Fortune(id = fortuneId, balances = Map.empty, evt.owner, Set.empty)
      }

  def behavior(fortuneId: FortuneId): Behavior[Fortune] = {

    case Uninitialized(id) => createFortune(id)

    case Initialized(fortune) =>
      fortune.unauthorizedUserCanNotAdjustFortune ++
        fortune.onlyOwnerCanAddEditors ++
        fortune.cantHaveNegativeBalance ++
        fortune.ownerCanAddEditors ++
        fortune.editorsCanIncreaseFortune ++
        fortune.editorsCanDecreaseFortune
  }
}
