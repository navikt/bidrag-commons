package no.nav.bidrag.commons.security.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor

open class SecurityTokenService(
    private val azureTokenService: TokenService,
    private val tokenXTokenService: TokenService,
    private val stsTokenService: TokenService
) {

    private val oidcTokenManager = OidcTokenManager()
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SecurityTokenService::class.java)
        const val HEADER_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }

    open fun serviceUserAuthTokenInterceptor(): ClientHttpRequestInterceptor? {
        return serviceUserAuthTokenInterceptor(null)
    }

    open fun serviceUserAuthTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (azureTokenService.isEnabled() && clientRegistrationId != null){
                LOGGER.debug("serviceUserAuthTokenInterceptor: Adding Azure client credentials token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
            } else {
                LOGGER.debug("serviceUserAuthTokenInterceptor: Adding STS token to auth header")
                request.headers.setBearerAuth(stsTokenService.fetchToken())
            }

            execution.execute(request, body!!)
        }
    }

    open fun authTokenInterceptor(): ClientHttpRequestInterceptor? {
        return authTokenInterceptor(null, false)
    }

    open fun authTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return authTokenInterceptor(clientRegistrationId, false)
    }

    open fun authTokenInterceptor(clientRegistrationId: String? = null, forwardIncomingSTSToken: Boolean = false): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (clientRegistrationId != null && oidcTokenManager.isValidTokenIssuedByAzure()){
                LOGGER.debug("authTokenInterceptor: Adding Azure on-behalf-of/client_credentials token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, oidcTokenManager.fetchToken()))
            } else if (clientRegistrationId != null && oidcTokenManager.isValidTokenIssuedByTokenX()) {
                LOGGER.debug("authTokenInterceptor: Adding TokenX token")
                request.headers.setBearerAuth(tokenXTokenService.fetchToken(clientRegistrationId))
            } else if (oidcTokenManager.isValidTokenIssuedBySTS() && !forwardIncomingSTSToken) {
                if (clientRegistrationId != null){
                    LOGGER.debug("authTokenInterceptor: Adding Azure client credentials token")
                    request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
                } else {
                    LOGGER.debug("authTokenInterceptor: Adding application STS token")
                    request.headers.setBearerAuth(stsTokenService.fetchToken())
                }
            } else {
                LOGGER.debug("authTokenInterceptor: Adding incoming token to auth header by issuer {}", oidcTokenManager.getIssuer())
                request.headers.setBearerAuth(oidcTokenManager.fetchTokenAsString())
            }

            execution.execute(request, body!!)
        }
    }

    open fun navConsumerTokenInterceptor(ignoreWhenIncomingSTS: Boolean = false): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->

            LOGGER.debug("navConsumerTokenInterceptor:" +
                    "isValidTokenIssuedByOpenAm: ${oidcTokenManager.isValidTokenIssuedByOpenAm()} " +
                    "isValidTokenIssuedByTokenX: ${oidcTokenManager.isValidTokenIssuedByTokenX()} " +
                    "isValidTokenIssuedByAzure: ${oidcTokenManager.isValidTokenIssuedByAzure()}")
            if (oidcTokenManager.isValidTokenIssuedByOpenAm() && !(ignoreWhenIncomingSTS && oidcTokenManager.isValidTokenIssuedBySTS())){
                LOGGER.debug("navConsumerTokenInterceptor: Adding STS token to Nav-Consumer-Token header")
                request.headers.set(HEADER_NAV_CONSUMER_TOKEN, "Bearer ${stsTokenService.fetchToken()}")
            } else {
                LOGGER.debug("navConsumerTokenInterceptor: Not adding STS token to Nav-Consumer-Token header. ignoreWhenIncomingSTS=$ignoreWhenIncomingSTS and isValidTokenIssuedBySTS=${oidcTokenManager.isValidTokenIssuedBySTS()}")
            }

            execution.execute(request, body!!)
        }
    }

    open fun navConsumerTokenInterceptor(): ClientHttpRequestInterceptor? {
        return navConsumerTokenInterceptor(false)
    }

}