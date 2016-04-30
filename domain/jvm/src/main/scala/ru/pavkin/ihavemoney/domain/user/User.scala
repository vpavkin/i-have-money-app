package ru.pavkin.ihavemoney.domain.user

import java.util.UUID

import io.funcqrs._
import io.funcqrs.behavior._
import ru.pavkin.ihavemoney.domain.errors.{EmailAlreadyConfirmed, InvalidConfirmationCode}
import ru.pavkin.ihavemoney.domain.passwords

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class User(id: UserId,
                passwordHash: String,
                displayName: String,
                confirmationCode: String,
                isConfirmed: Boolean) extends AggregateLike {

  type Id = UserId
  type Protocol = UserProtocol.type

  import UserProtocol._

  lazy val email: String = id.email

  def metadata(cmd: UserCommand): UserMetadata =
    User.metadata(id, cmd)

  def cantConfirmWithInvalidCode = action[User]
    .rejectCommand {
      case cmd: ConfirmEmail if this.confirmationCode != cmd.confirmationCode =>
        InvalidConfirmationCode
    }


  def cantSentConfirmationEmailForConfirmedUser = action[User]
    .rejectCommand {
      case ResendConfirmationEmail if this.isConfirmed =>
        EmailAlreadyConfirmed
      case cmd: ConfirmEmail if this.isConfirmed ⇒
        EmailAlreadyConfirmed
    }

  def confirmEmail = action[User]
    .handleCommand {
      cmd: ConfirmEmail ⇒ UserConfirmed(metadata(cmd))
    }
    .handleEvent {
      evt: UserConfirmed ⇒ this.copy(isConfirmed = true)
    }

  def resendConfirmationEmail = action[User]
    .handleCommandAsync[ResendConfirmationEmail.type, ConfirmationEmailSent] {
    cmd ⇒ User.sendConfirmationEmail(metadata(cmd), this.confirmationCode)
  }
    .handleEvent {
      evt: ConfirmationEmailSent ⇒ this
    }

  def login = action[User]
    .handleCommand {
      cmd: LoginUser ⇒ if (passwords.isCorrect(cmd.password, this.passwordHash))
      //todo: generate token
        UserLoggedIn("TOKEN", metadata(cmd))
      else
        UserFailedToLogIn(cmd.password, metadata(cmd))
    }
    .handleEvent {
      _: UserEvent ⇒ this
    }
}

object User {

  import UserProtocol._

  val tag = Tags.aggregateTag("user")

  def metadata(userId: UserId, cmd: UserCommand) = {
    UserMetadata(userId, cmd.id, tags = Set(tag))
  }

  def sendConfirmationEmail(userMetadata: UserMetadata, confirmationCode: String)(implicit ec: ExecutionContext): Future[ConfirmationEmailSent] =
    Future {
      println(s"Sending confirmation email (stub) to: ${userMetadata.aggregateId.email}. Code: $confirmationCode")
      ConfirmationEmailSent(userMetadata)
    }

  def generateConfirmationCode: String = UUID.randomUUID.toString

  def createUser(id: UserId) =
    actions[User]
      .handleCommandAsync {
        cmd: CreateUser ⇒
          val md = metadata(id, cmd)
          val code = generateConfirmationCode
          sendConfirmationEmail(md, code)
            .map(_ ⇒ UserCreated(
              cmd.password,
              cmd.displayName,
              code,
              md
            ))
      }
      .handleEvent {
        e: UserCreated ⇒ User(id, e.password, e.displayName, e.confirmationCode, isConfirmed = false)
      }

  def behavior(fortuneId: UserId): Behavior[User] = {

    case Uninitialized(id) => createUser(id)

    case Initialized(user) =>
      user.cantConfirmWithInvalidCode ++
        user.cantSentConfirmationEmailForConfirmedUser ++
        user.confirmEmail ++
        user.resendConfirmationEmail ++
        user.login

  }
}
