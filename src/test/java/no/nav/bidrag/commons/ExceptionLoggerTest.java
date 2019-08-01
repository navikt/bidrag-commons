package no.nav.bidrag.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("ExceptionLoggerTest")
class ExceptionLoggerTest {

  private static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("test exception");

  private ExceptionLogger exceptionLogger = new ExceptionLogger("bidrag-commons");
  private List<String> logMeldinger = new ArrayList<>();

  @Mock
  private Appender appenderMock;

  @BeforeEach
  void initMocks() {
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
  @DisplayName("skal logge exception")
  void skalLoggeException() {
    new Service().simulerServiceSomFeilerMedLoggingAvException();

    verifiserLogging();
    assertThat(String.join("\n", logMeldinger))
        .contains("Exception caught in bidrag-commons within ExceptionLoggerTest - java.lang.IllegalStateException: test exception");
  }

  @Test
  @DisplayName("skal logge root exception cause")
  void skalLoggeRootExceptionCause() {
    exceptionLogger.logException(
        new Exception("blew up", new IllegalStateException("in common code", new IllegalArgumentException("because of stupid arguments"))),
        "junit test"
    );

    verifiserLogging();

    assertAll(
        () -> assertThat(String.join("\n", logMeldinger)).contains("blew up"),
        () -> assertThat(String.join("\n", logMeldinger))
            .contains(
                "...caused by java.lang.Exception, java.lang.IllegalStateException, java.lang.IllegalArgumentException: because of stupid arguments"
            )
    );
  }

  @Test
  @DisplayName("skal logge exception når exception cause mangler")
  void skalLoggeExceptionNarCauseMangler() {
    exceptionLogger.logException(new Exception("the service blew up"), "junit test");

    verifiserLogging();
    assertThat(String.join("\n", logMeldinger)).contains("...caused by java.lang.Exception: the service blew up");
  }

  @Test
  @DisplayName("skal logge StackTraceElement fra no.nav før exception")
  void skalLoggeStackTraceElementFraNavForException() {
    exceptionLogger.logException(
        new Exception("blew up", new IllegalStateException("in common code", new IllegalArgumentException("because of stupid arguments"))),
        "junit test"
    );

    verifiserLogging();
    assertThat(String.join("\n", logMeldinger))
        .contains("Exception sett fra nav: no.nav.bidrag.commons.ExceptionLoggerTest.skalLoggeStackTraceElementFraNavForException(line:");
  }

  @SuppressWarnings("unchecked")
  private void verifiserLogging() {
    verify(appenderMock, atLeastOnce()).doAppend(
        argThat((ArgumentMatcher) argument -> {
          logMeldinger.add(((ILoggingEvent) argument).getFormattedMessage());

          return true;
        }));
  }

  private class Service {

    void simulerServiceSomFeilerMedLoggingAvException() {
      new OtherService(exceptionLogger).loggExceptionMedExceptionLogger();
    }
  }

  private class OtherService {

    private final ExceptionLogger exceptionLogger;

    OtherService(ExceptionLogger exceptionLogger) {
      this.exceptionLogger = exceptionLogger;
    }

    void loggExceptionMedExceptionLogger() {
      exceptionLogger.logException(ILLEGAL_STATE_EXCEPTION, "ExceptionLoggerTest");
    }
  }
}