package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.web.interceptor.ConsumerIdClientInterceptor
import no.nav.bidrag.commons.web.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.temporal.ChronoUnit

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(ConsumerIdClientInterceptor::class, NaisProxyCustomizer::class)
class RestTemplateBuilderBean {

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.aad.proxy_url")
    fun restTemplateBuilder(
        iNaisProxyCustomizer: INaisProxyCustomizer,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        metricsRestTemplateCustomizer: MetricsRestTemplateCustomizer
    ) = RestTemplateBuilder()
        .additionalInterceptors(consumerIdClientInterceptor, MdcValuesPropagatingClientInterceptor())
        .additionalCustomizers(iNaisProxyCustomizer, metricsRestTemplateCustomizer)
        .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
        .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))

    /**
     * Denne bønnnen initialiseres hvis proxy-url ikke finnes. Hvis proxy-url finnnes vil bønnen over
     * initialiseres og denne initialiseres ikke med mindre proxyen har verdien "umulig verdi",
     * som den aldri skal ha.
     */
    @Bean
    @ConditionalOnProperty(
        "no.nav.security.jwt.issuer.aad.proxy_url",
        matchIfMissing = true,
        havingValue = "Umulig verdi"
    )
    fun restTemplateBuilderNoProxy(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        metricsRestTemplateCustomizer: MetricsRestTemplateCustomizer
    ): RestTemplateBuilder = RestTemplateBuilder()
        .additionalCustomizers(metricsRestTemplateCustomizer)
        .additionalInterceptors(consumerIdClientInterceptor, MdcValuesPropagatingClientInterceptor())
        .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
        .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))
}
