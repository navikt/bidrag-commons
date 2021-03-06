package no.nav.bidrag.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@SuppressWarnings("rawtypes")
@ExtendWith(MockitoExtension.class)
@DisplayName("ExceptionLoggerTest")
class ExceptionLoggerTest {

  private static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("test exception");

  private ExceptionLogger exceptionLogger = new ExceptionLogger("bidrag-commons");
  private final List<String> logMeldinger = new ArrayList<>();

  @Mock
  private Appender appenderMock;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void mockLogAppender() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    lenient().when(appenderMock.getName()).thenReturn("MOCK");
    lenient().when(appenderMock.isStarted()).thenReturn(true);
    logger.addAppender(appenderMock);
  }

  @Test
  @DisplayName("skal logge exception")
  void skalLoggeException() {
    new Service().simulerServiceSomFeilerMedLoggingAvException();

    verifiserLoggingSamtSamleLoggMeldinger();
    assertThat(String.join("\n", logMeldinger))
        .contains(
            "java.lang.IllegalStateException: test exception - Exception caught in bidrag-commons within ExceptionLoggerTest has no cause exception"
        );
  }

  @Test
  @DisplayName("skal logge root exception cause")
  void skalLoggeRootExceptionCause() {
    exceptionLogger.logException(
        new Exception("blew up", new IllegalStateException("in common code", new IllegalArgumentException("because of stupid arguments"))),
        "junit test"
    );

    verifiserLoggingSamtSamleLoggMeldinger();

    assertAll(
        () -> assertThat(String.join("\n", logMeldinger)).contains("blew up"),
        () -> assertThat(String.join("\n", logMeldinger))
            .contains("...caused by java.lang.IllegalStateException, java.lang.IllegalArgumentException: because of stupid arguments")
    );
  }

  @Test
  @DisplayName("skal logge exception selv om exception cause mangler")
  void skalLoggeExceptionSelvOmCauseMangler() {
    exceptionLogger.logException(new Exception("the service blew up"), "junit test");

    verifiserLoggingSamtSamleLoggMeldinger();
    assertThat(String.join("\n", logMeldinger))
        .contains("java.lang.Exception: the service blew up - Exception caught in bidrag-commons within junit test has no cause exception");
  }

  @Test
  @DisplayName("skal logge StackTraceElement fra no.nav før exception")
  void skalLoggeStackTraceElementFraNavForException() {
    exceptionLogger.logException(
        new Exception("blew up", new IllegalStateException("in common code", new IllegalArgumentException("because of stupid arguments"))),
        "junit test"
    );

    verifiserLoggingSamtSamleLoggMeldinger();
    assertThat(String.join("\n", logMeldinger))
        .contains("Exception sett fra nav: no.nav.bidrag.commons.ExceptionLoggerTest.skalLoggeStackTraceElementFraNavForException(line:");
  }

  @Test
  @DisplayName("skal logge response body til et HttpStatusCodeException")
  void skalLoggeResponseBodyTilEtHttpStatusCodeException() {
    exceptionLogger.logException(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", "something is fishy".getBytes(), null), "junit test");

    verifiserLoggingSamtSamleLoggMeldinger();

    assertThat(String.join("\n", logMeldinger)).contains("Response body: something is fishy");
  }

  @Test
  @DisplayName("skal ikke logge response body til et HttpStatusCodeException når body er null")
  void skalIkkeLoggeResponseBodyTilEtHttpStatusCodeException() {
    exceptionLogger.logException(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", null, null), "junit test");

    verifiserLoggingSamtSamleLoggMeldinger();

    assertThat(String.join("\n", logMeldinger)).doesNotContain("Response body:");
  }

  @Test
  @DisplayName("skal kunne instansiere logger med klasser som ikke skal være del av logging")
  void skalInstansiereLoggerMedKlasserSomIkkeSkalVareDelAvLogging() {
    exceptionLogger = new ExceptionLogger("bidrag-commons", OtherService.class);
    new Service().simulerServiceSomFeilerMedLoggingAvException();

    verifiserLoggingSamtSamleLoggMeldinger();
    assertThat(String.join("\n", logMeldinger)).doesNotContain("OtherService");
  }

  @SuppressWarnings("unchecked")
  private void verifiserLoggingSamtSamleLoggMeldinger() {
    verify(appenderMock, atLeastOnce())
        .doAppend(argThat((ArgumentMatcher) argument -> logMeldinger.add(((ILoggingEvent) argument).getFormattedMessage())));
  }

  private class Service {

    void simulerServiceSomFeilerMedLoggingAvException() {
      new OtherService(exceptionLogger).loggExceptionMedExceptionLogger();
    }
  }

  private static class OtherService {

    private final ExceptionLogger exceptionLogger;

    OtherService(ExceptionLogger logger) {
      exceptionLogger = logger;
    }

    void loggExceptionMedExceptionLogger() {
      exceptionLogger.logException(ILLEGAL_STATE_EXCEPTION, "ExceptionLoggerTest");
    }
  }
}