package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.CorrelationIdFilter.CORRELATION_ID_MDC
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
    val callId = MDC.get(CORRELATION_ID_MDC) ?: generateId()

    request.headers.add(CorrelationId.CORRELATION_ID_HEADER, callId)

    return execution.execute(request, body)
  }

  fun generateId(): String {
    val uuid = UUID.randomUUID()
    return java.lang.Long.toHexString(uuid.mostSignificantBits) +
        java.lang.Long.toHexString(uuid.leastSignificantBits)
  }
}
