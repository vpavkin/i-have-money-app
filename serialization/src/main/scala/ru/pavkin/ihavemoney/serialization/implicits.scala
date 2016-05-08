package ru.pavkin.ihavemoney.serialization

import java.time.OffsetDateTime

import com.trueaccord.scalapb.GeneratedMessageCompanion
import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.proto.commands._
import ru.pavkin.ihavemoney.proto.events.{PBFortuneIncreased, PBFortuneSpent, PBMetadata, PBUserCreated}
import ru.pavkin.utils.option._
import ProtobufSuite.syntax._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.proto.commands.PBCommandEnvelope.Command._
import shapeless.{::, HNil, Poly, Poly1}
import shapeless.poly._

// todo: generalize conversions
// todo: add suites for events

object implicits {
  def deserializeFortuneMetadata(m: PBMetadata): FortuneMetadata =
    MetadataSerialization.deserialize[FortuneMetadata, FortuneId](FortuneMetadata, FortuneId(_), OffsetDateTime.parse)(m)

  implicit val fortuneIncreasedSuite: ProtobufSuite[FortuneIncreased, PBFortuneIncreased] =
    new ProtobufSuite[FortuneIncreased, PBFortuneIncreased] {
      def encode(m: FortuneIncreased): PBFortuneIncreased = PBFortuneIncreased(
        m.user.value,
        m.amount.toString,
        m.currency.code,
        m.category.name,
        Some(MetadataSerialization.serialize(m.metadata)),
        m.comment.getOrElse(""))
      def decode(p: PBFortuneIncreased): FortuneIncreased = FortuneIncreased(
        UserId(p.userId),
        BigDecimal(p.amount),
        Currency.unsafeFromCode(p.currency),
        IncomeCategory(p.category),
        deserializeFortuneMetadata(p.metadata.get),
        notEmpty(p.comment)
      )
      def companion = PBFortuneIncreased
    }

  implicit val fortuneSpentSuite: ProtobufSuite[FortuneSpent, PBFortuneSpent] =
    new ProtobufSuite[FortuneSpent, PBFortuneSpent] {
      def encode(m: FortuneSpent): PBFortuneSpent = PBFortuneSpent(
        m.user.value,
        m.amount.toString,
        m.currency.code,
        m.category.name,
        Some(MetadataSerialization.serialize(m.metadata)),
        m.comment.getOrElse(""))
      def decode(p: PBFortuneSpent): FortuneSpent = FortuneSpent(
        UserId(p.userId),
        BigDecimal(p.amount),
        Currency.unsafeFromCode(p.currency),
        ExpenseCategory(p.category),
        deserializeFortuneMetadata(p.metadata.get),
        notEmpty(p.comment)
      )
      def companion = PBFortuneSpent
    }

  implicit val receiveIncomeSuite: ProtobufSuite[ReceiveIncome, PBReceiveIncome] =
    new ProtobufSuite[ReceiveIncome, PBReceiveIncome] {
      def encode(m: ReceiveIncome): PBReceiveIncome = PBReceiveIncome(
        m.user.value,
        m.amount.toString,
        m.currency.code,
        m.category.name,
        m.comment.getOrElse("")
      )

      def decode(p: PBReceiveIncome): ReceiveIncome = ReceiveIncome(
        UserId(p.userId),
        BigDecimal(p.amount),
        Currency.unsafeFromCode(p.currency),
        IncomeCategory(p.category),
        notEmpty(p.comment)
      )
      def companion = PBReceiveIncome
    }

  implicit val spendSuite: ProtobufSuite[Spend, PBSpend] =
    new ProtobufSuite[Spend, PBSpend] {
      def encode(m: Spend): PBSpend = PBSpend(
        m.user.value,
        m.amount.toString,
        m.currency.code,
        m.category.name,
        m.comment.getOrElse("")
      )

      def decode(p: PBSpend): Spend = Spend(
        UserId(p.userId),
        BigDecimal(p.amount),
        Currency.unsafeFromCode(p.currency),
        ExpenseCategory(p.category),
        notEmpty(p.comment)
      )
      def companion = PBSpend
    }

