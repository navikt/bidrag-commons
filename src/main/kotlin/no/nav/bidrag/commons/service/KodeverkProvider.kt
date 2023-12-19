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
const val POSTNUMMER = "Postnummer"
const val SUMMERT_SKATTEGRUNNLAG = "Summert skattegrunnlag"
const val LOENNSBESKRIVELSE = "Loennsbeskrivelse"
const val YTELSEFRAOFFENTLIGE = "YtelseFraOffentligeBeskrivelse"
const val PENSJONELLERTRYGDEBESKRIVELSE = "PensjonEllerTrygdeBeskrivelse"
const val NAERINGSINNTEKTSBESKRIVELSE = "Naeringsinntektsbeskrivelse"
private val kodeverkCache: Cache<String, KodeverkKoderBetydningerResponse> = Caffeine.newBuilder()
    .maximumSize(1000).expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
    .build()
private val log = LoggerFactory.getLogger(KodeverkProvider::class.java)
fun finnVisningsnavnSkattegrunnlag(fulltNavnInntektspost: String): String = finnVisningsnavn(fulltNavnInntektspost, SUMMERT_SKATTEGRUNNLAG) ?: ""
fun finnPoststedForPostnummer(postnummer: String): String? = finnVisningsnavn(postnummer, POSTNUMMER)
fun finnVisningsnavnLønnsbeskrivelse(fulltNavnInntektspost: String): String = finnVisningsnavn(fulltNavnInntektspost, LOENNSBESKRIVELSE) ?: ""
fun finnVisningsnavnKodeverk(fulltNavnInntektspost: String, kodeverk: String): String = finnVisningsnavn(fulltNavnInntektspost, kodeverk) ?: ""

fun finnVisningsnavn(fulltNavnInntektspost: String): String {
    return finnVisningsnavn(fulltNavnInntektspost, SUMMERT_SKATTEGRUNNLAG)
        ?: finnVisningsnavn(fulltNavnInntektspost, LOENNSBESKRIVELSE)
        ?: finnVisningsnavn(fulltNavnInntektspost, YTELSEFRAOFFENTLIGE)
        ?: finnVisningsnavn(fulltNavnInntektspost, PENSJONELLERTRYGDEBESKRIVELSE)
        ?: finnVisningsnavn(fulltNavnInntektspost, NAERINGSINNTEKTSBESKRIVELSE)
        ?: ""
}

class KodeverkProvider {

    companion object {
        fun initialiser(url: String) {
            kodeverkUrl.set(url)
        }

        fun initialiserKodeverkCache() {
            kodeverkCache.get(SUMMERT_SKATTEGRUNNLAG) { hentKodeverk(SUMMERT_SKATTEGRUNNLAG) }
            kodeverkCache.get(LOENNSBESKRIVELSE) { hentKodeverk(LOENNSBESKRIVELSE) }
            kodeverkCache.get(YTELSEFRAOFFENTLIGE) { hentKodeverk(YTELSEFRAOFFENTLIGE) }
            kodeverkCache.get(PENSJONELLERTRYGDEBESKRIVELSE) { hentKodeverk(PENSJONELLERTRYGDEBESKRIVELSE) }
            kodeverkCache.get(NAERINGSINNTEKTSBESKRIVELSE) { hentKodeverk(NAERINGSINNTEKTSBESKRIVELSE) }
            kodeverkCache.get(POSTNUMMER) { hentKodeverk(POSTNUMMER) }
        }

        fun invaliderKodeverkCache() {
            kodeverkCache.invalidate(SUMMERT_SKATTEGRUNNLAG)
            kodeverkCache.invalidate(LOENNSBESKRIVELSE)
            kodeverkCache.invalidate(YTELSEFRAOFFENTLIGE)
            kodeverkCache.invalidate(PENSJONELLERTRYGDEBESKRIVELSE)
            kodeverkCache.invalidate(NAERINGSINNTEKTSBESKRIVELSE)
            kodeverkCache.invalidate(POSTNUMMER)
        }
    }
}

private fun finnVisningsnavn(kode: String, kodeverk: String): String? {
    val betydning = kodeverkCache
        .get(kodeverk) { hentKodeverk(kodeverk) }
        .betydninger[kode]?.firstNotNullOf { betydning -> betydning.beskrivelser["nb"] }
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

data class KodeverkKoderBetydningerResponse(
    val betydninger: Map<String, List<KodeverkBetydning>>
)

data class KodeverkBetydning(
    val gyldigFra: LocalDate,
    val gyldigTil: LocalDate,
    val beskrivelser: Map<String, KodeverkBeskrivelse>
)

data class KodeverkBeskrivelse(
    val tekst: String,
    val term: String
)
