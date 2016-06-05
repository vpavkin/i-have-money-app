package ru.pavkin.ihavemoney.domain.user

import java.util.UUID

import io.funcqrs._
import io.funcqrs.behavior._
import ru.pavkin.ihavemoney.domain.errors.{EmailAlreadyConfirmed, EmailIsNotYetConfirmed, InvalidConfirmationCode}
import ru.pavkin.ihavemoney.domain.passwords
import ru.pavkin.ihavemoney.services.EmailService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

// todo: inject email service into behaviour
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
      case cmd: ConfirmEmail if this.confirmationCode != cmd.confirmationCode ⇒
        InvalidConfirmationCode
    }

  def cantLogInUnconfirmedUser = action[User]
    .rejectCommand {
      case cmd: LoginUser if !this.isConfirmed ⇒
        EmailIsNotYetConfirmed
    }

  def cantSentConfirmationEmailForConfirmedUser = action[User]
    .rejectCommand {
      case cmd: ResendConfirmationEmail if this.isConfirmed ⇒
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

  def resendConfirmationEmail(emailService: EmailService, linkFactory: (String, String) ⇒ String) = action[User]
    .handleCommandAsync[ResendConfirmationEmail, ConfirmationEmailSent] {
    cmd ⇒ User.sendConfirmationEmail(emailService, linkFactory)(metadata(cmd), this.confirmationCode)
  }
    .handleEvent {
      evt: ConfirmationEmailSent ⇒ this
    }

  def login = action[User]
    .handleCommand {
      cmd: LoginUser ⇒ if (passwords.isCorrect(cmd.password, this.passwordHash))
        UserLoggedIn(metadata(cmd))
      else
        UserFailedToLogIn(cmd.password, metadata(cmd))
    }
    .handleEvent {
      _: UserLoggedIn ⇒ this
    }
    .handleEvent {
      _: UserFailedToLogIn ⇒ this
    }
}

object User {

  import UserProtocol._

  val tag = Tags.aggregateTag("user")

  def metadata(userId: UserId, cmd: UserCommand) = {
    UserMetadata(userId, cmd.id, tags = Set(tag))
  }

  def sendConfirmationEmail(emailService: EmailService, linkFactory: (String, String) ⇒ String)(userMetadata: UserMetadata, confirmationCode: String)(implicit ec: ExecutionContext): Future[ConfirmationEmailSent] = {
    println(s"Sending confirmation email to: ${userMetadata.aggregateId.email}. Code: $confirmationCode")
    val url = linkFactory(userMetadata.aggregateId.email, confirmationCode)
    emailService.sendEmail("ihavemoney@ihavemoney.com", userMetadata.aggregateId.email, "I have money: email confirmation",
      s"""<h3>Hi!</h3>
         <p>It's "I have money" service.</p>
         <p>Please, confirm your email by clicking following link:</p>
         <a href="$url">$url</a>

      """
    ).map(_ ⇒ ConfirmationEmailSent(userMetadata))
  }

  def generateConfirmationCode: String = UUID.randomUUID.toString

  def createUser(emailService: EmailService, linkFactory: (String, String) ⇒ String)(id: UserId) =
    actions[User]
      .handleCommandAsync {
        cmd: CreateUser ⇒
          val md = metadata(id, cmd)
          val code = generateConfirmationCode
          sendConfirmationEmail(emailService, linkFactory)(md, code)
            .map(_ ⇒ UserCreated(
              passwords.encrypt(cmd.password),
              cmd.displayName,
              code,
              md
            ))
      }
      .handleEvent {
        e: UserCreated ⇒ User(id, e.passwordHash, e.displayName, e.confirmationCode, isConfirmed = false)
      }

  def behavior(emailService: EmailService, linkFactory: (String, String) ⇒ String)(fortuneId: UserId): Behavior[User] = {

    case Uninitialized(id) ⇒ createUser(emailService, linkFactory)(id)

    case Initialized(user) ⇒
      user.cantConfirmWithInvalidCode ++
        user.cantSentConfirmationEmailForConfirmedUser ++
        user.cantLogInUnconfirmedUser ++
        user.confirmEmail ++
        user.resendConfirmationEmail(emailService, linkFactory) ++
        user.login

  }
}
