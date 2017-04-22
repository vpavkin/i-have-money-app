package ru.pavkin.ihavemoney.serialization.adapters

import ru.pavkin.ihavemoney.proto.events._
import ru.pavkin.ihavemoney.serialization.ProtobufReader
import ru.pavkin.ihavemoney.serialization.formats._

trait FortuneProtobufAdapter extends ProtobufReader {

  def deserialize(e: Any): Any = e match {
    case p: PBFortuneCreated ⇒ decodeOrThrow(p)
    case p: PBEditorAdded ⇒ decodeOrThrow(p)
    case p: PBFortuneInitializationFinished ⇒ decodeOrThrow(p)
    case p: PBFortuneIncreased ⇒ decodeOrThrow(p)
    case p: PBFortuneSpent ⇒ decodeOrThrow(p)
    case p: PBUserCreated ⇒ decodeOrThrow(p)
    case p: PBUserConfirmed ⇒ decodeOrThrow(p)
    case p: PBConfirmationEmailSent ⇒ decodeOrThrow(p)
    case p: PBUserLoggedIn ⇒ decodeOrThrow(p)
    case p: PBUserFailedToLogIn ⇒ decodeOrThrow(p)
    case p: PBAssetAcquired ⇒ decodeOrThrow(p)
    case p: PBAssetSold ⇒ decodeOrThrow(p)
    case p: PBAssetPriceChanged ⇒ decodeOrThrow(p)
    case p: PBLiabilityTaken ⇒ decodeOrThrow(p)
    case p: PBLiabilityPaidOff ⇒ decodeOrThrow(p)
    case p: PBCurrencyExchanged ⇒ decodeOrThrow(p)
    case p: PBLimitsUpdated ⇒ decodeOrThrow(p)
    case p: PBTransactionCancelled ⇒ decodeOrThrow(p)
    case p ⇒
      println("Received Event that is not handled by adapter")
      e
  }

}
