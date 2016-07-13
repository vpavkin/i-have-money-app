package ru.pavkin.ihavemoney.serialization

import java.time.{LocalDate, OffsetDateTime}
import java.util.UUID

import com.trueaccord.scalapb.GeneratedMessageCompanion
import io.funcqrs.{Tag, Tags}
import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.proto.commands.PBCommandEnvelope.Command._
import ru.pavkin.ihavemoney.proto.commands._
import ru.pavkin.ihavemoney.proto.common._
import ru.pavkin.ihavemoney.proto.events._
import ru.pavkin.ihavemoney.serialization.ProtobufSuite.syntax._
import ru.pavkin.ihavemoney.serialization.derivation.IsoSerializable
import ru.pavkin.ihavemoney.serialization.derivation.IsoSerializable.syntax._
import ru.pavkin.utils.option._
import shapeless.{::, Generic, HNil}

object implicits {

  implicit def stringIdIS[Id](implicit G: Generic.Aux[Id, String :: HNil]): IsoSerializable[Id, String] = new IsoSerializable[Id, String] {
    def serialize(t: Id): String = G.to(t).head
    def deserialize(t: String): Id = G.from(t :: HNil)
  }

  implicit def uuidIdIS[Id](implicit G: Generic.Aux[Id, UUID :: HNil]): IsoSerializable[Id, String] = new IsoSerializable[Id, String] {
    def serialize(t: Id): String = G.to(t).head.toString
    def deserialize(t: String): Id = G.from(UUID.fromString(t) :: HNil)
  }

  implicit val tagSetIS: IsoSerializable[Set[Tag], Seq[String]] = new IsoSerializable[Set[Tag], Seq[String]] {
    def serialize(t: Set[Tag]): Seq[String] = t.map(_.value).toSeq
    def deserialize(t: Seq[String]): Set[Tag] = t.toSet.map(Tags.aggregateTag)
  }

  implicit val localDateIS: IsoSerializable[LocalDate, String] =
    IsoSerializable.withString(_.toString, LocalDate.parse)

  implicit val offsetDateTimeIS: IsoSerializable[OffsetDateTime, String] =
    IsoSerializable.withString(_.toString, OffsetDateTime.parse)

  implicit def optionToString[S](implicit IS: IsoSerializable[S, String]): IsoSerializable[Option[S], String] =
    new IsoSerializable[Option[S], String] {
      def serialize(t: Option[S]): String = t.map(IS.serialize).getOrElse("")
      def deserialize(t: String): Option[S] = if (t == "") None else Some(IS.deserialize(t))
    }

  implicit def unsafeOptionIS[S, R](implicit IS: IsoSerializable[S, R]): IsoSerializable[S, Option[R]] =
    new IsoSerializable[S, Option[R]] {
      def serialize(t: S): Option[R] = Some(IS.serialize(t))
      def deserialize(t: Option[R]): S = IS.deserialize(t.get)
    }

  implicit val optionStringIS: IsoSerializable[Option[String], String] =
    IsoSerializable.withString(_.getOrElse(""), notEmpty)

  implicit val bigDecimalIS: IsoSerializable[BigDecimal, String] =
    IsoSerializable.withString(_.toString, BigDecimal(_))

  implicit val currencyIS: IsoSerializable[Currency, String] =
    IsoSerializable.withString(_.code, Currency.unsafeFromCode)

  /* Common */

  implicit val assetIS: IsoSerializable[Asset, PBAsset] = new IsoSerializable[Asset, PBAsset] {

    import PBAsset.Asset._

    def serialize(t: Asset): PBAsset = t match {
      case s: CountedAsset ⇒ PBAsset(Asset1(s.serialize[PBCountedAsset]))
    }
    def deserialize(t: PBAsset): Asset = t.asset match {
      case Empty ⇒ throw new Exception(s"Received empty asset")
      case Asset1(value) ⇒ value.deserialize[CountedAsset]
    }
  }

  implicit val liabilityIS: IsoSerializable[Liability, PBLiability] = new IsoSerializable[Liability, PBLiability] {

    import PBLiability.Liability._

    def serialize(t: Liability): PBLiability = t match {
      case n: NoInterestDebt ⇒ PBLiability(Liability1(n.serialize[PBNoInterestDebt]))
      case n: Loan ⇒ PBLiability(Liability2(n.serialize[PBLoan]))
    }

    def deserialize(t: PBLiability): Liability = t.liability match {
      case Empty ⇒ throw new Exception(s"Received empty liability")
      case Liability1(value) ⇒ value.deserialize[NoInterestDebt]
      case Liability2(value) ⇒ value.deserialize[Loan]
    }
  }

