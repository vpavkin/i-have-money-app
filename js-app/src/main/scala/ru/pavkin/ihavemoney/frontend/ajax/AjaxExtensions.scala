package ru.pavkin.ihavemoney.frontend.ajax

import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.ext.{Ajax, AjaxException}
import ru.pavkin.ihavemoney.protocol.{DecodingError, FailedRequest, HTTPError, RequestError}

import scala.concurrent.{ExecutionContext, Future}

object AjaxExtensions {

  type RequestResult[T] = Either[RequestError, T]

  private def toJson[T: Encoder](v: T): String = v.asJson.noSpaces

  private def defaultHandler(xhr: dom.XMLHttpRequest): Either[RequestError, String] =
    if (xhr.status / 100 == 2) {
      xhr.responseText.asRight
    } else
      HTTPError(xhr.status, decode[FailedRequest](xhr.responseText).toOption, xhr.responseText).asLeft

  private def ajaxExceptionHandler(
    handler: dom.XMLHttpRequest => RequestResult[String]): PartialFunction[Throwable, RequestResult[String]] = {
    case AjaxException(xhr) => handler(xhr)
  }

  def postEmpty(
    url: String,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false)
    (implicit ec: ExecutionContext): Future[RequestResult[String]] =
    Ajax.post(url, null, timeout, headers, withCredentials)
      .map(defaultHandler)
      .recover(ajaxExceptionHandler(defaultHandler))

  def postJson[R: Encoder](
    url: String,
    data: R,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false)
    (implicit ec: ExecutionContext): Future[RequestResult[String]] =
    Ajax.post(url, toJson(data), timeout, headers + ("Content-Type" -> "application/json"), withCredentials)
      .map(defaultHandler)
      .recover(ajaxExceptionHandler(defaultHandler))

  def get(
    url: String,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false)
    (implicit ec: ExecutionContext): Future[RequestResult[String]] =
    Ajax.get(url, timeout = timeout, headers = headers, withCredentials = withCredentials)
      .map(defaultHandler)
      .recover(ajaxExceptionHandler(defaultHandler))

  def expect[A: Decoder](result: Future[RequestResult[String]])
    (implicit ec: ExecutionContext): Future[RequestResult[A]] =
    result.map(_.flatMap(body =>
      decode[A](body).leftMap(DecodingError(_, body))
    ))
}
