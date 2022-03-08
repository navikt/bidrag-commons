package no.nav.bidrag.commons.security.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor

open class SecurityTokenService(
    private val azureTokenService: TokenService,
    private val stsTokenService: TokenService,
    private val oidcTokenManager: OidcTokenManager
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SecurityTokenService::class.java)
        const val HEADER_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }

    open fun serviceUserAuthTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (azureTokenService.isEnabled() && clientRegistrationId != null){
                LOGGER.debug("Adding Azure client credentials token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
            } else {
                LOGGER.debug("Adding STS token to auth header")
                request.headers.setBearerAuth(stsTokenService.fetchToken())
            }

            execution.execute(request, body!!)
        }
    }

    open fun authTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (clientRegistrationId != null && oidcTokenManager.isValidTokenIssuedByAzure()){
                LOGGER.debug("Adding Azure on-behalf-of token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, oidcTokenManager.fetchToken()))
            } else {
                LOGGER.debug("Adding incoming token to auth header")
                request.headers.setBearerAuth(oidcTokenManager.fetchTokenAsString())
            }

            execution.execute(request, body!!)
        }
    }

    open fun navConsumerTokenInterceptor(): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (!oidcTokenManager.isValidTokenIssuedByAzure()){
                LOGGER.debug("Adding STS token to Nav-Consumer-Token header")
                request.headers.set(HEADER_NAV_CONSUMER_TOKEN, "Bearer ${stsTokenService.fetchToken()}")
            }

            execution.execute(request, body!!)
        }
    }

}