package no.nav.bidrag.commons.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IdUtils(
    @Value("\${NAIS_APP_NAME}") private val appName: String,
) {
    fun generateId(): String {
        val uuid = UUID.randomUUID()
        return "${java.lang.Long.toHexString(uuid.mostSignificantBits)}${java.lang.Long.toHexString(uuid.leastSignificantBits)}-$appName"
    }
}
