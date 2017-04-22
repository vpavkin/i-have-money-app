package ru.pavkin.ihavemoney.serialization

import java.time.{LocalDate, OffsetDateTime}
import java.util.UUID

import io.funcqrs.{Tag, Tags}
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.proto.common._
import ru.pavkin.ihavemoney.proto.events._
import ru.pavkin.ihavemoney.proto.snapshots.PBFortune
import ru.pavkin.ihavemoney.serialization.derivation.Protocol
import ru.pavkin.ihavemoney.serialization.derivation.Protocol.syntax._
import shapeless.{::, Generic, HNil}
import cats.instances.either._
import cats.instances.list._

import scala.language.higherKinds
import cats.syntax.either._

object formats {

  implicit val uuidProtocol: Protocol.Try[UUID, String] = Protocol.Try.catchException(_.toString, UUID.fromString)

  /* Provides protocol for all case classes that have one UUID field (id wrappers) */
  implicit def uuidBasedIdProtocol[M](implicit G: Generic.Aux[M, UUID :: HNil]): Protocol.Try[M, String] =
    uuidProtocol.inmapM(uuid => G.from(uuid :: HNil), t => G.to(t).head)

  /* Provides protocol for all case classes that have one String field (id wrappers) */
  implicit def stringBasedIdProtocol[M](implicit G: Generic.Aux[M, String :: HNil]): Protocol.Try[M, String] =
    Protocol.Try.catchException(t => G.to(t).head, str => G.from(str :: HNil))

  implicit val tagSetProtocol: Protocol.Try[Set[Tag], Seq[String]] = Protocol.Try.catchException(
    _.map(_.value).toSeq,
    _.toSet.map(Tags.aggregateTag)
  )

  /** Wraps protobuf semantics of storing non existing strings as empty strings
    *
    * @example ```Option[LocalDate] <=> String```
    */
  implicit def optionToString[M](
    implicit P: Protocol.Try[M, String]): Protocol.Try[Option[M], String] = Protocol.Try.instance(
    _.map(_.serialize[String]).getOrElse(""),
    str => if (str == "") Right(None) else str.tryDeserialize[M].map(Some(_))
  )

  /** Wraps protobuf semantics of storing any non plain value as optional
    *
    * @example ```ShiftDefinition <=> Option[pb.ShiftDefinition]```
    */
  implicit def anyToOption[M, R](
    implicit P: Protocol.Try[M, R]): Protocol.Try[M, Option[R]] = Protocol.Try.instance(
    m => Option(m.serialize[R]),
    proto => Either.fromOption(proto, new Exception("Required field value was None"))
      .flatMap(_.tryDeserialize[M])
  )

  /* Protocol ```List[M] <=> Seq[R]``` (repeatable fields are represented as Seqs) */
  implicit def repeatable[M, R](
    implicit P: Protocol.Try[M, R]): Protocol.Try[List[M], scala.collection.Seq[R]] =
    Protocol.traverseProtocolInstance[Protocol.Try.Effect, M, R, List].inmapR[Seq[R]](_.toSeq, _.toList)

  implicit val localDateProtocol: Protocol.Try[LocalDate, String] =
    Protocol.Try.catchException(_.toString, LocalDate.parse)

  implicit val offsetDateTimeProtocol: Protocol.Try[OffsetDateTime, String] =
    Protocol.Try.catchException(_.toString, OffsetDateTime.parse)

  implicit val bigDecimalProtocol: Protocol.Try[BigDecimal, String] =
    Protocol.Try.catchException(_.toString, BigDecimal(_))

  implicit val currencyProtocol: Protocol.Try[Currency, String] =
    Protocol.Try.catchException(_.code, Currency.withName)

  /* Common */

  implicit val assetProtocol: Protocol.Try[Asset, PBAsset] = new Protocol.Try[Asset, PBAsset] {

    import PBAsset.Asset._

    def serialize(t: Asset): PBAsset = t match {
      case s: CountedAsset ⇒ PBAsset(Asset1(s.serialize[PBCountedAsset]))
    }
    def deserialize(t: PBAsset): Protocol.Try.Effect[Asset] = t.asset match {
      case Empty ⇒ throw new Exception(s"Received empty asset")
      case Asset1(value) ⇒ value.tryDeserialize[CountedAsset]
    }
  }

