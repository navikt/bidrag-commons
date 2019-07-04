package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HttpStatusResponseTest {

  @Test
  @DisplayName("skal ha tom body")
  void skalHaTomBody() {
    var httpStatusResponse = new HttpStatusResponse<>(HttpStatus.I_AM_A_TEAPOT);

    assertAll(
        () -> assertThat(httpStatusResponse.fetchOptionalResult()).isEmpty(),
        () -> assertThat(httpStatusResponse.getBody()).isNull(),
        () -> assertThat(httpStatusResponse.isBodyEmpty()).isTrue()
    );
  }

  @Test
  @DisplayName("skal ha body")
  void skalHaBody() {
    var httpStatusResponse = new HttpStatusResponse<>(HttpStatus.I_AM_A_TEAPOT, new Object());

    assertAll(
        () -> assertThat(httpStatusResponse.fetchOptionalResult()).isPresent(),
        () -> assertThat(httpStatusResponse.getBody()).isNotNull(),
        () -> assertThat(httpStatusResponse.isBodyPresent()).isTrue()
    );
  }
}