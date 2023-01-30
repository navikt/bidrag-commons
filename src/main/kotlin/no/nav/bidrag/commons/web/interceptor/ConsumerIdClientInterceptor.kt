package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.web.BidragHttpHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class ConsumerIdClientInterceptor(
  @Value("\${NAIS_APP_NAME}") private val appName: String,
  @Value("\${credential.username:}") private val serviceUser: String
) : ClientHttpRequestInterceptor {


  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    request.headers.add(BidragHttpHeaders.NAV_CONSUMER_ID, serviceUser.ifBlank { appName })
    return execution.execute(request, body)
  }

}
