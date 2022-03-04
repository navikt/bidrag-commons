package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.model.TokenException
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.slf4j.LoggerFactory


open class AzureTokenService(
  private val clientConfigurationProperties: ClientConfigurationProperties,
  private val azureTokenService: OAuth2AccessTokenService): TokenService("Azure") {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AzureTokenService::class.java)
    const val AZURE_CLAIM_OID = "oid"
    const val AZURE_CLAIM_SUB = "sub"
  }
  override fun isEnabled() = true

  override fun fetchToken(clientRegistrationId: String, token: JwtToken?): String {
    return getAccessToken(clientRegistrationId, token).accessToken
  }

  private fun getAccessToken(clientRegistrationId: String, token: JwtToken?): OAuth2AccessTokenResponse {
    if (token != null && isOnBehalfOfFlowToken(token)){
      LOGGER.debug("AZURE: Creating on-behalf-of token")
      return azureTokenService.getAccessToken(createClientPropertiesWithGrantType(clientRegistrationId, OAuth2GrantType.JWT_BEARER))
    }
    LOGGER.debug("AZURE: Creating client credentials token")
    return azureTokenService.getAccessToken(createClientPropertiesWithGrantType(clientRegistrationId, OAuth2GrantType.CLIENT_CREDENTIALS))
  }

  private fun isOnBehalfOfFlowToken(token: JwtToken): Boolean {
    val jwtTokenClaims: JwtTokenClaims = token.jwtTokenClaims
    return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) != jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID)
  }

  private fun createClientPropertiesWithGrantType(clientRegistrationId: String, grantType: OAuth2GrantType): ClientProperties{
    val registration = clientConfigurationProperties.registration[clientRegistrationId]
        ?: throw TokenException("Missing registration for client $clientRegistrationId")
    return ClientProperties(
      registration.tokenEndpointUrl,
      registration.wellKnownUrl,
      grantType,
      registration.scope,
      registration.authentication,
      registration.resourceUrl,
      registration.tokenExchange)
  }
}
