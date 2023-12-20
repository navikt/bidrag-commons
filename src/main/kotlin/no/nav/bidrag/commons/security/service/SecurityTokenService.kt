package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.SikkerhetsKontekst.erIApplikasjonKontekst
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor

open class SecurityTokenService(
    private val azureTokenService: TokenService,
    private val tokenXTokenService: TokenService,
    private val stsTokenService: TokenService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val oidcTokenManager = OidcTokenManager()

    companion object {
        const val HEADER_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }

    open fun stsAuthTokenInterceptor(): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            logger.debug("serviceUserAuthTokenInterceptor: Adding STS token to auth header")
            request.headers.setBearerAuth(stsTokenService.fetchToken())
            execution.execute(request, body!!)
        }
    }

    @Deprecated("Bruk clientCredentialsTokenInterceptor")
    open fun serviceUserAuthTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return clientCredentialsTokenInterceptor(clientRegistrationId)
    }

    open fun clientCredentialsTokenInterceptor(clientRegistrationId: String? = null): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (azureTokenService.isEnabled() && clientRegistrationId != null) {
                logger.debug("serviceUserAuthTokenInterceptor: Adding Azure client credentials token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
            } else {
                logger.debug("serviceUserAuthTokenInterceptor: Adding STS token to auth header")
                request.headers.setBearerAuth(stsTokenService.fetchToken())
            }

            execution.execute(request, body!!)
        }
    }

    open fun authTokenInterceptor(clientRegistrationId: String): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (erIApplikasjonKontekst()) {
                logger.debug("authTokenInterceptor: Er i applikasjonkontekst, legger til client credentials token til auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
            } else if (oidcTokenManager.isValidTokenIssuedByAzure()) {
                logger.debug("authTokenInterceptor: Legger til Azure on-behalf-of token til auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, oidcTokenManager.fetchToken()))
            } else if (oidcTokenManager.isValidTokenIssuedByTokenX()) {
                logger.debug("authTokenInterceptor: Legger TokenX token til auth header")
                request.headers.setBearerAuth(tokenXTokenService.fetchToken(clientRegistrationId))
            } else if (oidcTokenManager.isValidTokenIssuedBySTS()) {
                logger.debug("authTokenInterceptor: Legger til Azure client credentials token til auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
            }

            execution.execute(request, body!!)
        }
    }

    @Deprecated("authTokenInterceptor må inkludere clientRegistrationId")
    open fun authTokenInterceptor(): ClientHttpRequestInterceptor? {
        return authTokenInterceptor(null, false)
    }

    @Deprecated("authTokenInterceptor må inkludere clientRegistrationId")
    open fun authTokenInterceptor(
        clientRegistrationId: String? = null,
        forwardIncomingSTSToken: Boolean = false,
    ): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            if (clientRegistrationId != null && oidcTokenManager.isValidTokenIssuedByAzure()) {
                logger.debug("authTokenInterceptor: Adding Azure on-behalf-of/client_credentials token to auth header")
                request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, oidcTokenManager.fetchToken()))
            } else if (clientRegistrationId != null && oidcTokenManager.isValidTokenIssuedByTokenX()) {
                logger.debug("authTokenInterceptor: Adding TokenX token")
                request.headers.setBearerAuth(tokenXTokenService.fetchToken(clientRegistrationId))
            } else if (oidcTokenManager.isValidTokenIssuedBySTS() && !forwardIncomingSTSToken) {
                if (clientRegistrationId != null) {
                    logger.debug("authTokenInterceptor: Adding Azure client credentials token")
                    request.headers.setBearerAuth(azureTokenService.fetchToken(clientRegistrationId, null))
                } else {
                    logger.debug("authTokenInterceptor: Adding application STS token")
                    request.headers.setBearerAuth(stsTokenService.fetchToken())
                }
            } else {
                logger.debug("authTokenInterceptor: Adding incoming token to auth header by issuer {}", oidcTokenManager.getIssuer())
                request.headers.setBearerAuth(oidcTokenManager.fetchTokenAsString())
            }

            execution.execute(request, body!!)
        }
    }

    @Deprecated("Dette var i bruk i tillfeller der konsumenten krevde ISSO token. Dette skal ikke lenger brukes")
    open fun navConsumerTokenInterceptor(ignoreWhenIncomingSTS: Boolean = false): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->

            logger.debug(
                "navConsumerTokenInterceptor:" +
                    "isValidTokenIssuedByOpenAm: ${oidcTokenManager.isValidTokenIssuedByOpenAm()} " +
                    "isValidTokenIssuedByTokenX: ${oidcTokenManager.isValidTokenIssuedByTokenX()} " +
                    "isValidTokenIssuedByAzure: ${oidcTokenManager.isValidTokenIssuedByAzure()}",
            )
            if (oidcTokenManager.isValidTokenIssuedByOpenAm() && !(ignoreWhenIncomingSTS && oidcTokenManager.isValidTokenIssuedBySTS())) {
                logger.debug("navConsumerTokenInterceptor: Adding STS token to Nav-Consumer-Token header")
                request.headers.set(HEADER_NAV_CONSUMER_TOKEN, "Bearer ${stsTokenService.fetchToken()}")
            } else {
                logger.debug(
                    "navConsumerTokenInterceptor: Not adding STS token to Nav-Consumer-Token header. " +
                        "ignoreWhenIncomingSTS=$ignoreWhenIncomingSTS " +
                        "and isValidTokenIssuedBySTS=${oidcTokenManager.isValidTokenIssuedBySTS()}",
                )
            }

            execution.execute(request, body!!)
        }
    }

    open fun navConsumerTokenInterceptor(): ClientHttpRequestInterceptor? {
        return navConsumerTokenInterceptor(false)
    }
}
