package ru.pavkin.ihavemoney.readback

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import io.funcqrs.akka.EventsSourceProvider
import io.funcqrs.akka.backend.AkkaBackend
import io.funcqrs.backend.{Query, QueryByTag}
import io.funcqrs.config.api._
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.readback.projections._
import ru.pavkin.ihavemoney.readback.repo._
import ru.pavkin.ihavemoney.readback.sources.{CurrentFortuneTagEventSourceProvider, FortuneTagEventSourceProvider}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

import scala.util.Try

object ReadBackend extends App {

  println("Starting IHaveMoney read backend...")

  val config = ConfigFactory.load()
  val system: ActorSystem = ActorSystem(config.getString("app.system"))

  val persistQuerySide = Try(config.getBoolean("app.cache-query-side")).getOrElse(false)

  val database: PostgresDriver.Backend#Database = Database.forConfig("read-db")

  val moneyViewRepo =
    if (persistQuerySide) new DatabaseMoneyViewRepository(database)
    else new InMemoryMoneyViewRepository
  val assetsViewRepo =
    if (persistQuerySide) new DatabaseAssetsViewRepository(database)
    else new InMemoryAssetsViewRepository
  val liabilitiesViewRepo =
    if (persistQuerySide) new DatabaseLiabilitiesViewRepository(database)
    else new InMemoryLiabilitiesViewRepository


  val categoriesRepo = new InMemoryRepository[FortuneId, (Set[IncomeCategory], Set[ExpenseCategory])] {}
  val userRegistryRepo = new InMemoryRepository[UserId, Set[FortuneId]] {}
  val fortuneInfoRepo = new InMemoryRepository[FortuneId, FortuneInfo] {}

  val fortuneEventsProvider = new FortuneTagEventSourceProvider(Fortune.tag)

  val persistentProjectionConfig = projection(
    query = QueryByTag(Fortune.tag),
    projection = new MoneyViewProjection(moneyViewRepo, assetsViewRepo, liabilitiesViewRepo)
        .andThen(new AssetsViewProjection(assetsViewRepo))
        .andThen(new LiabilitiesViewProjection(liabilitiesViewRepo)),
    name = "FortuneViewProjection"
  )

  val backend = new AkkaBackend {
    val actorSystem: ActorSystem = system
    def sourceProvider(query: Query): EventsSourceProvider = {
      query match {
        case QueryByTag(Fortune.tag) ⇒ fortuneEventsProvider
      }
    }
  }.configure {
    if (persistQuerySide) persistentProjectionConfig.withBackendOffsetPersistence()
    else persistentProjectionConfig.withoutOffsetPersistence()
  }.configure {
    projection(
      query = QueryByTag(Fortune.tag),
      projection = new CategoriesViewProjection(categoriesRepo)
          .andThen(new FortunesPerUserProjection(userRegistryRepo))
          .andThen(new FortuneInfoProjection(fortuneInfoRepo)),
      name = "InMemoryFortuneViewProjection"
    ).withoutOffsetPersistence()
  }

  val interface = system.actorOf(Props(new InterfaceActor(
    new CurrentFortuneTagEventSourceProvider(Fortune.tag),
    moneyViewRepo,
    assetsViewRepo,
    liabilitiesViewRepo,
    categoriesRepo,
    userRegistryRepo,
    fortuneInfoRepo)), "interface")
}
