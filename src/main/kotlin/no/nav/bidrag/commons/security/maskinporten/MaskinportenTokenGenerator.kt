package no.nav.bidrag.commons.security.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date

class MaskinportenTokenGenerator(
    val maskinportenConfig: MaskinportenConfig,
) {
    internal fun genererJwtToken(scope: String): String {
        return SignedJWT(opprettJwsHeader(), generateJWTClaimSet(scope)).apply {
            sign(RSASSASigner(opprettRsaKey()))
        }.serialize()
    }

    private fun generateJWTClaimSet(scope: String): JWTClaimsSet {
        return JWTClaimsSet.Builder().apply {
            audience(maskinportenConfig.audience)
            issuer(maskinportenConfig.clientId)
            claim("scope", scope)
            issueTime(Date(Date().time))
            expirationTime(Date(Date().time + (maskinportenConfig.validInSeconds * 1000)))
        }.build()
    }

    private fun opprettJwsHeader(): JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(opprettRsaKey().keyID).build()

    private fun opprettRsaKey(): RSAKey = RSAKey.parse(maskinportenConfig.privateKey)
}
