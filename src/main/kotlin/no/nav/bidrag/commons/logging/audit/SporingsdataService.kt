package no.nav.bidrag.commons.logging.audit

/**
 * Dette interfacet må implementeres som en Spring-Service i alle applikasjoner som skal bruke audit-logging.
 * Metoden må takle alle feltnavn som brukes som grunnlag for audit-logging.
 */
interface SporingsdataService {

    fun findSporingsdataForFelt(feltnavn: String, oppslagsfelt: Any): Sporingsdata
}
