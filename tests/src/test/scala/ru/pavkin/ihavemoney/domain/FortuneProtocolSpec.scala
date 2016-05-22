package ru.pavkin.ihavemoney.domain

import io.funcqrs.CommandException
import io.funcqrs.backend.QueryByTag
import io.funcqrs.config.Api._
import io.funcqrs.test.InMemoryTestSupport
import io.funcqrs.test.backend.InMemoryBackend
import org.scalatest.concurrent.ScalaFutures
import ru.pavkin.ihavemoney.domain.errors._
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.readback.projections.{AssetsViewProjection, LiabilitiesViewProjection, MoneyViewProjection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FortuneProtocolSpec extends IHaveMoneySpec with ScalaFutures {

  class FortuneInMemoryTestBase extends InMemoryTestSupport {

    val moneyRepo = new InMemoryMoneyViewRepository
    val assetsRepo = new InMemoryAssetsViewRepository
    val liabRepo = new InMemoryLiabilitiesViewRepository
    val owner = UserId("owner@example.org")
    val id = FortuneId.generate

    def viewShouldBeEmpty[K, V](view: Map[K, V]) =
      view shouldBe Map.empty

    def assets = assetsRepo.findAll(id).futureValue
    def liabilities = liabRepo.findAll(id).futureValue
    def money = moneyRepo.findAll(id).futureValue

    def configure(backend: InMemoryBackend): Unit =
      backend.configure {
        aggregate[Fortune](Fortune.behavior)
      }
        .configure {
          projection(
            query = QueryByTag(Fortune.tag),
            projection = new MoneyViewProjection(moneyRepo, assetsRepo, liabRepo)
              .andThen(new AssetsViewProjection(assetsRepo))
              .andThen(new LiabilitiesViewProjection(liabRepo)),
            name = "ViewProjection"
          )
        }

    def ref(id: FortuneId) = aggregateRef[Fortune](id)
  }

  class FortuneInMemoryTest extends FortuneInMemoryTestBase {
    val fortune = ref(id)

    fortune ! CreateFortune(owner)
    expectEvent { case FortuneCreated(owner, _) if owner == this.owner ⇒ () }
  }

  test("Create fortune") {
    new FortuneInMemoryTest {
      viewShouldBeEmpty(money)
      viewShouldBeEmpty(assets)
      viewShouldBeEmpty(liabilities)
    }
  }

  test("Fortune can't be changed by somebody who's not the owner or editor") {
    new FortuneInMemoryTest {
      val hacker = UserId("hacker@example.com")

      intercept[InsufficientAccessRights] {
        fortune ? ReceiveIncome(hacker, BigDecimal(123.12), Currency.USD, IncomeCategory("salary"))
      }.getMessage should include("not allowed to perform this command")

      intercept[InsufficientAccessRights] {
        fortune ? AddEditor(hacker, hacker)
      }.getMessage should include("not allowed to perform this command")

      expectNoEvent()

      fortune ! AddEditor(owner, hacker)
      expectEventType[EditorAdded]

      intercept[InsufficientAccessRights] {
        fortune ? AddEditor(hacker, hacker)
      }.getMessage should include("not allowed to perform this command")

      fortune ! ReceiveIncome(hacker, BigDecimal(123.12), Currency.USD, IncomeCategory("salary"))
      expectEventType[FortuneIncreased]
    }
  }

  test("Increase fortune") {

    new FortuneInMemoryTest {
      fortune ! ReceiveIncome(owner, BigDecimal(123.12), Currency.USD, IncomeCategory("salary"))
      fortune ! ReceiveIncome(owner, BigDecimal(20), Currency.EUR, IncomeCategory("salary"))
      fortune ! ReceiveIncome(owner, BigDecimal(30.5), Currency.EUR, IncomeCategory("salary"))

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, _, _, None) if amount.toDouble == 123.12 ⇒ () }
      expectEvent { case FortuneIncreased(_, amount, Currency.EUR, _, _, _, None) if amount.toDouble == 20.0 ⇒ () }
      expectEvent { case FortuneIncreased(_, amount, Currency.EUR, _, _, _, None) if amount.toDouble == 30.5 ⇒ () }

      val view = money
      view(Currency.USD) shouldBe BigDecimal(123.12)
      view(Currency.EUR) shouldBe BigDecimal(50.5)
      viewShouldBeEmpty(assets)
      viewShouldBeEmpty(liabilities)
    }
  }

  test("Increase and decrease fortune") {

    new FortuneInMemoryTest {
      fortune ! ReceiveIncome(owner, BigDecimal(123.12), Currency.USD, IncomeCategory("salary"))
      fortune ! Spend(owner, BigDecimal(20), Currency.USD, ExpenseCategory("food"))

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, _, _, None) if amount.toDouble == 123.12 ⇒ () }
      expectEvent { case FortuneSpent(_, amount, Currency.USD, _, _, _, None) if amount.toDouble == 20.0 ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(103.12)
    }
  }

  test("Editing not initialized fortune produces an error") {

    new FortuneInMemoryTestBase {
      val fortune = ref(id)
      intercept[CommandException] {
        fortune ? ReceiveIncome(owner, BigDecimal(20), Currency.USD, IncomeCategory("food"))
      }.getMessage should startWith("Invalid command ReceiveIncome")
    }
  }

  test("Spending more than is available is not allowed") {
    new FortuneInMemoryTest {

      fortune ? ReceiveIncome(owner, BigDecimal(10), Currency.USD, IncomeCategory("salary"))

      val message = intercept[BalanceIsNotEnough] {
        fortune ? Spend(owner, BigDecimal(20), Currency.USD, ExpenseCategory("food"))
      }.getMessage
      message should startWith("Your balance")
      message should endWith("is not enough for this operation")
    }
  }

  test("Initialization Mode") {
    new FortuneInMemoryTest {

      fortune ! ReceiveIncome(owner, BigDecimal(10), Currency.USD, IncomeCategory("salary"), true)
      fortune ! ReceiveIncome(owner, BigDecimal(10), Currency.USD, IncomeCategory("salary"), false)

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, true, _, None) ⇒ () }
      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, false, _, None) ⇒ () }

      fortune ? FinishInitialization(owner)
      expectEventType[FortuneInitializationFinished]

      fortune ! ReceiveIncome(owner, BigDecimal(10), Currency.USD, IncomeCategory("salary"), false)
      expectEventType[FortuneIncreased]

      val message = intercept[FortuneAlreadyInitialized] {
        fortune ? ReceiveIncome(owner, BigDecimal(10), Currency.USD, IncomeCategory("salary"), initializer = true)
      }.getMessage

      message should include("already initialized")

      intercept[FortuneAlreadyInitialized] {
        fortune ? FinishInitialization(owner)
      }

      money(Currency.USD) shouldBe BigDecimal(30)
    }
  }

  test("Buy Assets with initializer = true does not reduce fortune") {
    new FortuneInMemoryTest {

      val asset = RealEstate("House", BigDecimal(100000), Currency.USD)

      fortune ! BuyAsset(owner, asset, initializer = true)

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, true, _, _) ⇒ () }
      expectEvent { case AssetAcquired(_, _, ass, true, _, _) if ass == asset ⇒ () }

      intercept[BalanceIsNotEnough] {
        fortune ? BuyAsset(owner, asset, initializer = false)
      }
      money(Currency.USD) shouldBe BigDecimal(0)
      assets.size shouldBe 1
      assets.values.toList.head shouldBe asset
    }
  }

  test("Buy Assets with initializer = false reduces fortune") {
    new FortuneInMemoryTest {

      val asset = RealEstate("House", BigDecimal(100000), Currency.USD)

      intercept[BalanceIsNotEnough] {
        fortune ? BuyAsset(owner, asset, initializer = false)
      }

      fortune ! ReceiveIncome(owner, BigDecimal(120000), Currency.USD, IncomeCategory("salary"), false)
      fortune ! BuyAsset(owner, asset, initializer = false)

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, false, _, _) ⇒ () }
      expectEvent { case AssetAcquired(_, _, ass, false, _, _) if ass == asset ⇒ () }


      money(Currency.USD) shouldBe BigDecimal(20000)
      assets.size shouldBe 1
      assets.values.toList.head shouldBe asset
    }
  }


  test("Sell Assets") {
    new FortuneInMemoryTest {

      val asset = RealEstate("House", BigDecimal(10000), Currency.USD)
      var assetId: AssetId = AssetId.generate

      fortune ! BuyAsset(owner, asset, initializer = true)
      fortune ! BuyAsset(owner, Stocks("", BigDecimal(100), Currency.USD, BigDecimal(300)), initializer = true)

      expectEventType[FortuneIncreased]
      expectEvent { case AssetAcquired(_, assId, ass, true, _, _) if ass == asset ⇒ assetId = assId }
      expectEventType[FortuneIncreased]
      expectEventType[AssetAcquired]
      assets.size shouldBe 2

      val message = intercept[AssetNotFound] {
        fortune ? SellAsset(owner, AssetId.generate)
      }.getMessage
      message should include("not found")

      fortune ! SellAsset(owner, assetId)
      expectEvent { case AssetSold(_, assId, _, _) if assId == assetId ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(10000)
      assets.size shouldBe 1
      assets.values.toList.head should not be asset
    }
  }

  test("Stocks special handling") {
    new FortuneInMemoryTest {

      val asset = Stocks("Apple", BigDecimal(432.15), Currency.USD, BigDecimal(250))
      var assetId: AssetId = AssetId.generate

      fortune ! ReceiveIncome(owner, BigDecimal(3210), Currency.USD, IncomeCategory("salary"))
      expectEventType[FortuneIncreased]

      fortune ! BuyAsset(owner, asset, initializer = true)

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, _, _, _) if amount == asset.price ⇒ () }
      expectEvent { case AssetAcquired(_, assId, ass, _, _, _) if ass == asset ⇒ assetId = assId }
      assets.values.toList.head shouldBe asset

      money(Currency.USD) shouldBe BigDecimal(3210)

      fortune ! SellAsset(owner, assetId)
      expectEvent { case AssetSold(_, assId, _, _) if assId == assetId ⇒ () }

      money(Currency.USD) shouldBe (BigDecimal(3210) + asset.price)
      viewShouldBeEmpty(assets)
    }
  }

  test("Reevaluate Assets") {
    new FortuneInMemoryTest {

      val asset = RealEstate("House", BigDecimal(100000), Currency.USD)
      var assetId: AssetId = AssetId.generate

      fortune ! BuyAsset(owner, asset, initializer = true)

      expectEventType[FortuneIncreased]
      expectEvent { case AssetAcquired(_, assId, ass, true, _, _) if ass == asset ⇒ assetId = assId }

      fortune ! ReevaluateAsset(owner, assetId, BigDecimal(50000))
      expectEvent { case AssetWorthChanged(_, assId, amount, _, _) if amount == BigDecimal(50000) ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(0)
      assets.values.toList.head.price shouldBe BigDecimal(50000)
    }
  }

  test("Liabilities") {
    new FortuneInMemoryTest {

      val liability = NoInterestDebt("House", BigDecimal(1000), Currency.USD)
      var liabilityId: LiabilityId = LiabilityId.generate

      fortune ! TakeOnLiability(owner, liability)
      expectEvent { case LiabilityTaken(_, liabId, liab, false, _, _) if liab == liability ⇒ liabilityId = liabId }

      money(Currency.USD) shouldBe BigDecimal(1000)
      liabilities.size shouldBe 1
      liabilities.values.toList.head shouldBe liability

      fortune ! PayLiabilityOff(owner, liabilityId, BigDecimal(400))
      expectEvent { case LiabilityPaidOff(_, assId, amount, _, _) if amount == BigDecimal(400) ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(600)
      liabilities.size shouldBe 1
      liabilities.values.toList.head.amount shouldBe BigDecimal(600)

      fortune ! PayLiabilityOff(owner, liabilityId, BigDecimal(600))
      expectEventType[LiabilityPaidOff]

      money(Currency.USD) shouldBe BigDecimal(0)
      liabilities.size shouldBe 0

      val message = intercept[LiabilityNotFound] {
        fortune ? PayLiabilityOff(owner, liabilityId, BigDecimal(10))
      }.getMessage
      message should include("not found")
    }
  }

  test("Currency exchange") {
    new FortuneInMemoryTest {
      val exchange = ExchangeCurrency(owner, BigDecimal(100), Currency.USD, BigDecimal(80), Currency.EUR)
      intercept[BalanceIsNotEnough] {
        fortune ? exchange
      }.getMessage

      expectNoEvent

      fortune ! ReceiveIncome(owner, BigDecimal(123.12), Currency.USD, IncomeCategory("salary"))
      fortune ! exchange

      expectEventType[FortuneIncreased]
      expectEvent { case CurrencyExchanged(_, fromAmount, Currency.USD, toAmount, Currency.EUR, _, None)
        if fromAmount == BigDecimal(100) && toAmount == BigDecimal(80) ⇒ ()
      }
      money(Currency.USD) shouldBe BigDecimal(23.12)
      money(Currency.EUR) shouldBe BigDecimal(80)
    }
  }

  test("Corrections") {
    new FortuneInMemoryTest {
      val correction = CorrectBalances(owner, Map(Currency.USD → BigDecimal(100), Currency.RUR → BigDecimal(2000)))

      fortune ! correction

      expectEvent { case FortuneIncreased(_, amount, Currency.USD, _, false, _, _) if amount == BigDecimal(100) ⇒ () }
      expectEvent { case FortuneIncreased(_, amount, Currency.RUR, _, false, _, _) if amount == BigDecimal(2000) ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(100)
      money(Currency.RUR) shouldBe BigDecimal(2000)

      fortune ! ReceiveIncome(owner, BigDecimal(50), Currency.USD, IncomeCategory("salary"))
      expectEventType[FortuneIncreased]

      money(Currency.USD) shouldBe BigDecimal(150)

      fortune ! correction
      expectEvent { case FortuneSpent(_, amount, Currency.USD, _, false, _, _) if amount == BigDecimal(50) ⇒ () }

      money(Currency.USD) shouldBe BigDecimal(100)
      money(Currency.RUR) shouldBe BigDecimal(2000)
    }
  }
}
