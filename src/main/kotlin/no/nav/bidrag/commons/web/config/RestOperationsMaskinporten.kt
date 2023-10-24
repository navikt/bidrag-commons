package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.web.interceptor.MaskinportenBearerTokenClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope

@Configuration
@Import(RestTemplateBuilderBean::class,
    MaskinportenBearerTokenClientInterceptor::class)
class RestOperationsMaskinporten {

    @Bean("maskinporten")
    @Scope("prototype")
    fun restOperationsMaskinporten(
        restTemplateBuilder: RestTemplateBuilder,
        maskinportenBearerTokenClientInterceptor: MaskinportenBearerTokenClientInterceptor
    ) = restTemplateBuilder.additionalInterceptors(maskinportenBearerTokenClientInterceptor).build()
}