  /* Events */
  implicit val currencyExchangedSuite: ProtobufSuite[CurrencyExchanged, PBCurrencyExchanged] = ProtobufSuite.iso[CurrencyExchanged, PBCurrencyExchanged]
  implicit val assetAcquiredSuite: ProtobufSuite[AssetAcquired, PBAssetAcquired] = ProtobufSuite.iso[AssetAcquired, PBAssetAcquired]
  implicit val assetSoldSuite: ProtobufSuite[AssetSold, PBAssetSold] = ProtobufSuite.iso[AssetSold, PBAssetSold]
  implicit val assetWorthChangedSuite: ProtobufSuite[AssetPriceChanged, PBAssetPriceChanged] = ProtobufSuite.iso[AssetPriceChanged, PBAssetPriceChanged]
  implicit val liabilityTakenSuite: ProtobufSuite[LiabilityTaken, PBLiabilityTaken] = ProtobufSuite.iso[LiabilityTaken, PBLiabilityTaken]
  implicit val liabilityPaidOffSuite: ProtobufSuite[LiabilityPaidOff, PBLiabilityPaidOff] = ProtobufSuite.iso[LiabilityPaidOff, PBLiabilityPaidOff]
  implicit val fortuneInitializationFinishedSuite: ProtobufSuite[FortuneInitializationFinished, PBFortuneInitializationFinished] = ProtobufSuite.iso[FortuneInitializationFinished, PBFortuneInitializationFinished]
  implicit val fortuneIncreasedSuite: ProtobufSuite[FortuneIncreased, PBFortuneIncreased] = ProtobufSuite.iso[FortuneIncreased, PBFortuneIncreased]
  implicit val fortuneSpentSuite: ProtobufSuite[FortuneSpent, PBFortuneSpent] = ProtobufSuite.iso[FortuneSpent, PBFortuneSpent]
  implicit val fortuneCreatedSuite: ProtobufSuite[FortuneCreated, PBFortuneCreated] = ProtobufSuite.iso[FortuneCreated, PBFortuneCreated]
  implicit val userCreatedSuite: ProtobufSuite[UserCreated, PBUserCreated] = ProtobufSuite.iso[UserCreated, PBUserCreated]
  implicit val editorAddedSuite: ProtobufSuite[EditorAdded, PBEditorAdded] = ProtobufSuite.iso[EditorAdded, PBEditorAdded]
  implicit val userConfirmedSuite: ProtobufSuite[UserConfirmed, PBUserConfirmed] = ProtobufSuite.iso[UserConfirmed, PBUserConfirmed]
  implicit val confirmationEmailSentSuite: ProtobufSuite[ConfirmationEmailSent, PBConfirmationEmailSent] = ProtobufSuite.iso[ConfirmationEmailSent, PBConfirmationEmailSent]
  implicit val userLoggedInSuite: ProtobufSuite[UserLoggedIn, PBUserLoggedIn] = ProtobufSuite.iso[UserLoggedIn, PBUserLoggedIn]
  implicit val userFailedToLogInSuite: ProtobufSuite[UserFailedToLogIn, PBUserFailedToLogIn] = ProtobufSuite.iso[UserFailedToLogIn, PBUserFailedToLogIn]
  implicit val limitsUpdatedSuite: ProtobufSuite[LimitsUpdated, PBLimitsUpdated] = ProtobufSuite.iso[LimitsUpdated, PBLimitsUpdated]

