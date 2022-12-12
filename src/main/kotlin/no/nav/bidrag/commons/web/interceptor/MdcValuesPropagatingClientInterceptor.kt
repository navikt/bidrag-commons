package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.EnhetFilter
import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.util.*

@Component
class MdcValuesPropagatingClientInterceptor : ClientHttpRequestInterceptor {

  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    val callId = MDC.get(NAV_CALL_ID) ?: generateId()
    request.headers.add(NAV_CALL_ID, callId)
    request.headers.add(CORRELATION_ID, CorrelationIdFilter.fetchCorrelationIdForThread())
    request.headers.add(ENHET, EnhetFilter.fetchForThread())
    return execution.execute(request, body)
  }

  fun generateId(): String {
    val uuid = UUID.randomUUID()
    return java.lang.Long.toHexString(uuid.mostSignificantBits) +
        java.lang.Long.toHexString(uuid.leastSignificantBits)
  }

  companion object {
    const val NAV_CALL_ID = "Nav-Call-Id"
    const val ENHET = "X-Enhet"
    const val CORRELATION_ID = "X-Correlation-ID"
  }
}
