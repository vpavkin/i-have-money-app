package ru.pavkin.ihavemoney.protocol

sealed trait RequestError extends Exception {
  final override def fillInStackTrace(): Throwable = this
}
case class HTTPError(statusCode: Int, requestData: Option[FailedRequest], body: String) extends RequestError {
  override def getMessage: String = s"HTTP request failed with code $statusCode"
}

case class DecodingError(underlying: io.circe.Error, body: String) extends RequestError {
  override def getMessage: String = s"Failed to parse result body. Underlying exception message: ${underlying.getMessage}"
}

case class OtherError(message: String) extends RequestError {
  override def getMessage: String = message
}
