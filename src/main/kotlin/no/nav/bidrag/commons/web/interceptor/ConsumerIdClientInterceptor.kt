package no.nav.bidrag.commons.web.interceptor

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class ConsumerIdClientInterceptor(
  @Value("\${spring.application.name}") private val appName: String,
  @Value("\${credential.username:}") private val serviceUser: String
) : ClientHttpRequestInterceptor {


  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    request.headers.add(NAV_CONSUMER_ID, serviceUser.ifBlank { appName })
    return execution.execute(request, body)
  }

  companion object {
    const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
  }

}
