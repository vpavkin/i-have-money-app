package ru.pavkin.ihavemoney.serialization.adapters

import ru.pavkin.ihavemoney.proto.events.{PBCurrencyExchanged, _}
import ru.pavkin.ihavemoney.serialization.ProtobufSuite.syntax._
import ru.pavkin.ihavemoney.serialization.implicits._

trait FortuneProtobufAdapter {

  def deserialize(e: Any): Any = e match {
    case p: PBFortuneCreated ⇒ p.decode
    case p: PBEditorAdded ⇒ p.decode
    case p: PBFortuneInitializationFinished ⇒ p.decode
    case p: PBFortuneIncreased ⇒ p.decode
    case p: PBFortuneSpent ⇒ p.decode
    case p: PBUserCreated ⇒ p.decode
    case p: PBUserConfirmed ⇒ p.decode
    case p: PBConfirmationEmailSent ⇒ p.decode
    case p: PBUserLoggedIn ⇒ p.decode
    case p: PBUserFailedToLogIn ⇒ p.decode
    case p: PBAssetAcquired ⇒ p.decode
    case p: PBAssetSold ⇒ p.decode
    case p: PBAssetWorthChanged ⇒ p.decode
    case p: PBLiabilityTaken ⇒ p.decode
    case p: PBLiabilityPaidOff ⇒ p.decode
    case p: PBCurrencyExchanged ⇒ p.decode
    case p ⇒
      println("Received Event that is not handled by adapter")
      e
  }

}
