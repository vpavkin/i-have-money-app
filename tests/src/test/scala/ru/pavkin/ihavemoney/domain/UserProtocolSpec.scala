package ru.pavkin.ihavemoney.domain

import io.funcqrs.config.Api._
import io.funcqrs.test.InMemoryTestSupport
import io.funcqrs.test.backend.InMemoryBackend
import org.scalatest.concurrent.ScalaFutures
import ru.pavkin.ihavemoney.domain._
import ru.pavkin.ihavemoney.domain.errors._
import ru.pavkin.ihavemoney.domain.user.UserProtocol._
import ru.pavkin.ihavemoney.domain.user.{User, UserId}
import ru.pavkin.ihavemoney.writeback.services.DummyEmailService

class UserProtocolSpec extends IHaveMoneySpec with ScalaFutures {

  class UserInMemoryTestBase extends InMemoryTestSupport {

    val userId = UserId("user@example.org")
    val displayName = "A user"
    val password = "secret"

    val emailService = new DummyEmailService

    def configure(backend: InMemoryBackend): Unit =
      backend.configure {
        aggregate[User](User.behavior(emailService, _ + _))
      }

    def ref(id: UserId) = aggregateRef[User](id)
  }

  class UserInMemoryTest extends UserInMemoryTestBase {
    val user = ref(userId)

    user ! CreateUser(cmdId, password, displayName)

    var confirmationCode: String = ""

    expectEventPF {
      case UserCreated(hash, dName, code, _) if passwords.isCorrect(password, hash) && dName == displayName ⇒
        confirmationCode = code
    }
  }

  test("User must confirm email") {
    new UserInMemoryTest {
      intercept[InvalidConfirmationCode.type] {
        user ? ConfirmEmail(cmdId, "smth")
      }.getMessage should include("Invalid confirmation code")
      expectNoEvent()

      intercept[EmailIsNotYetConfirmed.type] {
        user ? LoginUser(cmdId, password)
      }.getMessage should include("Email is not yet confirmed")
      expectNoEvent()

      user ! ConfirmEmail(cmdId, confirmationCode)
      expectEvent[UserConfirmed]

      intercept[EmailAlreadyConfirmed.type] {
        user ? ConfirmEmail(cmdId, confirmationCode)
      }.getMessage should include("Email already confirmed")
      expectNoEvent()
    }
  }

  test("User can resend confirmation email") {
    new UserInMemoryTest {

      user ! ResendConfirmationEmail(cmdId)
      expectEvent[ConfirmationEmailSent]

      user ! ConfirmEmail(cmdId, confirmationCode)
      expectEvent[UserConfirmed]

      intercept[EmailAlreadyConfirmed.type] {
        user ? ResendConfirmationEmail(cmdId)
      }.getMessage should include("Email already confirmed")
      expectNoEvent()
    }
  }
  // todo: test for email resend throttling

  test("User can log in with his own password") {
    new UserInMemoryTest {

      user ! ConfirmEmail(cmdId, confirmationCode)
      expectEvent[UserConfirmed]

      user ! LoginUser(cmdId, password)
      expectEvent[UserLoggedIn]

      user ! LoginUser(cmdId, "incorrect password")
      expectEvent[UserFailedToLogIn]
    }
  }

}
