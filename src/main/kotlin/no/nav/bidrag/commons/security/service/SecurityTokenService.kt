package no.nav.bidrag.commons.security.service

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor

class SecurityTokenService(
    private val azureTokenService: AzureTokenService,
    private val stsTokenService: StsTokenService,
    private val oidcTokenManager: OidcTokenManager
) {
    companion object {
        const val HEADER_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }
    fun authTokenInterceptor(clientRegistrationId: String): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (oidcTokenManager.isValidTokenIssuedByAzure()){
                request.headers.setBearerAuth(azureTokenService.getAccessToken(clientRegistrationId, oidcTokenManager.fetchToken()).tokenValue)
            } else {
                request.headers.setBearerAuth(oidcTokenManager.fetchTokenAsString())
            }

            execution.execute(request, body!!)
        }
    }

    fun navConsumerTokenInterceptor(): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (!oidcTokenManager.isValidTokenIssuedByAzure()){
                request.headers.set(HEADER_NAV_CONSUMER_TOKEN, stsTokenService.generateToken())
            }

            execution.execute(request, body!!)
        }
    }

}