  implicit val liabilityProtocol: Protocol.Try[Liability, PBLiability] = new Protocol.Try[Liability, PBLiability] {

    import PBLiability.Liability._

    def serialize(t: Liability): PBLiability = t match {
      case n: NoInterestDebt ⇒ PBLiability(Liability1(n.serialize[PBNoInterestDebt]))
      case n: Loan ⇒ PBLiability(Liability2(n.serialize[PBLoan]))
    }

    def deserialize(t: PBLiability): Protocol.Try.Effect[Liability] = t.liability match {
      case Empty ⇒ throw new Exception(s"Received empty liability")
      case Liability1(value) ⇒ value.tryDeserialize[NoInterestDebt]
      case Liability2(value) ⇒ value.tryDeserialize[Loan]
    }
  }

  implicit val metadataProtocol = Protocol.Try[FortuneMetadata, PBMetadata]

  /* Events */

  implicit val currencyExchangedProtocol = ProtobufFormat.fromProtocol[CurrencyExchanged, PBCurrencyExchanged]
  implicit val assetAcquiredProtocol = ProtobufFormat.fromProtocol[AssetAcquired, PBAssetAcquired]
  implicit val assetSoldProtocol = ProtobufFormat.fromProtocol[AssetSold, PBAssetSold]
  implicit val assetWorthChangedProtocol = ProtobufFormat.fromProtocol[AssetPriceChanged, PBAssetPriceChanged]
  implicit val liabilityTakenProtocol = ProtobufFormat.fromProtocol[LiabilityTaken, PBLiabilityTaken]
  implicit val liabilityPaidOffProtocol = ProtobufFormat.fromProtocol[LiabilityPaidOff, PBLiabilityPaidOff]
  implicit val fortuneInitializationFinishedProtocol = ProtobufFormat.fromProtocol[FortuneInitializationFinished, PBFortuneInitializationFinished]
  implicit val fortuneIncreasedProtocol = ProtobufFormat.fromProtocol[FortuneIncreased, PBFortuneIncreased]
  implicit val fortuneSpentProtocol = ProtobufFormat.fromProtocol[FortuneSpent, PBFortuneSpent]
  implicit val fortuneCreatedProtocol = ProtobufFormat.fromProtocol[FortuneCreated, PBFortuneCreated]
  implicit val userCreatedProtocol = ProtobufFormat.fromProtocol[UserCreated, PBUserCreated]
  implicit val editorAddedProtocol = ProtobufFormat.fromProtocol[EditorAdded, PBEditorAdded]
  implicit val userConfirmedProtocol = ProtobufFormat.fromProtocol[UserConfirmed, PBUserConfirmed]
  implicit val confirmationEmailSentProtocol = ProtobufFormat.fromProtocol[ConfirmationEmailSent, PBConfirmationEmailSent]
  implicit val userLoggedInProtocol = ProtobufFormat.fromProtocol[UserLoggedIn, PBUserLoggedIn]
  implicit val userFailedToLogInProtocol = ProtobufFormat.fromProtocol[UserFailedToLogIn, PBUserFailedToLogIn]
  implicit val limitsUpdatedProtocol = ProtobufFormat.fromProtocol[LimitsUpdated, PBLimitsUpdated]
  implicit val transactionCancelledProtocol = ProtobufFormat.fromProtocol[TransactionCancelled, PBTransactionCancelled]

  /* Snapshots */

  implicit val ignoreLast30DaysTransactions: Protocol.Try[Map[UUID, FortuneEvent], Boolean] =
    Protocol.Try.catchException(
      _ => false,
      _ => Map.empty
    )

  implicit val fortuneProtocol = ProtobufFormat.fromProtocol[Fortune, PBFortune]
}
