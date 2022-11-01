package no.nav.bidrag.commons.web.interceptor

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI

@Component
class BearerTokenClientInterceptor(
  private val oAuth2AccessTokenService: OAuth2AccessTokenService,
  private val clientConfigurationProperties: ClientConfigurationProperties
) :
  ClientHttpRequestInterceptor {

  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    request.headers.setBearerAuth(
      genererAccessToken(
        request,
        clientConfigurationProperties,
        oAuth2AccessTokenService
      )
    )
    return execution.execute(request, body)
  }

  private fun genererAccessToken(
    request: HttpRequest,
    clientConfigurationProperties: ClientConfigurationProperties,
    oAuth2AccessTokenService: OAuth2AccessTokenService,
  ): String {
    val clientProperties = clientPropertiesFor(
      request.uri,
      clientConfigurationProperties,
    )
    return oAuth2AccessTokenService.getAccessToken(clientProperties).accessToken
  }

  /**
   * Finds client property for grantType:
   *  - Returns first client property, if there is only one
   *  - Returns client property for client_credentials or jwt_bearer
   */
  private fun clientPropertiesFor(
    uri: URI,
    clientConfigurationProperties: ClientConfigurationProperties,
  ): ClientProperties {
    val clientProperties = filterClientProperties(clientConfigurationProperties, uri)
    return if (clientProperties.size == 1) {
      clientProperties.first()
    } else {
      clientPropertiesForGrantType(clientProperties, clientCredentialOrJwtBearer(), uri)
    }
  }


  private fun filterClientProperties(
    clientConfigurationProperties: ClientConfigurationProperties,
    uri: URI
  ) = clientConfigurationProperties
    .registration
    .values
    .filter { uri.toString().startsWith(it.resourceUrl.toString()) }

  private fun clientPropertiesForGrantType(
    values: List<ClientProperties>,
    grantType: OAuth2GrantType,
    uri: URI
  ): ClientProperties {
    return values.firstOrNull { grantType == it.grantType }
      ?: error("could not find oauth2 client config for uri=$uri and grant type=$grantType")
  }

  private fun clientCredentialOrJwtBearer() =
    if (erSystembruker()) OAuth2GrantType.CLIENT_CREDENTIALS else OAuth2GrantType.JWT_BEARER

  private fun erSystembruker(): Boolean {
    return try {
      val preferredUsername =
        SpringTokenValidationContextHolder()
          .tokenValidationContext
          .getClaims("azuread")["preferred_username"]
      return preferredUsername == null
    } catch (e: Throwable) {
      // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
      true
    }
  }

}
