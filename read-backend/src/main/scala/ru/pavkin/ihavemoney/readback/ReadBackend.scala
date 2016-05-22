package ru.pavkin.ihavemoney.readback

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import io.funcqrs.akka.EventsSourceProvider
import io.funcqrs.akka.backend.AkkaBackend
import io.funcqrs.backend.{Query, QueryByTag}
import io.funcqrs.config.api._
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.readback.projections.MoneyViewProjection
import ru.pavkin.ihavemoney.readback.repo.{DatabaseAssetsViewRepository, DatabaseLiabilitiesViewRepository, DatabaseMoneyViewRepository}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

object ReadBackend extends App {

  println("Starting IHaveMoney read backend...")

  val config = ConfigFactory.load()
  val system: ActorSystem = ActorSystem(config.getString("app.system"))

  val database: PostgresDriver.Backend#Database = Database.forConfig("read-db")
  val moneyViewRepo = new DatabaseMoneyViewRepository(database)
  val assetsViewRepo = new DatabaseAssetsViewRepository(database)
  val liabilitiesViewRepo = new DatabaseLiabilitiesViewRepository(database)

  val backend = new AkkaBackend {
    val actorSystem: ActorSystem = system
    def sourceProvider(query: Query): EventsSourceProvider = {
      query match {
        case QueryByTag(Fortune.tag) ⇒ new FortuneTagEventSourceProvider(Fortune.tag)
      }
    }
  }.configure {
    projection(
      query = QueryByTag(Fortune.tag),
      projection = new MoneyViewProjection(moneyViewRepo, assetsViewRepo, liabilitiesViewRepo),
      name = "MoneyViewProjection"
    ).withBackendOffsetPersistence()
  }

  val interface = system.actorOf(Props(new InterfaceActor(moneyViewRepo)), "interface")
}
