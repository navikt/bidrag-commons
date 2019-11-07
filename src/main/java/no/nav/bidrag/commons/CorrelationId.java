package no.nav.bidrag.commons;

public class CorrelationId {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final ThreadLocal<String> CORRELATION_ID_VALUE = new ThreadLocal<>();

  private final String idValue;

  private CorrelationId(String correlationId) {
    idValue = correlationId;
    CORRELATION_ID_VALUE.set(idValue);
  }

  public String get() {
    return idValue;
  }

  public static String fetchCorrelationIdForThread() {
    return CORRELATION_ID_VALUE.get();
  }

  public static CorrelationId existing(String value) {
    if (value == null || value.equals("")) {
      return generateTimestamped("correlationId");
    }

    return new CorrelationId(value);
  }

  public static CorrelationId generateTimestamped(String value) {
    String currentTimeAsString = Long.toHexString(System.currentTimeMillis());
    return new CorrelationId(currentTimeAsString + '-' + value);
  }
}
