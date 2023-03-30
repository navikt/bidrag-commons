package no.nav.bidrag.commons.web

import org.springframework.http.HttpHeaders

object WebUtil {
    fun initHttpHeadersWith(name: String, value: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.add(name, value)
        return httpHeaders
    }
}
