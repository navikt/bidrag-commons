package no.nav.bidrag.commons.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

  private static final String CORRELATION_ID = "X-Correlation-ID";

  private CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();
  private Set<String> logMeldinger = new HashSet<>();

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
  @SuppressWarnings("unchecked")
  @DisplayName("skal logge requests mot servlet")
  void skalLoggeRequestsMotActuatorEndpoints() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("something");
    when(httpServletRequestMock.getMethod()).thenReturn("GET");

    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    assertAll(
        () -> verify(appenderMock, atLeastOnce()).doAppend(
            argThat((ArgumentMatcher) argument -> {
              logMeldinger.add(((ILoggingEvent) argument).getFormattedMessage());

              return true;
            })),
        () -> assertThat(String.join("\n", logMeldinger)).contains("Prosessing GET something")
    );
  }

  @Test
  @DisplayName("skal ikke logge requests mot actuator endpoints")
  void skalIkkeLoggeRequestsMotActuatorEndpoints() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("/actuator/health").thenReturn("/actuator/something");

    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);
    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    verifyZeroInteractions(appenderMock);
  }

  @Test
  @DisplayName("skal legge HttpHeader.CORRELATION_ID på response")
  void skalLeggeHttpHeaderCorrelationIdPaaResponse() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("somewhere");

    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    verify(httpServletResponseMock).addHeader(eq(CORRELATION_ID), anyString());
  }

  @Test
  @DisplayName("skal ikke legge HttpHeader.CORRELATION_ID på response når den allerede eksisterer")
  void skalIkkeLeggeHttpHeaderCorrelationIdPaaResponseNaarDenAlleredeEksisterer() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("somewhere else");
    when(httpServletRequestMock.getHeader(CORRELATION_ID)).thenReturn("svada");
    when(httpServletResponseMock.containsHeader(CORRELATION_ID)).thenReturn(true);

    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    verify(httpServletResponseMock, never()).addHeader(anyString(), anyString());
  }

  @Test
  @DisplayName("skal bruke request uri som correlation id")
  void skalBrukeRequestUriSomCorrelationId() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("somewhere");
    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    ArgumentCaptor<String> correlationCaptor = ArgumentCaptor.forClass(String.class);
    verify(httpServletResponseMock).addHeader(eq(CORRELATION_ID), correlationCaptor.capture());

    assertThat(correlationCaptor.getValue()).contains("(somewhere)");
  }

  @Test
  @DisplayName("skal lage correlation id av siste del fra request uri")
  void skalLageCorrelationIdAvSisteDelFraRequestUri() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("/en/forbanna/journalpost");
    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    ArgumentCaptor<String> correlationCaptor = ArgumentCaptor.forClass(String.class);
    verify(httpServletResponseMock).addHeader(eq(CORRELATION_ID), correlationCaptor.capture());

    assertThat(correlationCaptor.getValue()).contains("(journalpost)");
  }

  @Test
  @DisplayName("skal lage correlation id av siste del fra request uri som er ren tekst, samt legge på eventuelle tall")
  void skalLageCorrelationIdAvSisteDelFraRequestUriSomHarRenTekstSamtEventuelleTall() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("/en/identifisert/journalpost/1001");
    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    ArgumentCaptor<String> correlationCaptor = ArgumentCaptor.forClass(String.class);
    verify(httpServletResponseMock).addHeader(eq(CORRELATION_ID), correlationCaptor.capture());

    assertThat(correlationCaptor.getValue()).contains("(journalpost/1001)");
  }

  @Test
  @DisplayName("skal lage correlation id av siste del fra request uri som er ren tekst, samt legge på eventuelle prefiksede tall")
  void skalLageCorrelationIdAvSisteDelFraRequestUriSomHarRenTekstSamtEventuellePrefiksedeTall() throws IOException, ServletException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("/en/identifisert/journalpost/BID-1001");
    correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock);

    ArgumentCaptor<String> correlationCaptor = ArgumentCaptor.forClass(String.class);
    verify(httpServletResponseMock).addHeader(eq(CORRELATION_ID), correlationCaptor.capture());

    assertThat(correlationCaptor.getValue()).contains("(journalpost/BID-1001)");
  }

  @Test
  @DisplayName("skal legge correlation id på ThreadLocal som kan leses for konfigurasjon")
  void skalLeggeCorrelationIdPaaThreadLocal() throws InterruptedException {
    when(httpServletRequestMock.getRequestURI()).thenReturn("go somewhere");

    CorrelationIdThread aCorrelationIdThread = new CorrelationIdThread(
        () -> correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
    );

    CorrelationIdThread anotherCorrelationIdThread = new CorrelationIdThread(
        () -> correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
    );

    aCorrelationIdThread.start();
    anotherCorrelationIdThread.start();
    Thread.sleep(10); // to be sure the value is not from the same millis
    aCorrelationIdThread.join();
    anotherCorrelationIdThread.join();

    assertAll(
        () -> assertThat(aCorrelationIdThread.correlationId).as("a correlation id")
            .isNotNull().isNotEqualTo(anotherCorrelationIdThread.correlationId),
        () -> assertThat(anotherCorrelationIdThread.correlationId).as("another correlation id")
            .isNotNull().isNotEqualTo(aCorrelationIdThread.correlationId)
    );
  }

  class CorrelationIdThread extends Thread {

    private final FilterExecutor filterExecutor;
    String correlationId;

    CorrelationIdThread(FilterExecutor filterExecutor) {
      this.filterExecutor = filterExecutor;
    }

    @Override
    public void run() {
      try {
        filterExecutor.doFilter();
        correlationId = CorrelationIdFilter.fetchCorrelationIdForThread();
      } catch (IOException | ServletException e) {
        throw new AssertionError(e);
      }
    }
  }

  @FunctionalInterface
  interface FilterExecutor {

    void doFilter() throws IOException, SecurityException, ServletException;
  }
}