  implicit val createUserSuite: ProtobufSuite[CreateUser, PBCreateUser] =
    ProtobufSuite.auto[CreateUser, PBCreateUser].identicallyShaped(PBCreateUser)

  implicit val confirmEmailSuite: ProtobufSuite[ConfirmEmail, PBConfirmEmail] =
    ProtobufSuite.auto[ConfirmEmail, PBConfirmEmail].identicallyShaped(PBConfirmEmail)

  implicit val logInSuite: ProtobufSuite[LoginUser, PBLogIn] =
    ProtobufSuite.auto[LoginUser, PBLogIn].identicallyShaped(PBLogIn)

  implicit val resendConfirmationEmailSuite: ProtobufSuite[ResendConfirmationEmail.type, PBResendConfirmationEmail] =
    new ProtobufSuite[ResendConfirmationEmail.type, PBResendConfirmationEmail] {
      def encode(m: ResendConfirmationEmail.type): PBResendConfirmationEmail = PBResendConfirmationEmail()
      def decode(p: PBResendConfirmationEmail): ResendConfirmationEmail.type = ResendConfirmationEmail
      def companion: GeneratedMessageCompanion[PBResendConfirmationEmail] = PBResendConfirmationEmail
    }

  implicit val createFortuneSuite: ProtobufSuite[CreateFortune, PBCreateFortune] =
    ProtobufSuite.auto[CreateFortune, PBCreateFortune].hlist(
      (m: UserId :: HNil) ⇒ m.head.value :: HNil,
      (m: String :: HNil) ⇒ UserId(m.head) :: HNil,
      PBCreateFortune
    )

  object userIdIsoString extends Poly1 {
    implicit def caseString = at[String](UserId(_))
    implicit def caseUserId = at[UserId](_.value)
  }

  implicit val addEditorSuite: ProtobufSuite[AddEditor, PBAddEditor] =
    ProtobufSuite.auto[AddEditor, PBAddEditor].hlist(
      (m: UserId :: UserId :: HNil) ⇒ m.map(userIdIsoString),
      (m: String :: String :: HNil) ⇒ m.map(userIdIsoString),
      PBAddEditor
    )

  implicit val commandEnvelopeSuite: ProtobufSuite[CommandEnvelope, PBCommandEnvelope] =
    new ProtobufSuite[CommandEnvelope, PBCommandEnvelope] {
      def encode(m: CommandEnvelope): PBCommandEnvelope = PBCommandEnvelope(
        m.aggregateId,
        m.command match {
          case cmd: FortuneCommand ⇒ cmd match {
            case c: ReceiveIncome ⇒ Command1(c.encode)
            case c: Spend ⇒ Command2(c.encode)
            case c: CreateFortune => Command7(c.encode)
            case c: AddEditor => Command8(c.encode)
          }
          case cmd: UserCommand ⇒ cmd match {
            case c: CreateUser => Command3(c.encode)
            case c: ConfirmEmail => Command4(c.encode)
            case c: LoginUser => Command5(c.encode)
            case ResendConfirmationEmail => Command6(ResendConfirmationEmail.encode)
          }
          case other ⇒ throw new Exception(s"Unknown domain command ${other.getClass.getName}")
        }
      )
      def decode(p: PBCommandEnvelope): CommandEnvelope = CommandEnvelope(
        p.aggregateId,
        p.command match {
          case Empty => throw new Exception(s"Received empty command envelope")
          case Command1(value) => value.decode
          case Command2(value) => value.decode
          case Command3(value) => value.decode
          case Command4(value) => value.decode
          case Command5(value) => value.decode
          case Command6(value) => value.decode
          case Command7(value) => value.decode
          case Command8(value) => value.decode
        }
      )
      def companion: GeneratedMessageCompanion[PBCommandEnvelope] = PBCommandEnvelope
    }
}
