package no.nav.bidrag.commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

public class ExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);
  private static final String CAUSED_BY_MSG = "...caused by %s: %s.";
  private static final String CLASS_NAME = ExceptionLogger.class.getName();
  private static final String PACKAGE_NO_NAV = ExceptionLogger.class.getPackageName().substring(
      0, ExceptionLogger.class.getPackageName().indexOf(".bidrag")
  );

  private final String application;
  private final Set<String> doNotLogClasses = new HashSet<>();

  public ExceptionLogger(String application, Class<?> ... doNotLogClasses) {
    this.application = application;

    if (doNotLogClasses != null) {
      Arrays.stream(doNotLogClasses).forEach(aClass -> this.doNotLogClasses.add(aClass.getName()));
    }
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

      if (throwable instanceof HttpStatusCodeException) {
        var statusCodeException = (HttpStatusCodeException) throwable;

        if (!"".equals(statusCodeException.getResponseBodyAsString())) {
          LOGGER.error("Response body: " + statusCodeException.getResponseBodyAsString());
        }
      }

      logFirstStackFrameForNav();
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
        logFirstStackFrameForNav();
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

  private void logFirstStackFrameForNav() {
    StackWalker.getInstance().walk(stackFrames -> {
      var stackFrame = stackFrames
          .filter(elem -> !elem.getClassName().equals(CLASS_NAME))
          .filter(elem -> elem.getClassName().startsWith(PACKAGE_NO_NAV))
          .filter(elem -> !doNotLogClasses.contains(elem.getClassName()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Unintended usage: ExceptionLogger is intented to be used within code from nav.no"));

      LOGGER.error(
          "Exception sett fra nav: {}.{}(line:{}) - {}",
          stackFrame.getClassName(),
          stackFrame.getMethodName(),
          stackFrame.getLineNumber(),
          stackFrame.getFileName()
      );

      return null;
    });
  }
}
