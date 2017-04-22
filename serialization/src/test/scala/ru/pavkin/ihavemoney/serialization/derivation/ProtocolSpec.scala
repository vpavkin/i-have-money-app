package ru.pavkin.ihavemoney.serialization.derivation

import org.scalatest.{FunSuite, Matchers}
import shapeless.test.illTyped
import cats.syntax.either._
import cats.instances.either._
import scala.language.higherKinds
import scala.util.Try

class ProtocolSpec extends FunSuite with Matchers {

  sealed trait T1
  case class A1(a: Int) extends T1
  case class B1(s: String, a: A1) extends T1

  sealed trait T2
  case class A2(a: Int) extends T2
  case class B2(s: String, a: A2) extends T2

  val ex = new Exception
  val protocolIntStr: Protocol.Try[Int, String] = new Protocol.Try[Int, String] {
    def serialize(model: Int): String = model.toString
    def deserialize(repr: String): Either[Throwable, Int] =
      Either.fromTry(Try(repr.toInt)).leftMap(_ => ex)
  }

  private def testProtocol[F[_], M, R](
    protocol: Protocol[F, M, R])(
    to: List[(M, R)],
    from: List[(R, F[M])]): Unit = {
    to.foreach {
      case (m, r) => protocol.serialize(m) shouldBe r
    }

    from.foreach {
      case (r, m) => protocol.deserialize(r) shouldBe m
    }
  }

  test("Implicit protocol derivation for standard types") {
    val p = Protocol.Safe[String, String]
    Protocol.Try[String, String]
    Protocol.Safe[Int, Int]

    testProtocol(p)(List("a" -> "a"), List("a" -> "a"))

    illTyped("""Protocol.Safe[Int, String]""")
    illTyped("""Protocol.Safe[Double, Int]""")
    illTyped("""Protocol[Future, Double, Int]""")
  }

  test("Implicit protocol derivation for any applicative container") {
    import cats.instances.future._
    import cats.instances.list._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future

    Protocol[Future, Int, Int]
    val p = Protocol[List, Int, Int]
    testProtocol(p)(List(1 -> 1), List(1 -> List(1)))
  }

  test("Implicit protocol derivation for any Traverse, wrapping serialized values") {
    import cats.instances.list._
    import cats.instances.option._
    implicit val strToInt = protocolIntStr

    Protocol.Try[Option[Int], Option[String]]
    val p = Protocol.Try[List[Int], List[String]]
    testProtocol(p)(
      List(List(1, 2) -> List("1", "2")),
      List(
        List("1", "2") -> Right(List(1, 2)),
        List("1", "a") -> Left(ex)
      )
    )
  }

  test("Implicit protocol derivation for case classes") {
    val p1 = Protocol.Safe[A1, A2]
    val p2 = Protocol.Safe[B1, B2]

    testProtocol(p1)(
      List(A1(2) -> A2(2)),
      List(A2(1) -> A1(1))
    )

    testProtocol(p2)(
      List(B1("a", A1(1)) -> B2("a", A2(1))),
      List(B2("a", A2(1)) -> B1("a", A1(1)))
    )

    illTyped("""Protocol.Safe[A1, B1]""")
    illTyped("""Protocol.Safe[A1, B2]""")
    illTyped("""Protocol.Safe[B1, A1]""")
    illTyped("""Protocol.Safe[B1, A2]""")
  }

  test("Implicit protocol derivation for case classes with custom implicits in scope") {

    case class A22(b: String)

    implicit val strToInt = protocolIntStr

    val p = Protocol.Try[A1, A22]
    testProtocol(p)(
      List(A1(12) -> A22("12")),
      List(A22("a") -> Left(ex), A22("12") -> Right(A1(12)))
    )
    illTyped("""Protocol.Safe[A1, A22]""")
  }

  test("Protocol.compose") {
    val strToAny: Protocol.Try[String, Any] =
      Protocol.Try.catchException(s => s: Any, a => Try(a.asInstanceOf[String]).getOrElse(throw ex))

    val p = protocolIntStr.compose(strToAny)

    testProtocol(p)(
      List(1 -> ("1": Any)),
      List(("1": Any) -> Right(1), ("a": Any) -> Left(ex))
    )
  }

  test("Protocol.inmapM") {
    case class A(v: Int)
    val p = protocolIntStr.inmapM[A](A, _.v)

    testProtocol(p)(
      List(A(1) -> "1"),
      List("1" -> Right(A(1)), "a" -> Left(ex))
    )
  }

  test("Protocol.inmapR") {
    case class B(s: String)
    val p = protocolIntStr.inmapR[B](B, _.s)

    testProtocol(p)(
      List(1 -> B("1")),
      List(B("1") -> Right(1), B("a") -> Left(ex))
    )
  }
}
