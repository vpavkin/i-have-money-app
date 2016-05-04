package ru.pavkin.ihavemoney.domain.user

import java.time.OffsetDateTime

import io.funcqrs._

case class UserId(email: String) extends AggregateId {
  val value: String = email
}

object UserProtocol extends ProtocolLike {

  /*-------------------Commands---------------------*/
  sealed trait UserCommand extends ProtocolCommand

  case class CreateUser(password: String, displayName: String) extends UserCommand
  case class ConfirmEmail(confirmationCode: String) extends UserCommand
  case class LoginUser(password: String) extends UserCommand

  case object ResendConfirmationEmail extends UserCommand

  /*-------------------Events---------------------*/
  sealed trait UserEvent extends ProtocolEvent with MetadataFacet[UserMetadata]

  case class UserCreated(passwordHash: String,
                         displayName: String,
                         confirmationCode: String,
                         metadata: UserMetadata) extends UserEvent

  case class UserConfirmed(metadata: UserMetadata) extends UserEvent
  case class ConfirmationEmailSent(metadata: UserMetadata) extends UserEvent
  case class UserLoggedIn(metadata: UserMetadata) extends UserEvent
  case class UserFailedToLogIn(invalidPassword: String, metadata: UserMetadata) extends UserEvent

  /*-------------------Metadata---------------------*/
  case class UserMetadata(aggregateId: UserId,
                          commandId: CommandId,
                          eventId: EventId = EventId(),
                          date: OffsetDateTime = OffsetDateTime.now(),
                          tags: Set[Tag] = Set()) extends Metadata with JavaTime {
    type Id = UserId
  }
}
