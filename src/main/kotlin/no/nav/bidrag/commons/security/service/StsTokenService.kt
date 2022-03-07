package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.model.TokenException
import no.nav.bidrag.commons.security.model.TokenForBasicAuthentication
import no.nav.security.token.support.client.core.OAuth2CacheFactory
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.Optional

open class StsTokenService(private val restTemplate: RestTemplate?): TokenService("STS") {
    private var stsCache = OAuth2CacheFactory.accessTokenResponseCache<String>(1000, 100)
    companion object {
        private val LOGGER = LoggerFactory.getLogger(StsTokenService::class.java)
        private val PARAMETERS: LinkedMultiValueMap<String?, String?> = object : LinkedMultiValueMap<String?, String?>(2) {
            init {
                add("grant_type", "client_credentials")
                add("scope", "openid")
            }
        }
        const val REST_TOKEN_ENDPOINT = "/rest/v1/sts/token"
    }

    override fun isEnabled() = true

    override fun fetchToken(): String {
        return stsCache.get("sts", this::getToken)!!.accessToken

    }

    private fun getToken(cacheName: String): OAuth2AccessTokenResponse {
        LOGGER.debug("Fetching STS token")
        val tokenForBasicAuthenticationResponse = restTemplate!!.exchange("/", HttpMethod.POST, HttpEntity<Any>(PARAMETERS), TokenForBasicAuthentication::class.java)
        val tokenForBasicAuthentication = tokenForBasicAuthenticationResponse.body
        return Optional.ofNullable(tokenForBasicAuthentication)
            .map { obj: TokenForBasicAuthentication -> OAuth2AccessTokenResponse.builder().accessToken(obj.access_token).build() }
            .orElseThrow {
                TokenException(
                    String.format(
                        "Kunne ikke hente token fra '%s', response: %s",
                        REST_TOKEN_ENDPOINT,
                        tokenForBasicAuthenticationResponse.statusCode
                    )
                )
            }
    }
}