package ru.pavkin.ihavemoney.serialization.derivation

import cats.syntax.all._
import cats.instances.list._
import cats.instances.set._
import cats.{Applicative, FlatMap, Functor, Monad, Traverse}
import shapeless.{::, Generic, HList, Lazy}

import scala.language.{higherKinds, implicitConversions}

/**
  * This typeclass is a proof that instances of `M` can be serialized to instances of `R` and backwards.
  * Backwards conversion (`R => M`) is done under effect `F`
  */
trait Protocol[F[_], M, R] { self =>
  def serialize(model: M): R
  def deserialize(repr: R): F[M]

  def compose[R2](other: Protocol[F, R, R2])(implicit F: FlatMap[F]): Protocol[F, M, R2] =
    new Protocol[F, M, R2] {
      def serialize(model: M): R2 = other.serialize(self.serialize(model))
      def deserialize(repr: R2): F[M] = other.deserialize(repr).flatMap(self.deserialize)
    }

  /**
    * Creates a new Protocol from [[M2]] to [[R]] by using supplied invariant mapping functions
    * on the model values of the Protocol
    *
    * @param to   convert from M to M2
    * @param from convert from M2 to M
    */
  def inmapM[M2](to: M => M2, from: M2 => M)(implicit F: Functor[F]): Protocol[F, M2, R] = new Protocol[F, M2, R] {
    def serialize(model: M2): R = self.serialize(from(model))
    def deserialize(repr: R): F[M2] = self.deserialize(repr).map(to)
  }

  /**
    * Creates a new Protocol from [[M]] to [[R2]] by using supplied invariant mapping functions
    * on the representation values of the Protocol
    *
    * @param to   convert from R to R2
    * @param from convert from R2 to R
    */
  def inmapR[R2](to: R => R2, from: R2 => R)(implicit F: Functor[F]): Protocol[F, M, R2] = new Protocol[F, M, R2] {
    def serialize(model: M): R2 = to(self.serialize(model))
    def deserialize(repr: R2): F[M] = self.deserialize(from(repr))
  }
}

object Protocol extends ProtocolImplicits {

  type Safe[M, R] = Protocol[cats.Id, M, R]
  type Try[M, R] = Protocol[Try.Effect, M, R]

  object Safe {
    def apply[M, R](implicit ev: Protocol.Safe[M, R]) = ev

    def instance[M, R](to: M => R, from: R => M): Safe[M, R] = new Protocol[cats.Id, M, R] {
      def serialize(model: M): R = to(model)
      def deserialize(repr: R): cats.Id[M] = from(repr)
    }
  }

  object Try {
    def apply[M, R](implicit ev: Protocol.Try[M, R]) = ev
    type Effect[T] = Either[Throwable, T]

    def catchException[M, R](to: M => R, from: R => M): Try[M, R] =
      instance(to, r => Either.catchNonFatal(from(r)))

    def scalaTry[M, R](to: M => R, from: R => util.Try[M]): Try[M, R] =
      instance(to, r => Either.catchNonFatal(from(r).get))

    def instance[M, R](to: M => R, from: R => Effect[M]): Try[M, R] = new Protocol[Try.Effect, M, R] {
      def serialize(model: M): R = to(model)
      def deserialize(repr: R): Effect[M] = from(repr)
    }

    def fromSafe[M, R](safe: Protocol.Safe[M, R]): Protocol.Try[M, R] =
      Protocol.Try.instance(
        safe.serialize,
        r => Right(safe.deserialize(r))
      )
  }

  def apply[F[_], M, R](implicit ev: Protocol[F, M, R]) = ev

  trait ProtocolOps[T] {
    def self: T
    def serialize[To](implicit P: Protocol.Try[T, To]): To = P.serialize(self)
    def safeDeserialize[To](implicit P: Protocol.Safe[To, T]): To = P.deserialize(self)
    def tryDeserialize[To](implicit P: Protocol.Try[To, T]): Try.Effect[To] = P.deserialize(self)
  }

  trait ToProtocolOps {
    implicit def toProtocolOps[T](target: T): ProtocolOps[T] = new ProtocolOps[T] {
      val self = target
    }
  }

  object syntax extends ToProtocolOps
}

trait ProtocolImplicits extends LowPriorityProtocolImplicits {
  implicit def identityProtocolInstance[F[_] : Applicative, T]: Protocol[F, T, T] = new Protocol[F, T, T] {
    def serialize(model: T): T = model
    def deserialize(repr: T): F[T] = repr.pure[F]
  }

  implicit def mapProtocolInstance[F[_] : Monad, KM, KR, VM, VR](
    implicit PK: Protocol[F, KM, KR],
    PV: Protocol[F, VM, VR]) = new Protocol[F, Map[KM, VM], Map[KR, VR]] {
    def serialize(model: Map[KM, VM]): Map[KR, VR] = model.map { case (k, v) => PK.serialize(k) -> PV.serialize(v) }
    def deserialize(repr: Map[KR, VR]): F[Map[KM, VM]] = repr.map {
      case (k, v) => PK.deserialize(k).flatMap(k => PV.deserialize(v).map(k -> _))
    }.toList.sequence.map(_.toMap)
  }

  implicit def traverseProtocolInstance[F[_] : Applicative, M, R, L[_] : Traverse](implicit P: Protocol[F, M, R]) =
    new Protocol[F, L[M], L[R]] {
      def serialize(model: L[M]): L[R] = model.map(P.serialize)
      def deserialize(repr: L[R]): F[L[M]] = repr.map(P.deserialize).sequence
    }
}

trait LowPriorityProtocolImplicits {
  implicit def hlistProtocol[F[_] : FlatMap, MH, MT <: HList, RH, RT <: HList](
    implicit PH: Protocol[F, MH, RH],
    PT: Protocol[F, MT, RT]) =
    new Protocol[F, MH :: MT, RH :: RT] {
      def serialize(t: MH :: MT): RH :: RT = PH.serialize(t.head) :: PT.serialize(t.tail)
      def deserialize(t: RH :: RT): F[MH :: MT] =
        PT.deserialize(t.tail)
          .flatMap(tail => PH.deserialize(t.head).map(head => head :: tail))
    }

  implicit def genProtocol[F[_] : Functor, M, MRepr, R, RRepr](
    implicit MG: Generic.Aux[M, MRepr],
    RG: Generic.Aux[R, RRepr],
    GP: Lazy[Protocol[F, MRepr, RRepr]]): Protocol[F, M, R] =
    new Protocol[F, M, R] {
      def serialize(t: M): R = RG.from(GP.value.serialize(MG.to(t)))
      def deserialize(t: R): F[M] = GP.value.deserialize(RG.to(t)).map(MG.from)
    }
}



