package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.web.interceptor.StsBearerTokenClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(
    RestTemplateBuilderBean::class,
    StsBearerTokenClientInterceptor::class,
)
class RestOperationsSts {
    @Bean("sts")
    @Scope("prototype")
    fun restOperationsSts(
        restTemplateBuilder: RestTemplateBuilder,
        stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
    ) = restTemplateBuilder.additionalInterceptors(stsBearerTokenClientInterceptor).build()
}
