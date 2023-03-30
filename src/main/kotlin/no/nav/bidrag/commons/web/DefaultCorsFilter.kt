package no.nav.bidrag.commons.web

import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Component
class DefaultCorsFilter : CorsFilter(
    UrlBasedCorsConfigurationSource().apply {
        this.registerCorsConfiguration(
            "/**",
            CorsConfiguration().apply {
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
        )
    }
)
