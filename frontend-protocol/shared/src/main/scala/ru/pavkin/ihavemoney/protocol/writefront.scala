package ru.pavkin.ihavemoney.protocol

import io.circe._
import io.circe.generic.semiauto._
import ru.pavkin.ihavemoney.domain.fortune.Currency

object writefront extends SharedProtocol {

  sealed trait WriteFrontRequest

  // Fortune commands
  case class ReceiveIncomeRequest(amount: BigDecimal,
                                  currency: Currency,
                                  category: String,
                                  comment: Option[String] = None) extends WriteFrontRequest
  case class SpendRequest(amount: BigDecimal,
                          currency: Currency,
                          category: String,
                          comment: Option[String] = None) extends WriteFrontRequest

  // User commands

  case class CreateUserRequest(email: String, displayName: String, password: String) extends WriteFrontRequest
  case class ConfirmEmailRequest(email: String, confirmationCode: String) extends WriteFrontRequest
  case class LogInRequest(email: String, password: String) extends WriteFrontRequest
  case class ResendConfirmationEmailRequest(email: String) extends WriteFrontRequest

  // misc
  case class RequestResult(commandId: String, success: Boolean, error: Option[String] = None)


  implicit val riEncoder: Encoder[ReceiveIncomeRequest] = deriveEncoder[ReceiveIncomeRequest]
  implicit val riDecoder: Decoder[ReceiveIncomeRequest] = deriveDecoder[ReceiveIncomeRequest]

  implicit val sEncoder: Encoder[SpendRequest] = deriveEncoder[SpendRequest]
  implicit val sDecoder: Decoder[SpendRequest] = deriveDecoder[SpendRequest]

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


  implicit val resEncoder: Encoder[RequestResult] = deriveEncoder[RequestResult]
  implicit val resDecoder: Decoder[RequestResult] = deriveDecoder[RequestResult]

}
