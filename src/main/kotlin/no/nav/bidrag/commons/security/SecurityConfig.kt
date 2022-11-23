package no.nav.bidrag.commons.security

import no.nav.bidrag.commons.security.service.AzureTokenService
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.service.TokenService
import no.nav.bidrag.commons.security.service.TokenXTokenService
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
class SecurityConfig {

    @Bean
    fun azureTokenService(
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ) = AzureTokenService(clientConfigurationProperties, oAuth2AccessTokenService)

    @Bean
    fun tokenxTokenService(
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ) = TokenXTokenService(clientConfigurationProperties, oAuth2AccessTokenService)

    @Bean
    fun oidcTokenManager(tokenValidationContextHolder: TokenValidationContextHolder) = OidcTokenManager()

    @Bean
    fun securityTokenService(azureTokenService: TokenService, stsTokenService: TokenService, tokenxTokenService: TokenService) =
        SecurityTokenService(azureTokenService, tokenxTokenService, stsTokenService)
}