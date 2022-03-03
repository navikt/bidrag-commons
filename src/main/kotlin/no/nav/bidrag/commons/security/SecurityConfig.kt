package no.nav.bidrag.commons.security

import no.nav.bidrag.commons.security.azure.OnBehalfOfTokenResponseClient
import no.nav.bidrag.commons.security.service.AzureTokenService
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.service.StsTokenService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

@Configuration
@EnableJwtTokenValidation
open class SecurityConfig {

    @Bean
    open fun disabledAzureTokenService() = AzureTokenService(null, null, null)
    @Bean
    open fun disabledStsTokenService() = StsTokenService(null)

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.aad.discoveryurl", havingValue = "")
    open fun onBehalfOfTokenResponseClient(restTemplateBuilder: RestTemplateBuilder, environment: Environment?) =
        OnBehalfOfTokenResponseClient(restTemplateBuilder, environment)

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.aad.discoveryurl", havingValue = "")
    open fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository?,
        authorizedClientService: OAuth2AuthorizedClientService?
    ): OAuth2AuthorizedClientManager? {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()
        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService
        )
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.aad.discoveryurl", havingValue = "")
    open fun azureTokenService(
        authorizedClientManager: OAuth2AuthorizedClientManager,
        onBehalfOfTokenResponseClient: OnBehalfOfTokenResponseClient,
        clientRegistrationRepository: ClientRegistrationRepository
    ) = AzureTokenService(authorizedClientManager, onBehalfOfTokenResponseClient, clientRegistrationRepository)

    @Bean
    open fun oidcTokenManager(tokenValidationContextHolder: TokenValidationContextHolder) = OidcTokenManager(tokenValidationContextHolder)

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.sts.discoveryurl", havingValue = "")
    open fun stsTokenService(
        restTemplateBuilder: RestTemplateBuilder,
        @Value("\${no.nav.bidrag.commons.security.sts.url}") url: String,
        @Value("\${no.nav.bidrag.commons.security.sts.serviceuser.username}") username: String,
        @Value("\${no.nav.bidrag.commons.security.sts.serviceuser.password}") password: String,
    ) = StsTokenService(restTemplateBuilder.rootUri(url).basicAuthentication(username, password).build())

    @Bean
    open fun securityTokenService(azureTokenService: AzureTokenService, stsTokenService: StsTokenService, oidcTokenManager: OidcTokenManager) =
        SecurityTokenService(azureTokenService, stsTokenService, oidcTokenManager)


}