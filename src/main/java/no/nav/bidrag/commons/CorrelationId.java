package no.nav.bidrag.commons;

public class CorrelationId {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final ThreadLocal<String> CORRELATION_ID_VALUE = new ThreadLocal<>();

  private final String idValue;

  public CorrelationId(String correlationId) {
    idValue = correlationId;
    CORRELATION_ID_VALUE.set(idValue);
  }

  public CorrelationId(IdValue idValue) {
    String currentTimeAsString = Long.toHexString(System.currentTimeMillis());
    this.idValue = currentTimeAsString + '(' + idValue.get() + ')';
    CORRELATION_ID_VALUE.set(this.idValue);
  }

  public String get() {
    return idValue;
  }

  public static String fetchCorrelationIdForThread() {
    return CORRELATION_ID_VALUE.get();
  }

  @FunctionalInterface
  public interface IdValue {

    String get();
  }
}
