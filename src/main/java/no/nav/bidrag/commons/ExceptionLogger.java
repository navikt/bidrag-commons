package no.nav.bidrag.commons;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);
  private static final String CAUSED_BY_MSG = " ...caused by %s: %s.";
  private static final String PACKAGE_NO_NAV = ExceptionLogger.class.getPackageName().substring(
      0, ExceptionLogger.class.getPackageName().indexOf(".bidrag")
  );

  private final String application;

  public ExceptionLogger(String application) {
    this.application = application;
  }

  public void logException(Throwable throwable, String defaultLocation) {
    LOGGER.error("Exception caught in {} within {} - {}: {}", application, defaultLocation, throwable.getClass().getName(), throwable.getMessage());
    log(throwable);

    StackWalker.getInstance().walk(
        stackFrameStream -> stackFrameStream
            .filter(stackFrame -> stackFrame.getClassName().startsWith(PACKAGE_NO_NAV))
            .skip(2) // skip this method, and the caller of this method
            .collect(Collectors.toList())
    ).forEach(
        stackFrame -> LOGGER.error(" - {}(line:{}) - {}", stackFrame.getClassName(), stackFrame.getLineNumber(), stackFrame.getFileName())
    );
  }

  private void log(Throwable throwable) {
    var possibleCause = Optional.ofNullable(throwable.getCause());

    if (possibleCause.isPresent()) {
      logCause(possibleCause.get());
    } else {
      LOGGER.error(String.format(CAUSED_BY_MSG, throwable.getClass().getName(), throwable.getMessage()), throwable);
    }
  }

  private void logCause(Throwable cause) {
    var exceptionTypes = new ArrayList<String>();

    exceptionTypes.add(cause.getClass().getSimpleName());

    while (cause.getCause() != null) {
      exceptionTypes.add(cause.getCause().getClass().getSimpleName());
      cause = cause.getCause();
    }

    LOGGER.error(String.format(CAUSED_BY_MSG, String.join(", ", exceptionTypes), cause.getMessage()), cause);
  }
}
