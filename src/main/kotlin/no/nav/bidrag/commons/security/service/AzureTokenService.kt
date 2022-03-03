package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.azure.OAuth2JwtBearerGrantRequest
import no.nav.bidrag.commons.security.azure.OnBehalfOfTokenResponseClient
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken


open class AzureTokenService(
  private val authorizedClientManager: OAuth2AuthorizedClientManager?,
  private val onBehalfOfTokenResponseClient: OnBehalfOfTokenResponseClient?,
  private val clientRegistrationRepository: ClientRegistrationRepository?): TokenService() {

  companion object {
    const val AZURE_CLAIM_OID = "oid"
    const val AZURE_CLAIM_SUB = "sub"
  }
  private val ANONYMOUS_AUTHENTICATION: Authentication = AnonymousAuthenticationToken(
    "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
  )

  open fun getAccessToken(clientRegistrationId: String, token: JwtToken): OAuth2AccessToken {
    if (isOnBehalfOfFlowToken(token)){
      val clientRegistration: ClientRegistration = clientRegistrationRepository!!.findByRegistrationId(clientRegistrationId)
      return onBehalfOfTokenResponseClient!!.getTokenResponse(OAuth2JwtBearerGrantRequest(clientRegistration, token.tokenAsString)).accessToken
    }
    return authorizedClientManager!!
      .authorize(
        OAuth2AuthorizeRequest
          .withClientRegistrationId(clientRegistrationId)
          .principal(ANONYMOUS_AUTHENTICATION)
          .build()
      )!!.accessToken
  }

  protected open fun isOnBehalfOfFlowToken(token: JwtToken): Boolean {
    val jwtTokenClaims: JwtTokenClaims = token.getJwtTokenClaims()
    return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) != jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID)
  }

  protected open fun isClientCredentialFlowToken(token: JwtToken): Boolean {
    val jwtTokenClaims: JwtTokenClaims = token.getJwtTokenClaims()
    return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) == jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID)
  }
}
