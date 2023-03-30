package no.nav.bidrag.commons

class CorrelationId private constructor(private val idValue: String) {
    init {
        CORRELATION_ID_VALUE.set(idValue)
    }

    fun get(): String {
        return idValue
    }

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        private val CORRELATION_ID_VALUE = ThreadLocal<String>()
        fun fetchCorrelationIdForThread(): String {
            return CORRELATION_ID_VALUE.get() ?: generateTimestamped("UNKNOWN").get()
        }

        fun existing(value: String?): CorrelationId {
            return if (value.isNullOrBlank()) {
                generateTimestamped("correlationId")
            } else {
                CorrelationId(value)
            }
        }

        fun generateTimestamped(value: String): CorrelationId {
            val currentTimeAsString = java.lang.Long.toHexString(System.currentTimeMillis())
            return CorrelationId("$currentTimeAsString-$value")
        }
    }
}
