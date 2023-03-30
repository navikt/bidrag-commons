package no.nav.bidrag.commons.web

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.commons.web.HttpResponse.Companion.from
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class HttpResponseTest {
    @Test
    fun `skal opprettes med gitte verdier`() {
        val httpHeaders = HttpHeaders()
        httpHeaders.add("SVADA", "lada")

        val httpResponse = from(101, httpHeaders, HttpStatus.I_AM_A_TEAPOT)

        httpResponse.responseEntity shouldNotBe null
        val responseEntity = httpResponse.responseEntity
        responseEntity.body shouldBe 101
        responseEntity.headers shouldNotBe null
        responseEntity.headers.getFirst("SVADA") shouldBe "lada"
        responseEntity.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT
    }

    @Test
    fun `skal ikke videresende Contenth headers`() {
        val httpHeaders = HttpHeaders()
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "disp...")
        httpHeaders.add(HttpHeaders.CONTENT_ENCODING, "encode...")
        httpHeaders.add(HttpHeaders.CONTENT_LANGUAGE, "no...")
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, "så mye...")
        httpHeaders.add(HttpHeaders.CONTENT_LOCATION, "på nett...")
        httpHeaders.add(HttpHeaders.CONTENT_RANGE, "hit og dit...")
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "svada...")
        httpHeaders.add(HttpHeaders.WARNING, "high tide")
        val responseEntity = HttpResponse(ResponseEntity<Any>(101, httpHeaders, HttpStatus.I_AM_A_TEAPOT))
            .clearContentHeaders()
            .responseEntity
        responseEntity.body shouldBe 101
        responseEntity.headers shouldHaveSize 1
        responseEntity.headers.getFirst(HttpHeaders.WARNING) shouldBe "high tide"
        responseEntity.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT
    }
}
