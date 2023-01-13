package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.CorrelationIdFilter
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
    val correlationId = MDC.get(CorrelationId.CORRELATION_ID_HEADER) ?: generateId()
    request.headers.add(NAV_CALL_ID, callId)
    request.headers.add(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
    return execution.execute(request, body)
  }

  fun generateId(): String {
    val uuid = UUID.randomUUID()
    return java.lang.Long.toHexString(uuid.mostSignificantBits) +
        java.lang.Long.toHexString(uuid.leastSignificantBits)
  }

  companion object {
    const val NAV_CALL_ID = "Nav-Call-Id"
  }
}
