package ru.pavkin.ihavemoney.writeback

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.funcqrs.akka.EventsSourceProvider
import io.funcqrs.akka.backend.AkkaBackend
import io.funcqrs.backend.Query
import io.funcqrs.config.api._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.FortuneCommand
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.domain.user.UserProtocol.UserCommand
import ru.pavkin.ihavemoney.domain.user.{User, UserId}
import services.{SmtpConfig, SmtpEmailService}

import scala.concurrent.duration._

object WriteBackend extends App {

  println("Starting IHaveMoney write backend...")

  val config = ConfigFactory.load()
  val system: ActorSystem = ActorSystem(config.getString("app.system"))

  val emailUrlBase = s"http://${config.getString("app.writefront.host")}:${config.getString("app.writefront.port")}"
  def confirmationUrlFactory(email: String, code: String) = s"$emailUrlBase/confirmEmail?email=$email&code=$code"
  val emailConfig = SmtpConfig.load(config)
  val emailService = new SmtpEmailService(emailConfig)

  val backend = new AkkaBackend {
    val actorSystem: ActorSystem = system
    def sourceProvider(query: Query): EventsSourceProvider = null
  }.configure {
    aggregate[User](User.behavior(emailService, confirmationUrlFactory))
  }.configure {
    aggregate[Fortune](Fortune.behavior)
  }

  implicit val timeout: Timeout = new Timeout(30.seconds)

  val fortuneRegion = ClusterSharding(system).start(
    typeName = "FortuneShard",
    entityProps = Props(new AggregateOffice[Fortune, FortuneCommand](backend, FortuneId(_))),
    settings = ClusterShardingSettings(system),
    messageExtractor = new CommandMessageExtractor(config.getInt("app.number-of-nodes"))
  )

  val userRegion = ClusterSharding(system).start(
    typeName = "UserShard",
    entityProps = Props(new AggregateOffice[User, UserCommand](backend, UserId)),
    settings = ClusterShardingSettings(system),
    messageExtractor = new CommandMessageExtractor(config.getInt("app.number-of-nodes"))
  )

  val interface = system.actorOf(Props(
    new WritebackInterfaceActor(fortuneRegion, userRegion)),
    "interface"
  )
  ClusterClientReceptionist(system).registerService(interface)
}
