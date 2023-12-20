package no.nav.bidrag.commons.logging

import com.fasterxml.jackson.core.JsonStreamContext
import net.logstash.logback.mask.ValueMasker
import java.util.regex.Matcher
import java.util.regex.Pattern

class SensitiveLogMasker : ValueMasker {
    companion object {
        private val FNR_PATTERN: Pattern = "(?<!\\bcorrelationId\":\")(\\b\\d{11}\\b)(?!\\d)".toPattern()
        private val AKTOER_PATTERN: Pattern = "(?<!\\bcorrelationId\":\")(\\b\\d{13}\\b)(?!\\d)".toPattern()
    }

    override fun mask(
        p0: JsonStreamContext?,
        p1: Any?,
    ): Any? {
        return (
            if (p1 is CharSequence) {
                maskLogMessage(p1)
            } else {
                p1
            }
        )
    }

    fun maskLogMessage(logMessage: CharSequence?): String {
        val sb = StringBuilder(logMessage)
        maskAll(sb, AKTOER_PATTERN)
        maskAll(sb, FNR_PATTERN)
        return sb.toString()
    }

    private fun maskAll(
        sb: StringBuilder,
        pattern: Pattern,
    ) {
        val matcher: Matcher = pattern.matcher(sb)
        while (matcher.find()) {
            mask(sb, matcher.start(), matcher.end())
            matcher.start()
        }
    }

    private fun mask(
        sb: StringBuilder,
        start: Int,
        end: Int,
    ) {
        for (i in start until end) {
            sb.setCharAt(i, '*')
        }
    }
}
