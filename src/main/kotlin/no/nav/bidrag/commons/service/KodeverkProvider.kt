@file:Suppress("unused")

package no.nav.bidrag.commons.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

private val kodeverkUrl = AtomicReference("")
const val SUMMERT_SKATTEGRUNNLAG = "Summert skattegrunnlag"
const val POSTNUMMER = "Postnummer"
const val LOENNSBESKRIVELSE = "Loennsbeskrivelse"
private val kodeverkCache: Cache<String, KodeverkKoderBetydningerResponse> = Caffeine.newBuilder()
    .maximumSize(1000).expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
    .build()
private val log = LoggerFactory.getLogger(KodeverkProvider::class.java)
fun finnVisningsnavnSkattegrunnlag(fulltNavnInntektspost: String): String = finnVisningsnavn(fulltNavnInntektspost, SUMMERT_SKATTEGRUNNLAG) ?: ""
fun finnPoststedForPostnummer(postnummer: String): String? = finnVisningsnavn(postnummer, POSTNUMMER)
fun finnVisningsnavnLønnsbeskrivelse(fulltNavnInntektspost: String): String = finnVisningsnavn(fulltNavnInntektspost, LOENNSBESKRIVELSE) ?: ""
class KodeverkProvider {

    companion object {
        fun initialiser(url: String) {
            kodeverkUrl.set(url)
        }

        fun initialiserKodeverkCache() {
            kodeverkCache.get(SUMMERT_SKATTEGRUNNLAG) { hentKodeverk(SUMMERT_SKATTEGRUNNLAG) }
            kodeverkCache.get(LOENNSBESKRIVELSE) { hentKodeverk(LOENNSBESKRIVELSE) }
            kodeverkCache.get(POSTNUMMER) { hentKodeverk(POSTNUMMER) }
        }
    }
}

private fun finnVisningsnavn(fulltNavnInntektspost: String, kodeverk: String): String? {
    val betydning = kodeverkCache
        .get(kodeverk) { hentKodeverk(kodeverk) }
        .betydninger[fulltNavnInntektspost]?.firstNotNullOf { betydning -> betydning.beskrivelser["nb"] }
    return if (betydning?.tekst.isNullOrEmpty()) betydning?.term else betydning?.tekst
}

private fun hentKodeverk(kodeverk: String): KodeverkKoderBetydningerResponse {
    val kodeverkContext = "${kodeverkUrl.get()}/api/v1/kodeverk/$kodeverk/koder/betydninger?ekskluderUgyldige=true&spraak=nb"
    val restTemplate: RestTemplate = RestTemplateBuilder()
        .defaultHeader("Nav-Call-Id", CorrelationId.fetchCorrelationIdForThread())
        .defaultHeader("Nav-Consumer-Id", System.getenv("NAIS_APP_NAME") ?: "bidrag-commons")
        .build()
    log.info("Laster kodeverk for $kodeverk")
    return restTemplate.getForEntity<KodeverkKoderBetydningerResponse>(kodeverkContext).body!!
}

internal class KodeverkKoderBetydningerResponse {
    var betydninger: Map<String, List<Betydning>> = emptyMap()
        set(betydninger) {
            field = LinkedHashMap(betydninger)
        }
}

internal data class Betydning(
    val gyldigFra: LocalDate,
    val gyldigTil: LocalDate,
    val beskrivelser: Map<String, Beskrivelse>
)

internal data class Beskrivelse(
    val tekst: String,
    val term: String
)
