package no.nav.bidrag.commons.security

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.security.azure.OnBehalfOfTokenResponseClient
import no.nav.bidrag.commons.security.service.AzureTokenService
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.service.StsTokenService
import no.nav.bidrag.commons.security.service.TokenService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.util.concurrent.TimeUnit

@Configuration
@EnableJwtTokenValidation
@EnableCaching
class SecurityConfig {

    companion object {
        const val STS_SERVICE_USER_TOKEN_CACHE = "STS_SERVICE_USER_TOKEN_CACHE"
        const val AZURE_AD_TOKEN_CACHE = "AZURE_AD_TOKEN_CACHE"
    }

    @Bean
    @ConditionalOnProperty("no.nav.bidrag.commons.security.aad.enabled", havingValue = "true")
    fun onBehalfOfTokenResponseClient(restTemplateBuilder: RestTemplateBuilder, environment: Environment?) =
        OnBehalfOfTokenResponseClient(restTemplateBuilder, environment)

    @Bean
    @ConditionalOnProperty("no.nav.bidrag.commons.security.aad.enabled", havingValue = "true")
    fun authorizedClientManager(
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
    @ConditionalOnProperty("no.nav.bidrag.commons.security.aad.enabled", havingValue = "true")
    fun azureTokenService(
        authorizedClientManager: OAuth2AuthorizedClientManager,
        onBehalfOfTokenResponseClient: OnBehalfOfTokenResponseClient,
        clientRegistrationRepository: ClientRegistrationRepository
    ) = AzureTokenService(authorizedClientManager, onBehalfOfTokenResponseClient, clientRegistrationRepository)

    @Bean
    fun oidcTokenManager(tokenValidationContextHolder: TokenValidationContextHolder) = OidcTokenManager(tokenValidationContextHolder)

    @Bean
    @ConditionalOnProperty("no.nav.bidrag.commons.security.sts.url", havingValue = "")
    fun stsTokenService(
        restTemplateBuilder: RestTemplateBuilder,
        @Value("\${no.nav.bidrag.commons.security.sts.url}") url: String,
        @Value("\${no.nav.bidrag.commons.security.sts.serviceuser.username}") username: String,
        @Value("\${no.nav.bidrag.commons.security.sts.serviceuser.password}") password: String,
    ) = StsTokenService(restTemplateBuilder.rootUri(url).basicAuthentication(username, password).build())


    @Bean("azureTokenService")
    @ConditionalOnProperty("no.nav.bidrag.commons.security.aad.enabled", matchIfMissing = true, havingValue = "false")
    fun dummyAzureTokenService() = TokenService("AZURE")
    @Bean("stsTokenService")
    @ConditionalOnProperty("no.nav.bidrag.commons.security.sts.url", matchIfMissing = true, havingValue = "false")
    fun dummyStsTokenService() = TokenService("STS")

    @Bean
    fun securityTokenService(azureTokenService: TokenService, stsTokenService: TokenService, oidcTokenManager: OidcTokenManager) =
        SecurityTokenService(azureTokenService, stsTokenService, oidcTokenManager)

    @Bean
    fun securityTokenCacheManager(): CacheManager? {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            STS_SERVICE_USER_TOKEN_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(50, TimeUnit.MINUTES)
                .recordStats()
                .build()
        )
        caffeineCacheManager.registerCustomCache(
            AZURE_AD_TOKEN_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(50, TimeUnit.MINUTES)
                .recordStats()
                .build()
        )
        return caffeineCacheManager
    }
}