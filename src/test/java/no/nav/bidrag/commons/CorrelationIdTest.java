package no.nav.bidrag.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CorrelationId")
class CorrelationIdTest {

  @Test
  @DisplayName("skal lage correlation id med eksisterende verdi")
  void skalLageCorrelationIdMedEksisterendeVerdi() {
    CorrelationId correlationId = CorrelationId.existing("eksisterende");

     assertThat(correlationId.get()).isEqualTo("eksisterende");
  }

  @Test
  @DisplayName("skal lage correlation id som tidsstemplet verdi")
  void skalLageCorrelationIdSomTidsstempletVerdi() {
    CorrelationId correlationId = CorrelationId.generateTimestamped("value");
    String timestamp = Long.toHexString(System.currentTimeMillis());

     assertThat(correlationId.get())
         .startsWith(timestamp.substring(0, timestamp.length() - 3))
         .contains("-value");
  }

  @Test
  @DisplayName("skal generere ny verdi når gitt Correlation ID er null")
  void skalGenerereNyVerdiNarGittVerdiErNull() {
    var correlationId = CorrelationId.existing(null);

    assertThat(correlationId.get()).contains("-correlationId");
  }
}