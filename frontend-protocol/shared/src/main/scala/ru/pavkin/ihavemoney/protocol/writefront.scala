package ru.pavkin.ihavemoney.protocol

import java.util.UUID

import io.circe._
import io.circe.generic.semiauto._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, Liability}

object writefront extends SharedProtocol {

  sealed trait WriteFrontRequest

  // Fortune commands
  case class ReceiveIncomeRequest(amount: BigDecimal,
                                  currency: Currency,
                                  category: String,
                                  initializer: Boolean = false,
                                  comment: Option[String] = None) extends WriteFrontRequest
  case class SpendRequest(amount: BigDecimal,
                          currency: Currency,
                          category: String,
                          initializer: Boolean = false,
                          comment: Option[String] = None) extends WriteFrontRequest

  case class BuyAssetRequest(asset: Asset,
                             initializer: Boolean = false,
                             comment: Option[String] = None) extends WriteFrontRequest

  case class SellAssetRequest(comment: Option[String] = None) extends WriteFrontRequest

  /* Reevaluate per-stock worth for stocks, whole asset worth otherwise*/
  case class ReevaluateAssetRequest(newPrice: BigDecimal,
                                    comment: Option[String] = None) extends WriteFrontRequest

  case class TakeOnLiabilityRequest(liability: Liability,
                                    initializer: Boolean = false,
                                    comment: Option[String] = None) extends WriteFrontRequest

  case class PayLiabilityOffRequest(byAmount: BigDecimal,
                                    comment: Option[String] = None) extends WriteFrontRequest

  // User commands

  case class CreateUserRequest(email: String, displayName: String, password: String) extends WriteFrontRequest
  case class ConfirmEmailRequest(email: String, confirmationCode: String) extends WriteFrontRequest
  case class LogInRequest(email: String, password: String) extends WriteFrontRequest
  case class ResendConfirmationEmailRequest(email: String) extends WriteFrontRequest

  // misc
  case class RequestResult[T](commandId: String, success: Boolean, result: Option[T], error: Option[String] = None)

  object RequestResult {
    def success[T](commandId: String, result: T) = RequestResult(commandId, success = true, Some(result))
    def justSuccess(commandId: String) = RequestResult[String](commandId, success = true, None)
    def failure(commandId: String, error: String) = RequestResult[String](commandId, success = false, None, Some(error))
  }

  implicit val riEncoder: Encoder[ReceiveIncomeRequest] = deriveEncoder[ReceiveIncomeRequest]
  implicit val riDecoder: Decoder[ReceiveIncomeRequest] = deriveDecoder[ReceiveIncomeRequest]

  implicit val sEncoder: Encoder[SpendRequest] = deriveEncoder[SpendRequest]
  implicit val sDecoder: Decoder[SpendRequest] = deriveDecoder[SpendRequest]

  implicit val buyAssetRequestEncoder: Encoder[BuyAssetRequest] = deriveEncoder[BuyAssetRequest]
  implicit val buyAssetRequestDecoder: Decoder[BuyAssetRequest] = deriveDecoder[BuyAssetRequest]

  implicit val sellAssetRequestEncoder: Encoder[SellAssetRequest] = deriveEncoder[SellAssetRequest]
  implicit val sellAssetRequestDecoder: Decoder[SellAssetRequest] = deriveDecoder[SellAssetRequest]

  implicit val reevaluateAssetRequestEncoder: Encoder[ReevaluateAssetRequest] = deriveEncoder[ReevaluateAssetRequest]
  implicit val reevaluateAssetRequestDecoder: Decoder[ReevaluateAssetRequest] = deriveDecoder[ReevaluateAssetRequest]

  implicit val takeOnLiabilityRequestEncoder: Encoder[TakeOnLiabilityRequest] = deriveEncoder[TakeOnLiabilityRequest]
  implicit val takeOnLiabilityRequestDecoder: Decoder[TakeOnLiabilityRequest] = deriveDecoder[TakeOnLiabilityRequest]

  implicit val payLiabilityOffRequestEncoder: Encoder[PayLiabilityOffRequest] = deriveEncoder[PayLiabilityOffRequest]
  implicit val payLiabilityOffRequestDecoder: Decoder[PayLiabilityOffRequest] = deriveDecoder[PayLiabilityOffRequest]

  implicit val createUserEncoder: Encoder[CreateUserRequest] = deriveEncoder[CreateUserRequest]
  implicit val createUserDecoder: Decoder[CreateUserRequest] = deriveDecoder[CreateUserRequest]

  implicit val confirmEmailReqEncoder: Encoder[ConfirmEmailRequest] = deriveEncoder[ConfirmEmailRequest]
  implicit val confirmEmailReqDecoder: Decoder[ConfirmEmailRequest] = deriveDecoder[ConfirmEmailRequest]

  implicit val logInEncoder: Encoder[LogInRequest] = deriveEncoder[LogInRequest]
  implicit val logInDecoder: Decoder[LogInRequest] = deriveDecoder[LogInRequest]

  implicit val resendEmailEncoder: Encoder[ResendConfirmationEmailRequest] = deriveEncoder[ResendConfirmationEmailRequest]
  implicit val resendEmailDecoder: Decoder[ResendConfirmationEmailRequest] = deriveDecoder[ResendConfirmationEmailRequest]

  implicit val reqEncoder: Encoder[WriteFrontRequest] = deriveEncoder[WriteFrontRequest]
  implicit val reqDecoder: Decoder[WriteFrontRequest] = deriveDecoder[WriteFrontRequest]


  implicit def resEncoder[T: Encoder]: Encoder[RequestResult[T]] = deriveEncoder[RequestResult[T]]
  implicit def resDecoder[T: Decoder]: Decoder[RequestResult[T]] = deriveDecoder[RequestResult[T]]

}
