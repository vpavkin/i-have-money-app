package ru.pavkin.ihavemoney.auth

import java.time.Instant
import java.time.temporal.ChronoUnit

import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

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

  def authenticate(token: String): Option[String] =
    JwtCirce.decode(token, secretKey, Seq(JwtAlgorithm.HS256)).toOption.map(_.content)
}
