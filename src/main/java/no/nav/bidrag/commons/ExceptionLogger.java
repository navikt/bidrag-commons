package no.nav.bidrag.commons;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);
  private static final String PACKAGE_NO_NAV = ExceptionLogger.class.getPackageName().substring(
      0, ExceptionLogger.class.getPackageName().indexOf(".bidrag")
  );

  private final String application;

  public ExceptionLogger(String application) {
    this.application = application;
  }

  public void logException(Exception exception, String defaultLocation) {
    LOGGER.error("Exception caught in {} within {}", application, defaultLocation);
    LOGGER.error("Failed by {}: {}", exception.getClass().getName(), exception.getMessage());

    StackWalker.getInstance().walk(
        stackFrameStream -> stackFrameStream
            .filter(stackFrame -> stackFrame.getClassName().startsWith(PACKAGE_NO_NAV))
            .skip(2) // skip this method, and the caller of this method
            .collect(Collectors.toList())
    ).forEach(
        stackFrame -> LOGGER.error(" - {}(line:{}) - {}", stackFrame.getClassName(), stackFrame.getLineNumber(), stackFrame.getFileName())
    );
  }
}
