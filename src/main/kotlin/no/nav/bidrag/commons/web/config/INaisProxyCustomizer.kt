package no.nav.bidrag.commons.web.config

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

interface INaisProxyCustomizer : RestTemplateCustomizer

@Component
class NaisProxyCustomizer(
    @Value("\${bidrag.nais.proxy.requestTimeout:15000}") val requestTimeout: Long
) : INaisProxyCustomizer {

    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val requestTimeout = Timeout.ofMilliseconds(requestTimeout)

        val client = HttpClients.custom().setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectionRequestTimeout(requestTimeout)
                .build()
        ).setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {

            public override fun determineProxy(
                target: HttpHost,
                context: HttpContext
            ): HttpHost? {
                return if (target.hostName.contains("microsoft")) {
                    super.determineProxy(target, context)
                } else {
                    null
                }
            }
        }).build()

        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }
}
