package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HttpResponseTest {

  @Test
  @DisplayName("skal opprettes med gitte verdier")
  void skalOpprettesMedGitteVerdier() {
    var httpHeaders = new HttpHeaders();
    httpHeaders.add("SVADA", "lada");
    var httpResponse = HttpResponse.from(101, httpHeaders, HttpStatus.I_AM_A_TEAPOT);

    assertThat(httpResponse.getResponseEntity()).as("response entity").isNotNull();

    var responseEntity = httpResponse.getResponseEntity();

    assertAll(
        () -> assertThat(responseEntity.getBody()).as("body").isEqualTo(101),
        () -> assertThat(responseEntity.getHeaders()).as("headers").isNotNull(),
        () -> assertThat(responseEntity.getHeaders().getFirst("SVADA")).as("header").isEqualTo("lada"),
        () -> assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.I_AM_A_TEAPOT)
    );
  }

  @Test
  @DisplayName("skal ikke videresende Contenth* headers")
  void skalIkkeVideresendeHeaderContentLength() {
    var httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "disp...");
    httpHeaders.add(HttpHeaders.CONTENT_ENCODING, "encode...");
    httpHeaders.add(HttpHeaders.CONTENT_LANGUAGE, "no...");
    httpHeaders.add(HttpHeaders.CONTENT_LENGTH, "så mye...");
    httpHeaders.add(HttpHeaders.CONTENT_LOCATION, "på nett...");
    httpHeaders.add(HttpHeaders.CONTENT_RANGE, "hit og dit...");
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "svada...");
    httpHeaders.add(HttpHeaders.WARNING, "high tide");

    var responseEntity = new HttpResponse<Object>(new ResponseEntity<>(101, httpHeaders, HttpStatus.I_AM_A_TEAPOT))
        .clearContentHeaders()
        .getResponseEntity();

    assertAll(
        () -> assertThat(responseEntity.getBody()).as("body").isEqualTo(101),
        () -> assertThat(responseEntity.getHeaders()).as("headers").isNotNull().hasSize(1),
        () -> assertThat(responseEntity.getHeaders().getFirst(HttpHeaders.WARNING)).as("header").isEqualTo("high tide"),
        () -> assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.I_AM_A_TEAPOT)
    );
  }
}