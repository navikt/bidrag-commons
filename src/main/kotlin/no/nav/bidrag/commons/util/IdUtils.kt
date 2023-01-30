package no.nav.bidrag.commons.util

import java.util.*

object IdUtils {
  fun generateId(): String {
    val uuid = UUID.randomUUID()
    return java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
  }
}
