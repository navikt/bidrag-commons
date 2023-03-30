package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.security.service.StsTokenService
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
@Import(StsTokenService::class)
class StsBearerTokenClientInterceptor(private val stsRestClient: StsTokenService) :
    ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val systembrukerToken = stsRestClient.fetchToken()
        request.headers.setBearerAuth(systembrukerToken)
        return execution.execute(request, body)
    }
}
