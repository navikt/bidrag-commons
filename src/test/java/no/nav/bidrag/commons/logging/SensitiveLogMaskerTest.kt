package no.nav.bidrag.commons.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SensitiveLogMaskerTest {

    @Test
    fun shouldMaskAktoerId(){
        val masker = SensitiveLogMasker()
        val maskedMessage = masker.maskLogMessage("Some message 1234567891033 aktoerid")
        assertThat(maskedMessage).isEqualTo("Some message *********1033 aktoerid")
    }

    @Test
    fun shouldMaskFNR(){
        val masker = SensitiveLogMasker()
        val maskedMessage = masker.maskLogMessage("Some message 12345678910 fnr")
        assertThat(maskedMessage).isEqualTo("Some message *******8910 fnr")
    }

    @Test
    fun shouldNotMaskMessage(){
        val masker = SensitiveLogMasker()
        assertThat(masker.maskLogMessage("Some message 123456789")).isEqualTo("Some message 123456789")
        assertThat(masker.maskLogMessage("Some message 1234")).isEqualTo("Some message 1234")
        assertThat(masker.maskLogMessage("Some message 1234")).isEqualTo("Some message 1234")
        assertThat(masker.maskLogMessage("Some message 12343213123213123")).isEqualTo("Some message 12343213123213123")
        assertThat(masker.maskLogMessage("")).isEqualTo("")
    }

}