package no.nav.bidrag.commons.security.azure

import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import kotlin.Throws
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext


internal class ProxyCustomizer : RestTemplateCustomizer {
    companion object {
        const val PROXY_SERVER_HOST = "http://webproxy.nais"
        const val PROXY_SERVER_PORT = 8088
    }
    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost(PROXY_SERVER_HOST, PROXY_SERVER_PORT)
        val httpClient: HttpClient = HttpClientBuilder.create()
            .setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {
                @Throws(HttpException::class)
                override fun determineProxy(target: HttpHost?, request: HttpRequest?, context: HttpContext?): HttpHost {
                    return super.determineProxy(target, request, context)
                }
            })
            .build()
        restTemplate.setRequestFactory(HttpComponentsClientHttpRequestFactory(httpClient))
    }
}