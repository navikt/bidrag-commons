package no.nav.bidrag.commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);
  private static final String CAUSED_BY_MSG = "...caused by %s: %s.";
  private static final String PACKAGE_NO_NAV = ExceptionLogger.class.getPackageName().substring(
      0, ExceptionLogger.class.getPackageName().indexOf(".bidrag")
  );

  private final String application;

  public ExceptionLogger(String application) {
    this.application = application;
  }

  public void logException(Throwable throwable, String defaultLocation) {
    var exceptionClassName = throwable.getClass().getName();
    var exceptionMessage = throwable.getMessage();
    var possibleCause = Optional.ofNullable(throwable.getCause());

    if (possibleCause.isPresent()) {
      LOGGER.error("{}: {} - Exception caught in {} within {}", exceptionClassName, exceptionMessage, application, defaultLocation);
      logCause(throwable.getCause());
    } else {
      var message = String.format(
          "%s: %s - Exception caught in %s within %s has no cause exception", exceptionClassName, exceptionMessage, application, defaultLocation
      );

      LOGGER.error(message, throwable);
      logFirstStackTraceElementFromNav(Arrays.stream(throwable.getStackTrace()));
    }
  }

  private void logCause(Throwable cause) {
    var throwables = fetchAllThrowables(cause);
    var exceptionTypes = throwables.stream()
        .map(aThrowable -> aThrowable.getClass().getName())
        .collect(Collectors.joining(", "));

    Collections.reverse(throwables);

    for (Throwable throwable : throwables) {
      if (throwable.getCause() == null) {
        LOGGER.error(String.format(CAUSED_BY_MSG, exceptionTypes, throwable.getMessage()), throwable);
      }

      if (logFirstStackTraceElementFromNav(Arrays.stream(throwable.getStackTrace()))) {
        return;
      }
    }
  }

  private List<Throwable> fetchAllThrowables(Throwable throwable) {
    var cause = throwable;
    var allThrowables = new ArrayList<Throwable>();

    do {
      allThrowables.add(cause);
      cause = cause.getCause();
    } while (cause != null);

    return allThrowables;
  }

  private boolean logFirstStackTraceElementFromNav(Stream<StackTraceElement> stackTraceElements) {

    var firstElementFromNav = stackTraceElements.filter(elem -> elem.getClassName().startsWith(PACKAGE_NO_NAV)).findFirst();
    var loggedStackTraceElement = false;

    if (firstElementFromNav.isPresent()) {
      StackTraceElement stackTraceElement = firstElementFromNav.get();
      LOGGER.error(
          "Exception sett fra nav: {}.{}(line:{}) - {}",
          stackTraceElement.getClassName(),
          stackTraceElement.getMethodName(),
          stackTraceElement.getLineNumber(),
          stackTraceElement.getFileName()
      );
      loggedStackTraceElement = true;
    }

    return loggedStackTraceElement;
  }
}
