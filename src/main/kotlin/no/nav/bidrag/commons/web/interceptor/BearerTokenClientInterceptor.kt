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

abstract class AzureTokenClientInterceptor(
  private val oAuth2AccessTokenService: OAuth2AccessTokenService,
  private val clientConfigurationProperties: ClientConfigurationProperties
) :
  ClientHttpRequestInterceptor {

  protected fun genererAccessToken(
    request: HttpRequest,
    oAuth2GrantType: OAuth2GrantType? = null
  ): String {
    val clientProperties = clientPropertiesFor(
      request.uri,
      clientConfigurationProperties,
      oAuth2GrantType
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
    oAuth2GrantType: OAuth2GrantType? = null
  ): ClientProperties {
    val clientProperties = filterClientProperties(clientConfigurationProperties, uri)

    return ClientProperties(
      clientProperties.tokenEndpointUrl,
      clientProperties.wellKnownUrl,
      oAuth2GrantType ?: clientCredentialOrJwtBearer(),
      clientProperties.scope,
      clientProperties.authentication,
      clientProperties.resourceUrl,
      clientProperties.tokenExchange
    )
  }

  private fun filterClientProperties(
    clientConfigurationProperties: ClientConfigurationProperties,
    uri: URI
  ) = clientConfigurationProperties
    .registration
    .values
    .firstOrNull { uri.toString().startsWith(it.resourceUrl.toString()) }
    ?: error("could not find oauth2 client config for uri=$uri")

  private fun clientCredentialOrJwtBearer() =
    if (erSystembruker()) OAuth2GrantType.CLIENT_CREDENTIALS else OAuth2GrantType.JWT_BEARER

  private fun erSystembruker(): Boolean {
    return try {
      val preferredUsername =
        SpringTokenValidationContextHolder()
          .tokenValidationContext
          .getClaims("aad")["preferred_username"]
      return preferredUsername == null
    } catch (e: Throwable) {
      // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
      true
    }
  }
}

@Component
class BearerTokenClientInterceptor(
  oAuth2AccessTokenService: OAuth2AccessTokenService,
  clientConfigurationProperties: ClientConfigurationProperties
) : AzureTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {

  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    request.headers.setBearerAuth(genererAccessToken(request))
    return execution.execute(request, body)
  }
}

@Component
class ServiceUserAuthTokenInterceptor(
  oAuth2AccessTokenService: OAuth2AccessTokenService,
  clientConfigurationProperties: ClientConfigurationProperties
) : AzureTokenClientInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {

  override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
    request.headers.setBearerAuth(genererAccessToken(request, OAuth2GrantType.CLIENT_CREDENTIALS))
    return execution.execute(request, body)
  }
}
