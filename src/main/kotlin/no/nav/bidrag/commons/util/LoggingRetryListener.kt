package no.nav.bidrag.commons.util

import org.slf4j.LoggerFactory
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener

private val log = LoggerFactory.getLogger(LoggingRetryListener::class.java)

class LoggingRetryListener(val details: String? = null) : RetryListener {
    override fun <T : Any?, E : Throwable?> onError(
        context: RetryContext?,
        callback: RetryCallback<T, E>?,
        throwable: Throwable?,
    ) {
        super.onError(context, callback, throwable)
        val details = if (!details.isNullOrEmpty()) " med detaljer: $details" else ""
        log.warn("Det skjedde en feil i retryTemplate$details. Dette er ${context?.retryCount}. forsøk.", throwable)
    }
}
