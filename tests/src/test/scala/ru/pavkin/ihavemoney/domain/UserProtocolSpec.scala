package ru.pavkin.ihavemoney.domain

import io.funcqrs.CommandException
import io.funcqrs.backend.QueryByTag
import io.funcqrs.config.Api._
import io.funcqrs.test.InMemoryTestSupport
import io.funcqrs.test.backend.InMemoryBackend
import org.scalatest.concurrent.ScalaFutures
import ru.pavkin.ihavemoney.domain.errors._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, Fortune, FortuneId}
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.domain.user.{User, UserId}
import ru.pavkin.ihavemoney.readback.MoneyViewProjection

import scala.concurrent.ExecutionContext.Implicits.global

class UserProtocolSpec extends IHaveMoneySpec with ScalaFutures {

  class UserInMemoryTestBase extends InMemoryTestSupport {

    val userId = UserId("user@example.org")
    val displayName = "A user"
    val password = "secret"

    def configure(backend: InMemoryBackend): Unit =
      backend.configure {
        aggregate[User](User.behavior)
      }

    def ref(id: UserId) = aggregateRef[User](id)
  }

  class UserInMemoryTest extends UserInMemoryTestBase {
    val user = ref(userId)

    user ! CreateUser(password, displayName)

    var confirmationCode: String = ""

    expectEvent {
      case UserCreated(hash, dName, code, _) if passwords.isCorrect(password, hash) && dName == displayName ⇒
        confirmationCode = code
    }
  }

  test("User must confirm email") {
    new UserInMemoryTest {
      intercept[InvalidConfirmationCode.type] {
        user ? ConfirmEmail("smth")
      }.getMessage should include("Invalid confirmation code")
      expectNoEvent()

      intercept[EmailIsNotYetConfirmed.type] {
        user ? LoginUser(password)
      }.getMessage should include("Email is not yet confirmed")
      expectNoEvent()

      user ! ConfirmEmail(confirmationCode)
      expectEventType[UserConfirmed]

      intercept[EmailAlreadyConfirmed.type] {
        user ? ConfirmEmail(confirmationCode)
      }.getMessage should include("Email already confirmed")
      expectNoEvent()
    }
  }

  test("User can resend confirmation email") {
    new UserInMemoryTest {

      user ! ResendConfirmationEmail()
      expectEventType[ConfirmationEmailSent]

      user ! ConfirmEmail(confirmationCode)
      expectEventType[UserConfirmed]

      intercept[EmailAlreadyConfirmed.type] {
        user ? ResendConfirmationEmail()
      }.getMessage should include("Email already confirmed")
      expectNoEvent()
    }
  }
  // todo: test for email resend throttling

  test("User can log in with his own password") {
    new UserInMemoryTest {

      user ! ConfirmEmail(confirmationCode)
      expectEventType[UserConfirmed]

      user ! LoginUser(password)
      expectEventType[UserLoggedIn]

      user ! LoginUser("incorrect password")
      expectEventType[UserFailedToLogIn]
    }
  }

}
