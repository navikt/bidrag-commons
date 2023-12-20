package no.nav.bidrag.commons.security.maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.nimbusds.jwt.SignedJWT
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse.BodyHandlers.ofString

/**
 * Maskinporten brukes for å sikre autentisering og tilgangskontroll for datautveksling mellom ulike virksomheter.
 *
 * For å ta i bruk klienten i din Nais applikasjon trengs følgene config:
 *
 * maskinporten:
 *   tokenUrl: ${MASKINPORTEN_ISSUER}token
 *   audience: ${MASKINPORTEN_ISSUER}
 *   clientId: ${MASKINPORTEN_CLIENT_ID}
 *   scope: ${MASKINPORTEN_SCOPES}
 *   privateKey: ${MASKINPORTEN_CLIENT_JWK}
 *   validInSeconds: 120 #120 er maks antall sekunder et maskinporten Jwt-token kan være gyldig.
 *
 *   Følgende må være med i nais.yaml for at Nais skal opprette miljøvariablene MASKINPORTEN_ISSUER,
 *   MASKINPORTEN_CLIENT_ID, MASKINPORTEN_SCOPES og MASKINPORTEN_CLIENT_JWK.
 *
 *   spec:
 *      maskinporten:
 *          enabled: true
 */

@EnableConfigurationProperties(MaskinportenConfig::class)
@Service("maskinportenClient")
class MaskinportenClient(
    private val maskinportenConfig: MaskinportenConfig,
) {
    companion object {
        internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        internal const val CONTENT_TYPE = "application/x-www-form-urlencoded"
    }

    private val maskinportenTokenGenerator = MaskinportenTokenGenerator(maskinportenConfig)

    private val httpClient = HttpClient.newBuilder().build()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    private val maskinportenTokenCache: LoadingCache<String, MaskinportenTokenCache> =
        Caffeine.newBuilder().build { scope: String ->
            MaskinportenTokenCache(hentNyttJwtToken(scope))
        }

    fun hentMaskinportenToken(scope: String): SignedJWT {
        val cache =
            maskinportenTokenCache.get(scope) { nyttScope: String ->
                MaskinportenTokenCache(hentNyttJwtToken(nyttScope))
            } ?: error(
                "Feil ved henting eller opprettelse av cached scope for maskinporten-token! " +
                    "Scope: $scope, cache content: $maskinportenTokenCache",
            )
        return cache.run {
            maskinportenToken ?: renew(hentNyttJwtToken(scope))
        }
    }

    fun hentMaskinportenToken(): SignedJWT {
        val cache =
            maskinportenTokenCache.get(maskinportenConfig.scope) { nyttScope: String ->
                MaskinportenTokenCache(hentNyttJwtToken(nyttScope))
            } ?: error(
                "Feil ved henting eller opprettelse av cached scope for maskinporten-token! " +
                    "Scope: ${maskinportenConfig.scope}, cache content: $maskinportenTokenCache",
            )
        return cache.run {
            maskinportenToken ?: renew(hentNyttJwtToken(maskinportenConfig.scope))
        }
    }

    private fun hentNyttJwtToken(scope: String): String =
        httpClient.send(opprettMaskinportenTokenRequest(scope), ofString()).run {
            if (statusCode() != 200) {
                throw MaskinportenClientException(
                    "Feil ved henting av token: Status: ${statusCode()} , Body: ${body()}",
                )
            }
            mapTilMaskinportenResponseBody(body()).access_token
        }

    private fun mapTilMaskinportenResponseBody(body: String): MaskinportenTokenResponse =
        try {
            objectMapper.readValue(body)
        } catch (e: Exception) {
            throw MaskinportenClientException("Feil ved deserialisering av response fra maskinporten: $e.message")
        }

    private fun opprettMaskinportenTokenRequest(scope: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(maskinportenConfig.tokenUrl)).header("Content-Type", CONTENT_TYPE)
            .POST(ofString(opprettRequestBody(maskinportenTokenGenerator.genererJwtToken(scope)))).build()

    private fun opprettRequestBody(jwt: String) = "grant_type=$GRANT_TYPE&assertion=$jwt"
}
