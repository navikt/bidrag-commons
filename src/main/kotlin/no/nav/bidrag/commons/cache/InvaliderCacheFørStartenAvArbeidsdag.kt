package no.nav.bidrag.commons.cache

import com.github.benmanes.caffeine.cache.Expiry
import java.time.Duration
import java.time.LocalDateTime

class InvaliderCacheFørStartenAvArbeidsdag : Expiry<Any, Any> {
  override fun expireAfterCreate(p0: Any, p1: Any, currentTime: Long): Long {
    val expireAt = LocalDateTime.now()
      .plusDays(1)
      .withHour(6)
      .withMinute(0)
      .withSecond(0)

    return Duration.between(LocalDateTime.now(), expireAt).toNanos()
  }

  override fun expireAfterUpdate(o1: Any, o2: Any, currentTime: Long, currentDuration: Long): Long {
    return currentDuration
  }

  override fun expireAfterRead(p0: Any, p1: Any, currentTime: Long, currentDuration: Long): Long {
    return currentDuration
  }
}