package ru.pavkin.ihavemoney.auth

import java.time.Instant
import java.time.temporal.ChronoUnit

import io.circe.parser._
import io.circe.generic.auto._
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import ru.pavkin.ihavemoney.domain.user.UserId

class JWTTokenFactory(secretKey: String) {

  def issue(email: String): String =
    JwtCirce.encode(
      JwtClaim(
        issuedAt = Some(Instant.now().getEpochSecond),
        expiration = Some(Instant.now().plus(300, ChronoUnit.DAYS).getEpochSecond)
      ) ++ ("email" → email),
      secretKey,
      JwtAlgorithm.HS256
    )

  def authenticate(token: String): Option[UserId] =
    JwtCirce.decode(token, secretKey, Seq(JwtAlgorithm.HS256)).toOption.flatMap(
      c ⇒ decode[UserId](c.content).toOption
    )
}
