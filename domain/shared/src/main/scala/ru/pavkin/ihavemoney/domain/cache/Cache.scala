package ru.pavkin.ihavemoney.domain.cache

/**
  * Read-only in-memory cache that provides instant access to some state T
  */
trait Cache[T] {
  def state: T
}
