package ru.pavkin.ihavemoney.writefront.auth

import java.time.Instant
import java.time.temporal.ChronoUnit

import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

class JWTTokenFactory(secretKey: String) {

  def issue(email: String): String =
    JwtCirce.encode(
      JwtClaim(
        content = email,
        issuedAt = Some(Instant.now().getEpochSecond),
        expiration = Some(Instant.now().plus(1, ChronoUnit.YEARS).getEpochSecond)
      ),
      secretKey,
      JwtAlgorithm.HS256
    )

}
