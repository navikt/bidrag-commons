package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

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
}