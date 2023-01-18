package no.nav.bidrag.commons.logging.audit

/**
 * [custom1], [custom2], [custom3] brukes for å logge ekstra felter, eks sak, behandling,
 * disse logges til cs3,cs5,cs6 då cs1,cs2 og cs4 er til internt bruk
 * Kan brukes med eks CustomKeyValue(key=sak, value=saksnummer)
 */
data class Sporingsdata(
    val personIdent: String,
    val custom1: Pair<String, String>? = null,
    val custom2: Pair<String, String>? = null,
    val custom3: Pair<String, String>? = null
)
