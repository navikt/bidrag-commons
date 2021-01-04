package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
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

  private final HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();

  @Mock
  @SuppressWarnings("rawtypes")
  private Appender appenderMock;

  @Mock
  private Type typeMock;

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
  @DisplayName("skal logge hvilke http headers den lager")
  void skalLoggeHttpHeaderDenLager() {
    httpHeaderRestTemplate.addHeaderGenerator("JUNIT_HEADER", () -> "header value");
    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock).doAppend(argCapture.capture());
    var logMsg = String.valueOf(argCapture.getValue());

    assertThat(logMsg).contains("Generate header(s): [JUNIT_HEADER]");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge navnet på eksisterende headers fra gitt request object")
  void skalLoggeNavnAvEksisterendeHttpHeader() {
    var existingHttpHeaders = new HttpHeaders();
    existingHttpHeaders.add("EXISTING_HEADER", "existing value");

    httpHeaderRestTemplate.addHeaderGenerator("ADDITIONAL_HEADER", () -> "additional value");

    httpHeaderRestTemplate.httpEntityCallback(new HttpEntity<>(null, existingHttpHeaders), typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock, atLeastOnce()).doAppend(argCapture.capture());
    var logMsgs = argCapture.getAllValues().stream().map(Object::toString).collect(Collectors.joining("\n"));

    assertThat(logMsgs).contains("Existing header(s): [EXISTING_HEADER]");
  }

  @Test
  @DisplayName("skal ikke feile når httpEntityCallback brukes med request body som er annet enn HttpEntity")
  void skalIkkeFeileNaarHttpEntityCallbackBrukesMedTypeSomErAnnetEnnHttpEntity() {
    httpHeaderRestTemplate.addHeaderGenerator("na", () -> "na");

    httpHeaderRestTemplate.httpEntityCallback("a request body", typeMock);
    httpHeaderRestTemplate.httpEntityCallback(new Object(), typeMock);
  }

  @Test
  @DisplayName("skal fjerne header generator")
  void skalFjerneHeaderGenerator() {
    httpHeaderRestTemplate.addHeaderGenerator("EN_HEADER", () -> "header value");
    httpHeaderRestTemplate.addHeaderGenerator("EN_ANNEN_HEADER", () -> "en annen header value");
    httpHeaderRestTemplate.removeHeaderGenerator("EN_ANNEN_HEADER");
    httpHeaderRestTemplate.httpEntityCallback(null, typeMock);

    var argCapture = ArgumentCaptor.forClass(Object.class);
    verify(appenderMock).doAppend(argCapture.capture());
    var logMsg = String.valueOf(argCapture.getValue());

    assertThat(logMsg)
        .contains("EN_HEADER")
        .doesNotContain("EN_ANNEN_HEADER");
  }
}
