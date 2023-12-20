package no.nav.bidrag.commons.logging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SensitiveLogMaskerTest {
    @Test
    fun shouldMaskAktoerId() {
        val masker = SensitiveLogMasker()
        val maskedMessage = masker.maskLogMessage("Some message 1234567891033 aktoerid")
        maskedMessage shouldBe "Some message ************* aktoerid"
    }

    @Test
    fun shouldNotMaskCorrelationId() {
        val masker = SensitiveLogMasker()
        val maskedMessage =
            masker.maskLogMessage(
                "{\"applicationKey\":\"asdasd-sadas-ddas-qwe\",\"correlationId\":\"1234567891033-avvik\"," +
                    "\"user\":\"Z999444\",\"@timestamp\":\"2023-03-20T07:11:37.596+01:00\"," +
                    "\"message\":\"Some sensitive message 1234567891033\",\"level\":\"INFO\"}\n",
            )
        maskedMessage shouldBe "{\"applicationKey\":\"asdasd-sadas-ddas-qwe\"," +
            "\"correlationId\":\"1234567891033-avvik\",\"user\":\"Z999444\"," +
            "\"@timestamp\":\"2023-03-20T07:11:37.596+01:00\"," +
            "\"message\":\"Some sensitive message *************\",\"level\":\"INFO\"}\n"
    }

    @Test
    fun shouldMaskFNR() {
        val masker = SensitiveLogMasker()
        val maskedMessage = masker.maskLogMessage("Some message 12345678910 fnr")
        maskedMessage shouldBe "Some message *********** fnr"

        val maskedMessage2 = masker.maskLogMessage("Some message 12345678910 fnr 12345678910")
        maskedMessage2 shouldBe "Some message *********** fnr ***********"
    }

    @Test
    fun shouldNotMaskMessage() {
        val masker = SensitiveLogMasker()
        masker.maskLogMessage("Some message 123456789") shouldBe "Some message 123456789"
        masker.maskLogMessage("Some message 1234") shouldBe "Some message 1234"
        masker.maskLogMessage("Some message 1234") shouldBe "Some message 1234"
        masker.maskLogMessage("Some message 12343213123213123") shouldBe "Some message 12343213123213123"
        masker.maskLogMessage("") shouldBe ""
    }
}
