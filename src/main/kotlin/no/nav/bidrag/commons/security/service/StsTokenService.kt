package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.StsConfigurationProperties
import no.nav.bidrag.commons.security.model.TokenException
import no.nav.bidrag.commons.security.model.TokenForBasicAuthentication
import no.nav.security.token.support.client.core.OAuth2CacheFactory
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap

@EnableConfigurationProperties(StsConfigurationProperties::class)
@Service("stsTokenService")
class StsTokenService(stsConfigurationProperties: StsConfigurationProperties) : TokenService("STS") {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val stsCache = OAuth2CacheFactory.accessTokenResponseCache<String>(100, 300)

  private val restTemplate =
    RestTemplateBuilder().rootUri(stsConfigurationProperties.properties.url)
      .basicAuthentication(
        stsConfigurationProperties.properties.username,
        stsConfigurationProperties.properties.password
      ).build()

  companion object {
    private val PARAMETERS: LinkedMultiValueMap<String?, String?> =
      object : LinkedMultiValueMap<String?, String?>(2) {
        init {
          add("grant_type", "client_credentials")
          add("scope", "openid")
        }
      }
    const val REST_TOKEN_ENDPOINT = "/rest/v1/sts/token"
  }

  override fun isEnabled() = true

  override fun fetchToken(): String {
    return stsCache.get("STS", this::getToken)!!.accessToken

  }

  private fun getToken(cacheName: String): OAuth2AccessTokenResponse {
    logger.debug("Fetching STS token")
    val tokenForBasicAuthenticationResponse = restTemplate!!.exchange(
      "/",
      HttpMethod.POST,
      HttpEntity<Any>(PARAMETERS),
      TokenForBasicAuthentication::class.java
    )
    val tokenForBasicAuthentication = tokenForBasicAuthenticationResponse.body
    return tokenForBasicAuthentication?.let {
      OAuth2AccessTokenResponse.builder()
        .accessToken(it.access_token)
        .expiresIn(it.expiresIn)
        .build()
    } ?: throw TokenException(
      String.format(
        "Kunne ikke hente token fra '%s', response: %s",
        REST_TOKEN_ENDPOINT,
        tokenForBasicAuthenticationResponse.statusCode
      )
    )
  }
}
