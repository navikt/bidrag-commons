package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;

@DisplayName("EnhetFilter")
class EnhetFilterTest {

  private EnhetFilter enhetFilter = new EnhetFilter();

  @Mock
  private Appender appenderMock;

  @Mock
  private FilterChain filterChainMock;

  @Mock
  private HttpServletRequest httpServletRequestMock;

  @Mock
  private HttpServletResponseWrapper httpServletResponseMock;

  @BeforeEach
  void initMocksAndMockLogAppender() {
    MockitoAnnotations.initMocks(this);
    mockLogAppender();
  }

  @SuppressWarnings("unchecked")
  private void mockLogAppender() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    when(appenderMock.getName()).thenReturn("MOCK");
    when(appenderMock.isStarted()).thenReturn(true);
    logger.addAppender(appenderMock);
  }

  @Test
  @DisplayName("skal videresende X_ENHETSNR_HEADER fra request til response")
  void skalVideresendeHeaderMedEnhetsnummer() throws IOException, ServletException {
    when(httpServletRequestMock.getHeader(EnhetFilter.X_ENHETSNR_HEADER)).thenReturn("enhetsnummer");

    enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    assertAll(
        () -> verify(httpServletRequestMock).getHeader(EnhetFilter.X_ENHETSNR_HEADER),
        () -> verify(httpServletResponseMock).addHeader(eq(EnhetFilter.X_ENHETSNR_HEADER), eq("enhetsnummer"))
    );
  }

  @Test
  @DisplayName("skal ikke videresende X_ENHETSNR_HEADER fra request til response når headerverdi ikke finnes på request")
  void skalIkkeVideresendeHeaderMedEnhetsnummerNarDetIkkeFinnes() throws IOException, ServletException {
    when(httpServletRequestMock.getHeader(EnhetFilter.X_ENHETSNR_HEADER)).thenReturn(null);

    enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    assertAll(
        () -> verify(httpServletRequestMock).getHeader(EnhetFilter.X_ENHETSNR_HEADER),
        () -> verify(httpServletResponseMock, never()).addHeader(anyString(), anyString())
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge enhetsnummer som videresendes")
  void skalLoggeEnhetsnummerSomVideresendes() throws IOException, ServletException {
    when(httpServletRequestMock.getHeader(EnhetFilter.X_ENHETSNR_HEADER)).thenReturn("007");
    when(httpServletRequestMock.getRequestURI()).thenReturn("noe");

    enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    var logCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(appenderMock).doAppend(logCaptor.capture());
    var loggingEvent = (ILoggingEvent) logCaptor.getValue();

    assertThat(loggingEvent).isNotNull();
    assertThat(loggingEvent.getFormattedMessage()).contains("Behandler request 'noe' for enhet med enhetsnummer 007");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge at enhetsnummer ikke videresendes")
  void skalLoggeAtEnhetsnummerIkkeVideresendes() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("noe");

    enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    var logCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(appenderMock).doAppend(logCaptor.capture());
    var loggingEvent = (ILoggingEvent) logCaptor.getValue();

    assertThat(loggingEvent).isNotNull();
    assertThat(loggingEvent.getFormattedMessage()).contains("Behandler request 'noe' uten informasjon om enhetsnummer");
  }

  @Test
  @DisplayName("skal fortsette filtrering av request etter at filter er kjørt")
  void skalFortsetteFiltrering() throws IOException, ServletException {
    enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    verify(filterChainMock).doFilter(httpServletRequestMock, httpServletResponseMock);
  }
}
