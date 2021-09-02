package no.nav.bidrag.commons;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

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
  private static final String PACKAGE_NO_NAV = ExceptionLogger.class.getPackageName().substring(
      0, ExceptionLogger.class.getPackageName().indexOf(".bidrag")
  );

  private final String application;
  private final Set<String> doNotLogClasses = new HashSet<>();

  public ExceptionLogger(String application, Class<?>... doNotLogClasses) {
    this.application = application;

    if (doNotLogClasses != null) {
      Arrays.stream(doNotLogClasses).forEach(aClass -> this.doNotLogClasses.add(aClass.getName()));
    }
  }

  public List<String> logException(Throwable throwable, String defaultLocation) {
    var loggMeldinger = new ArrayList<String>();
    var exceptionClassName = throwable.getClass().getName();
    var exceptionMessage = throwable.getMessage();
    var possibleCause = Optional.ofNullable(throwable.getCause());

    if (possibleCause.isPresent()) {
      var melding = String.format(
          "%s: %s - Exception caught in %s within %s", exceptionClassName, exceptionMessage, application, defaultLocation
      );

      loggMeldinger.add(melding);
      LOGGER.error(melding);
      loggMeldinger.addAll(logCause(throwable.getCause()));
    } else {
      var message = String.format(
          "%s: %s - Exception caught in %s within %s has no cause exception", exceptionClassName, exceptionMessage, application, defaultLocation
      );

      loggMeldinger.add(message);
      LOGGER.error(message, throwable);

      if (throwable instanceof HttpStatusCodeException) {
        var statusCodeException = (HttpStatusCodeException) throwable;

        if (!"".equals(statusCodeException.getResponseBodyAsString())) {
          var responseBody = "Response body: " + statusCodeException.getResponseBodyAsString();
          loggMeldinger.add(responseBody);
          LOGGER.error(responseBody);
        }
      }

      loggMeldinger.addAll(logFirstThreeStackFramesFromNavCode(throwable));
    }

    return loggMeldinger;
  }

  private List<String> logCause(Throwable cause) {
    var loggMeldinger = new ArrayList<String>();
    var throwables = fetchAllThrowables(cause);
    var exceptionTypes = throwables.stream()
        .map(aThrowable -> aThrowable.getClass().getName())
        .collect(Collectors.joining(", "));

    Collections.reverse(throwables);

    for (Throwable throwable : throwables) {
      if (throwable.getCause() == null) {
        var causedBy = String.format(CAUSED_BY_MSG, exceptionTypes, throwable.getMessage());
        LOGGER.error(causedBy, throwable);
        loggMeldinger.add(causedBy);
        loggMeldinger.addAll(logFirstThreeStackFramesFromNavCode(throwable));
      }
    }

    return loggMeldinger;
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

  private List<String> logFirstThreeStackFramesFromNavCode(Throwable throwable) {
    var stackFrames = Arrays.stream(throwable.getStackTrace())
        .filter(not(elem -> elem.getClassName().equals(ExceptionLogger.class.getName())))
        .filter(elem -> elem.getClassName().startsWith(PACKAGE_NO_NAV))
        .filter(not(elem -> doNotLogClasses.contains(elem.getClassName())))
        .filter(not(elem -> "<generated>".equals(elem.getFileName()))) // generated proxy code
        .limit(3)
        .collect(toList());

    if (stackFrames.isEmpty()) {
      throw new IllegalStateException("Unintended usage: ExceptionLogger is intented to be used within code from nav.no");
    }

    var firstStack = stackFrames.get(0);
    var exceptionSettFraNav = String.format(
        "Exception sett fra nav: %s.%s(line:%s - %s)%s",
        firstStack.getClassName(),
        firstStack.getMethodName(),
        firstStack.getLineNumber(),
        firstStack.getFileName(),
        fetchFileInfoFromPreviousElements(stackFrames)
    );

    LOGGER.error(exceptionSettFraNav);

    return List.of(exceptionSettFraNav);
  }

  private String fetchFileInfoFromPreviousElements(List<StackTraceElement> stackFrames) {
    if (stackFrames.size() == 1) {
      return "";
    }

    var fileInfo = new StringBuilder();

    for (int i = 1; i < stackFrames.size(); i++) {
      var stackFrame = stackFrames.get(i);
      fileInfo.append(String.format(", %s.%s: %s", stackFrame.getFileName(), stackFrame.getMethodName(), stackFrame.getLineNumber()));
    }

    return " - previous frames: " + fileInfo.substring(2);
  }
}
