package no.nav.bidrag.commons.util

import com.fasterxml.jackson.databind.exc.MismatchedInputException

fun hentForespørselValideringsfeil(exception: Exception): String? {
    val cause = exception.cause
    return if (cause is MismatchedInputException) createMissingKotlinParameterViolation(cause) else null
}
private fun createMissingKotlinParameterViolation(ex: MismatchedInputException): String {
    val errorFieldRegex = Regex("\\.([^.]*)\\[\\\"(.*)\"\\]\$")
    val paths = ex.path.map { errorFieldRegex.find(it.description)!! }.map {
        val (objectName, field) = it.destructured
        "$objectName.$field"
    }
    return "${paths.joinToString("->")} kan ikke være null"
}
