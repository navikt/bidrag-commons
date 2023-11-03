package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.security.maskinporten.MaskinportenClient
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
@Import(MaskinportenClient::class)
class MaskinportenBearerTokenClientInterceptor(private val maskinportenClient: MaskinportenClient) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        request.headers.setBearerAuth(maskinportenClient.hentMaskinportenToken().parsedString)
        request.headers.accept = listOf(MediaType.APPLICATION_JSON)
        request.headers.contentType = MediaType.APPLICATION_JSON
        return execution.execute(request, body)
    }
}
