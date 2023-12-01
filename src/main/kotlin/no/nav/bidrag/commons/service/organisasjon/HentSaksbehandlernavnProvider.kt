@file:Suppress("unused")

package no.nav.bidrag.commons.service.organisasjon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.LoggingRetryListener
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * SaksbehandlernavnProvider brukes for å hente saksbehandlernavn for NAV-ident.
 *
 * For å ta i bruker provideren må følgende annotering legges til i konfigurasjon
 *
 * ```kotlin
 * @EnableSaksbehandlernavnProvider
 * ```
 *
 * Og følgende må legges til i application.yaml
 *
 * ```yaml
 * .....
 * no.nav.security.jwt:
 *   client:
 *     registration:
 *       bidrag-organisasjon:
 *         resource-url: ${BIDRAG_ORGANISASJON_URL}
 *         token-endpoint-url: https://login.microsoftonline.com/${AZURE_APP_TENANT_ID}/oauth2/v2.0/token
 *         grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer
 *         scope: api://${BIDRAG_ORGANISASJON_SCOPE}/.default
 *         authentication:
 *           client-id: ${AZURE_APP_CLIENT_ID}
 *           client-secret: ${AZURE_APP_CLIENT_SECRET}
 *           client-auth-method: client_secret_post
 *
 * ......
 * ```
 *
 * Hvor følgende miljøvariabler er definert i nais konfigurasjonen og outbound traffik er tillatt for BIDRAG_ORGANISASJON_URL
 * ```yaml
 *   BIDRAG_ORGANISASJON_URL: https://bidrag-organisasjon.<cluster>-pub.nais.io/bidrag-organisasjon
 *   BIDRAG_ORGANISASJON_SCOPE: <cluster>.bidrag.bidrag-organisasjon
 * ```
 */

@Component("CommonsBidragOrganisasjonConsumer")
internal class BidragOrganisasjonConsumer(
    @Value("\${BIDRAG_ORGANISASJON_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "bidrag-organisasjon") {
    private fun createUri(path: String?) = UriComponentsBuilder.fromUri(url)
        .path(path ?: "").build().toUri()

    @Cacheable(SaksbehandlernavnProvider.SAKSBEHANDLERINFO_CACHE)
    fun hentSaksbehandlerInfo(saksbehandlerIdent: String): SaksbehandlerInfoResponse? {
        return getForEntity(createUri("/saksbehandler/info/$saksbehandlerIdent"))
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SaksbehandlerInfoResponse(val ident: String, val navn: String)
class SaksbehandlernavnProvider {

    companion object {
        const val SAKSBEHANDLERINFO_CACHE = "SAKSBEHANDLERINFO_CACHE"

        /**
         * Hent navn på personen som tilhører en NAV-ident (feks Z994977)
         */
        fun hentSaksbehandlernavn(saksbehandlerIdent: String): String? {
            return try {
                retryTemplate("SaksbehandlernavnProvider.hentSaksbehandlernavn for ident $saksbehandlerIdent").execute<String, HttpClientErrorException> {
                    AppContext.getBean("CommonsBidragOrganisasjonConsumer", BidragOrganisasjonConsumer::class.java)
                        .hentSaksbehandlerInfo(saksbehandlerIdent)?.navn
                }
            } catch (e: Exception) {
                secureLogger.error("Det skjedde en feil ved henting av saksbehandlernavn for saksbehandler $saksbehandlerIdent", e)
                null
            }
        }
    }
}

private fun retryTemplate(details: String? = null): RetryTemplate {
    val retryTemplate = RetryTemplate()
    val fixedBackOffPolicy = FixedBackOffPolicy()
    fixedBackOffPolicy.backOffPeriod = 500L
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy)
    val retryPolicy = SimpleRetryPolicy()
    retryPolicy.maxAttempts = 3
    retryTemplate.setRetryPolicy(retryPolicy)
    retryTemplate.setThrowLastExceptionOnExhausted(true)
    retryTemplate.registerListener(LoggingRetryListener(details))
    return retryTemplate
}
