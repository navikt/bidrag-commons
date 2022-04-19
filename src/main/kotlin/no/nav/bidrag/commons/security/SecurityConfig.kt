package no.nav.bidrag.commons.security

import no.nav.bidrag.commons.security.service.AzureTokenService
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.service.StsTokenService
import no.nav.bidrag.commons.security.service.TokenService
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation
@EnableConfigurationProperties(StsConfigurationProperties::class)
@EnableOAuth2Client(cacheEnabled = true)
class SecurityConfig {

    @Bean
    fun azureTokenService(
        clientConfigurationProperties: ClientConfigurationProperties,
        azureTokenService: OAuth2AccessTokenService
    ) = AzureTokenService(clientConfigurationProperties, azureTokenService)

    @Bean
    fun oidcTokenManager(tokenValidationContextHolder: TokenValidationContextHolder) = OidcTokenManager(tokenValidationContextHolder)

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.sts.properties.url", havingValue = "")
    fun stsTokenService(
        restTemplateBuilder: RestTemplateBuilder,
        stsConfigurationProperties: StsConfigurationProperties
    ) = StsTokenService(restTemplateBuilder.rootUri(stsConfigurationProperties.properties?.url).basicAuthentication(stsConfigurationProperties.properties?.username, stsConfigurationProperties.properties?.password).build())


    @Bean("stsTokenService")
    @ConditionalOnMissingBean(StsTokenService::class)
    fun dummyStsTokenService() = TokenService("STS")

    @Bean
    fun securityTokenService(azureTokenService: TokenService, stsTokenService: TokenService, oidcTokenManager: OidcTokenManager) =
        SecurityTokenService(azureTokenService, stsTokenService, oidcTokenManager)
}