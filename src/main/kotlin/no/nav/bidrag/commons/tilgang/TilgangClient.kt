package no.nav.bidrag.commons.tilgang

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.transport.tilgang.Sporingsdata
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
@Import(RestOperationsAzure::class)
class TilgangClient(
    @Value("\${BIDRAG_TILGANG_URL}") private val tilgangURI: URI,
    @Qualifier("azure") private val restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "tilgang") {

    val sakUri = UriComponentsBuilder.fromUri(tilgangURI).pathSegment("sak").build().toUri()
    val personUri = UriComponentsBuilder.fromUri(tilgangURI).pathSegment("person").build().toUri()
    val sporingsdataSakUri = UriComponentsBuilder.fromUri(tilgangURI).pathSegment("sak", "sporingsdata").build().toUri()
    val sporingsdataPersonUri = UriComponentsBuilder.fromUri(tilgangURI).pathSegment("person", "sporingsdata").build().toUri()

    fun sjekkTilgangSaksnummer(saksnummer: String): Boolean {
        return postForNonNullEntity(sakUri, saksnummer)
    }

    fun sjekkTilgangPerson(personIdent: String): Boolean {
        return postForNonNullEntity(personUri, personIdent)
    }

    fun hentSporingsdataSak(saksnummer: String): Sporingsdata {
        return postForNonNullEntity(sporingsdataSakUri, saksnummer)
    }

    fun hentSporingsdataPerson(personIdent: String): Sporingsdata {
        return postForNonNullEntity(sporingsdataPersonUri, personIdent)
    }
}
