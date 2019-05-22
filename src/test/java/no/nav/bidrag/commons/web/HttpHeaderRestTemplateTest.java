package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.core.Appender;
import java.lang.reflect.Type;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

@DisplayName("HttpHeaderRestTemplate")
class HttpHeaderRestTemplateTest {

  private HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();

  @Mock
  private Appender appenderMock;
  @Mock
  private Type typeMock;

  private int invoke;

  @BeforeEach
  void initMocks() {
    MockitoAnnotations.initMocks(this);
    mockLogAppender();
  }

  @SuppressWarnings("unchecked")
  private void mockLogAppender() {
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    when(appenderMock.getName()).thenReturn("MOCK");
    when(appenderMock.isStarted()).thenReturn(true);
    logger.addAppender(appenderMock);
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge hvilke http headers den bruker")
  void skalLoggeBrukAvHttpHeader() {
    httpHeaderRestTemplate.addHeaderGenerator("JUNIT_HEADER", () -> "header value");

    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock).doAppend(argCapture.capture());
    var logMsg = String.valueOf(argCapture.getValue());

    assertThat(logMsg).contains("Using JUNIT_HEADER: header value");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge eksisterende headers fra gitt request object")
  void skalLoggeBrukAvEksisterendeHttpHeader() {
    var existingHttpHeaders = new HttpHeaders();
    existingHttpHeaders.add("EXISTING_HEADER", "existing value");

    httpHeaderRestTemplate.addHeaderGenerator("ADDITIONAL_HEADER", () -> "additional value");

    httpHeaderRestTemplate.httpEntityCallback(new HttpEntity<>(null, existingHttpHeaders), typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock, atLeastOnce()).doAppend(argCapture.capture());
    var logMsgs = argCapture.getAllValues().stream().map(Object::toString).collect(Collectors.joining("\n"));

    assertAll(
        () -> assertThat(logMsgs).contains("Using EXISTING_HEADER: existing value"),
        () -> assertThat(logMsgs).contains("Using ADDITIONAL_HEADER: additional value")
    );
  }

  @Test
  @DisplayName("skal ikke feile når httpEntityCallback brukes med request body som er annet enn HttpEntity")
  void skalIkkeFeileNaarHttpEntityCallbackBrukesMedTypeSomErAnnetEnnHttpEntity() {
    httpHeaderRestTemplate.addHeaderGenerator("na", () -> "na");

    httpHeaderRestTemplate.httpEntityCallback("a request body", typeMock);
    httpHeaderRestTemplate.httpEntityCallback(new Object(), typeMock);
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge dynamisk header-verdi")
  void skalLoggeDynamiskHeaderVerdi() {
    httpHeaderRestTemplate.addHeaderGenerator("DYNAMIC_HEADER", () -> String.format("Header value #%d is created!!!", ++invoke));

    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);
    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);
    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock, atLeastOnce()).doAppend(argCapture.capture());
    var logMsgs = argCapture.getAllValues().stream().map(Object::toString).collect(Collectors.joining("\n"));

    assertAll(
        () -> assertThat(logMsgs).contains("Header value #1 is created!!!"),
        () -> assertThat(logMsgs).contains("Header value #2 is created!!!"),
        () -> assertThat(logMsgs).contains("Header value #3 is created!!!")
    );
  }
}
