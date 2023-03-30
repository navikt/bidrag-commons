package no.nav.bidrag.commons

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.bidrag.commons.CorrelationId.Companion.existing
import no.nav.bidrag.commons.CorrelationId.Companion.generateTimestamped
import org.junit.jupiter.api.Test

internal class CorrelationIdTest {
    @Test
    fun `skal lage correlation id med eksisterende verdi`() {
        val correlationId = existing("eksisterende")
        correlationId.get() shouldBe "eksisterende"
    }

    @Test
    fun `skal lage correlation id som tidsstemplet verdi`() {
        val correlationId = generateTimestamped("value")
        val timestamp = java.lang.Long.toHexString(System.currentTimeMillis())
        correlationId.get() shouldStartWith timestamp.substring(0, timestamp.length - 3) shouldContain "-value"
    }

    @Test
    fun `skal generere ny verdi når gitt Correlation ID er null`() {
        val correlationId = existing(null)
        correlationId.get() shouldContain "-correlationId"
    }
}
