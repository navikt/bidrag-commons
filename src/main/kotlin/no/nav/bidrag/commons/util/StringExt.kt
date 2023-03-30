package no.nav.bidrag.commons.util

fun String?.trimToNull(): String? {
    return this?.trim()?.ifBlank { null }
}
