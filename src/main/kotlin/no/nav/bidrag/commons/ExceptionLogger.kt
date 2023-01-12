package no.nav.bidrag.commons

import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpStatusCodeException

class ExceptionLogger(private val application: String, vararg doNotLogClasses: Class<*>) {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val doNotLogClasses = doNotLogClasses.map { it.name }

  fun logException(throwable: Throwable, defaultLocation: String?): List<String> {
    val exceptionAndDetails = ArrayList<String>()
    val exceptionClassSimpleName = throwable.javaClass.simpleName
    val exceptionMessage = throwable.message
    val possibleCause = throwable.cause
    val melding =
      String.format("%s: %s - caught in %s within %s. Details:", exceptionClassSimpleName, exceptionMessage, application, defaultLocation)
    exceptionAndDetails.add(melding)
    if (possibleCause != null) {
      exceptionAndDetails.addAll(logCause(possibleCause))
    } else {
      exceptionAndDetails.add("|> no root cause")
      if (throwable is HttpStatusCodeException) {
        if (throwable.responseBodyAsString.isNotEmpty()) {
          val responseBody = "|> response body: " + throwable.responseBodyAsString
          exceptionAndDetails.add(responseBody)
        }
      }
    }
    exceptionAndDetails.addAll(logFirstThreeStackFramesFromNavCode(throwable))
    logger.error(java.lang.String.join("\n", exceptionAndDetails))
    return exceptionAndDetails
  }

  private fun logCause(cause: Throwable): List<String> {
    val throwables = fetchAllThrowables(cause)
    val exceptionTypes = throwables.joinToString(", ") { it.javaClass.name }
    return throwables.mapNotNull {
      if (it.cause == null) {
        String.format(CAUSED_BY_MSG, exceptionTypes, it.message)
      } else
        null
    }
  }

  private fun fetchAllThrowables(throwable: Throwable): List<Throwable> {
    var cause: Throwable? = throwable
    val allThrowables = ArrayList<Throwable>()
    while (cause != null) {
      allThrowables.add(cause)
      cause = cause.cause
    }
    return allThrowables
  }

  private fun logFirstThreeStackFramesFromNavCode(throwable: Throwable): List<String> {
    val stackFrames = throwable.stackTrace
      .filter { it.className != ExceptionLogger::class.java.name }
      .filter { it.className.startsWith(PACKAGE_NO_NAV) }
      .filter { !doNotLogClasses.contains(it.className) }
      .filter { "<generated>" != it.fileName } // generated proxy code
      .take(3)
    if (stackFrames.isEmpty()) {
      return emptyList()
    }
    val firstStack = stackFrames[0]
    val exceptionSettFraNav = String.format(
      "|> kode i nav: %s.%s(line:%s - %s)",
      firstStack.className,
      firstStack.methodName,
      firstStack.lineNumber,
      firstStack.fileName
    )
    return if (stackFrames.size > 1) {
      listOf(exceptionSettFraNav, fetchFileInfoFromPreviousElements(stackFrames))
    } else listOf(exceptionSettFraNav)
  }

  private fun fetchFileInfoFromPreviousElements(stackFrames: List<StackTraceElement>): String {
    val fileInfo = StringBuilder()
    for (i in 1 until stackFrames.size) {
      val stackFrame = stackFrames[i]
      fileInfo.append(String.format(", %s.%s: %s", stackFrame.fileName, stackFrame.methodName, stackFrame.lineNumber))
    }
    return "|> previous frames: " + fileInfo.substring(2)
  }

  companion object {
    private const val CAUSED_BY_MSG = "|> caused by %s: %s."
    private val PACKAGE_NO_NAV = ExceptionLogger::class.java.packageName.substring(
      0, ExceptionLogger::class.java.packageName.indexOf(".bidrag")
    )
  }
}