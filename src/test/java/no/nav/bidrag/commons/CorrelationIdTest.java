package no.nav.bidrag.commons;

import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("CorrelationId")
class CorrelationIdTest {

  private CorrelationId correlationId = new CorrelationId(() -> "value");
}