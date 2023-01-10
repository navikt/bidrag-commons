package no.nav.bidrag.commons.web

import no.nav.bidrag.commons.web.DefaultCorsFilter.defaultCorsConfiguration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

object DefaultCorsFilter : CorsFilter(defaultCorsConfiguration) {

  val defaultCorsConfiguration: UrlBasedCorsConfigurationSource
    get() {
      val corsConfiguration = CorsConfiguration().apply {
        this.allowedOrigins = listOf("*")
        this.allowedHeaders = listOf(
          "Content-Type",
          "Authorization",
          "Content-Length",
          "X-Requested-With",
          "X-Correlation-ID",
          "X-Enhet",
          "Nav-Call-Id",
          "Nav-Consumer-Id"
        )
        this.exposedHeaders = listOf("Warning", "X-Enhet", "X-Correlation-Id")
        this.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
      }
      return UrlBasedCorsConfigurationSource().apply {
        this.registerCorsConfiguration("/**", corsConfiguration)
      }
    }
}