  /* Commands */
  implicit val exchangeCurrencySuite: ProtobufSuite[ExchangeCurrency, PBExchangeCurrency] = ProtobufSuite.iso[ExchangeCurrency, PBExchangeCurrency]
  implicit val correctBalancesSuite: ProtobufSuite[CorrectBalances, PBCorrectBalances] = ProtobufSuite.iso[CorrectBalances, PBCorrectBalances]
  implicit val buyAssetSuite: ProtobufSuite[BuyAsset, PBBuyAsset] = ProtobufSuite.iso[BuyAsset, PBBuyAsset]
  implicit val sellAssetSuite: ProtobufSuite[SellAsset, PBSellAsset] = ProtobufSuite.iso[SellAsset, PBSellAsset]
  implicit val reevaluateAssetSuite: ProtobufSuite[ReevaluateAsset, PBReevaluateAsset] = ProtobufSuite.iso[ReevaluateAsset, PBReevaluateAsset]
  implicit val takeOnLiabilitySuite: ProtobufSuite[TakeOnLiability, PBTakeOnLiability] = ProtobufSuite.iso[TakeOnLiability, PBTakeOnLiability]
  implicit val payLiabilityOffSuite: ProtobufSuite[PayLiabilityOff, PBPayLiabilityOff] = ProtobufSuite.iso[PayLiabilityOff, PBPayLiabilityOff]
  implicit val finishInitializationSuite: ProtobufSuite[FinishInitialization, PBFinishInitialization] = ProtobufSuite.iso[FinishInitialization, PBFinishInitialization]
  implicit val receiveIncomeSuite: ProtobufSuite[ReceiveIncome, PBReceiveIncome] = ProtobufSuite.iso[ReceiveIncome, PBReceiveIncome]
  implicit val spendSuite: ProtobufSuite[Spend, PBSpend] = ProtobufSuite.iso[Spend, PBSpend]
  implicit val createUserSuite: ProtobufSuite[CreateUser, PBCreateUser] = ProtobufSuite.iso[CreateUser, PBCreateUser]
  implicit val confirmEmailSuite: ProtobufSuite[ConfirmEmail, PBConfirmEmail] = ProtobufSuite.iso[ConfirmEmail, PBConfirmEmail]
  implicit val logInSuite: ProtobufSuite[LoginUser, PBLogIn] = ProtobufSuite.iso[LoginUser, PBLogIn]
  implicit val resendConfirmationEmailSuite: ProtobufSuite[ResendConfirmationEmail, PBResendConfirmationEmail] = ProtobufSuite.iso[ResendConfirmationEmail, PBResendConfirmationEmail]
  implicit val createFortuneSuite: ProtobufSuite[CreateFortune, PBCreateFortune] = ProtobufSuite.iso[CreateFortune, PBCreateFortune]
  implicit val addEditorSuite: ProtobufSuite[AddEditor, PBAddEditor] = ProtobufSuite.iso[AddEditor, PBAddEditor]
  implicit val updateLimitsSuite: ProtobufSuite[UpdateLimits, PBUpdateLimits] = ProtobufSuite.iso[UpdateLimits, PBUpdateLimits]

  implicit val commandEnvelopeSuite: ProtobufSuite[CommandEnvelope, PBCommandEnvelope] =
    new ProtobufSuite[CommandEnvelope, PBCommandEnvelope] {
      def encode(m: CommandEnvelope): PBCommandEnvelope = PBCommandEnvelope(
        m.aggregateId,
        m.command match {
          case cmd: FortuneCommand ⇒ cmd match {
            case c: ReceiveIncome ⇒ Command1(c.encode)
            case c: Spend ⇒ Command2(c.encode)
            case c: CreateFortune ⇒ Command7(c.encode)
            case c: AddEditor ⇒ Command8(c.encode)
            case c: FinishInitialization ⇒ Command9(c.encode)
            case c: BuyAsset ⇒ Command10(c.encode)
            case c: SellAsset ⇒ Command11(c.encode)
            case c: ReevaluateAsset ⇒ Command12(c.encode)
            case c: TakeOnLiability ⇒ Command13(c.encode)
            case c: PayLiabilityOff ⇒ Command14(c.encode)
            case c: ExchangeCurrency ⇒ Command15(c.encode)
            case c: CorrectBalances ⇒ Command16(c.encode)
            case c: UpdateLimits ⇒ Command17(c.encode)
          }
          case cmd: UserCommand ⇒ cmd match {
            case c: CreateUser ⇒ Command3(c.encode)
            case c: ConfirmEmail ⇒ Command4(c.encode)
            case c: LoginUser ⇒ Command5(c.encode)
            case c: ResendConfirmationEmail ⇒ Command6(c.encode)
          }
          case other ⇒ throw new Exception(s"Unknown domain command ${other.getClass.getName}")
        }
      )
      def decode(p: PBCommandEnvelope): CommandEnvelope = CommandEnvelope(
        p.aggregateId,
        p.command match {
          case Empty ⇒ throw new Exception(s"Received empty command envelope")
          case Command1(value) ⇒ value.decode
          case Command2(value) ⇒ value.decode
          case Command3(value) ⇒ value.decode
          case Command4(value) ⇒ value.decode
          case Command5(value) ⇒ value.decode
          case Command6(value) ⇒ value.decode
          case Command7(value) ⇒ value.decode
          case Command8(value) ⇒ value.decode
          case Command9(value) ⇒ value.decode
          case Command10(value) ⇒ value.decode
          case Command11(value) ⇒ value.decode
          case Command12(value) ⇒ value.decode
          case Command13(value) ⇒ value.decode
          case Command14(value) ⇒ value.decode
          case Command15(value) ⇒ value.decode
          case Command16(value) ⇒ value.decode
          case Command17(value) ⇒ value.decode
        }
      )
      def companion: GeneratedMessageCompanion[PBCommandEnvelope] = PBCommandEnvelope
    }
}
