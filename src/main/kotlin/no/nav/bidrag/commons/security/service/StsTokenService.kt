package no.nav.bidrag.commons.security.service

import org.springframework.web.client.RestTemplate
import no.nav.bidrag.commons.security.model.TokenForBasicAuthentication
import org.springframework.http.HttpMethod
import no.nav.bidrag.commons.security.model.TokenException
import org.springframework.http.HttpEntity
import org.springframework.util.LinkedMultiValueMap
import java.util.Optional

class StsTokenService(private val restTemplate: RestTemplate?) {
    companion object {
        private val PARAMETERS: LinkedMultiValueMap<String?, String?> = object : LinkedMultiValueMap<String?, String?>(2) {
            init {
                add("grant_type", "client_credentials")
                add("scope", "openid")
            }
        }
        const val REST_TOKEN_ENDPOINT = "/rest/v1/sts/token"
    }

    fun generateToken(): String {
        val tokenForBasicAuthenticationResponse = restTemplate!!.exchange("/", HttpMethod.POST, HttpEntity<Any>(PARAMETERS), TokenForBasicAuthentication::class.java)
        val tokenForBasicAuthentication = tokenForBasicAuthenticationResponse.body
        return Optional.ofNullable(tokenForBasicAuthentication)
            .map { obj: TokenForBasicAuthentication -> obj.fetchToken() }
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