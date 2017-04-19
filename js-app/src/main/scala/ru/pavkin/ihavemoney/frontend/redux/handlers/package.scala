package ru.pavkin.ihavemoney.frontend.redux


import diode.Effect
import diode.data.AsyncAction

import scala.concurrent.{ExecutionContext, Future}

package object handlers {
  implicit class AsyncActionUtilOps[A, P <: AsyncAction[A, P]](a: AsyncAction[A, P]) {
    def effectEither[E <: Throwable, B](f: => Future[Either[E, B]])(success: B => A, failure: Throwable => Throwable = identity)
                                    (implicit ec: ExecutionContext) = Effect {
      f.flatMap {
        case Left(exception) ⇒ Future.failed(exception)
        case Right(result) ⇒ Future.successful(result)
      }.map(x => a.ready(success(x))).recover { case e: Throwable => a.failed(failure(e)) }
    }
  }
}
