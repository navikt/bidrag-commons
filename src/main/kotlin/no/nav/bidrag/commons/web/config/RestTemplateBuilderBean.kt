package no.nav.bidrag.commons.web.config

import io.micrometer.observation.ObservationRegistry
import no.nav.bidrag.commons.web.interceptor.ConsumerIdClientInterceptor
import no.nav.bidrag.commons.web.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import java.time.Duration
import java.time.temporal.ChronoUnit

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(ConsumerIdClientInterceptor::class, MdcValuesPropagatingClientInterceptor::class)
class RestTemplateBuilderBean {

    @Bean
    fun restTemplateBuilderNoProxy(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        observationRestTemplateCustomizer: ObservationRestTemplateCustomizer,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor
    ): RestTemplateBuilder = RestTemplateBuilder()
        .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
        .additionalCustomizers(observationRestTemplateCustomizer)
        .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
        .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))

    @Bean
    fun metricsRestTemplateCustomizer(): ObservationRestTemplateCustomizer {
        return ObservationRestTemplateCustomizer(ObservationRegistry.create(), DefaultClientRequestObservationConvention())
    }
}
