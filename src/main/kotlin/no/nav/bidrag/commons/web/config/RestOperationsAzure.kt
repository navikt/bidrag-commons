package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.web.interceptor.BearerTokenClientInterceptor
import no.nav.bidrag.commons.web.interceptor.ServiceUserAuthTokenInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(RestTemplateBuilderBean::class, BearerTokenClientInterceptor::class, ServiceUserAuthTokenInterceptor::class)
class RestOperationsAzure {

  @Bean("azure")
  fun restOperationsJwtBearer(
    restTemplateBuilder: RestTemplateBuilder,
    bearerTokenClientInterceptor: BearerTokenClientInterceptor
  ) = restTemplateBuilder.additionalInterceptors(bearerTokenClientInterceptor).build()

  @Bean("azureService")
  fun restOperationsServiceJwtBearer(
    restTemplateBuilder: RestTemplateBuilder,
    bearerTokenClientInterceptor: ServiceUserAuthTokenInterceptor
  ) = restTemplateBuilder.additionalInterceptors(bearerTokenClientInterceptor).build()
}
