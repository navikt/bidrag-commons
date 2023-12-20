package no.nav.bidrag.commons.security.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date

object MaskinportenTestUtils {
    fun opprettMaskinportenToken(utgarOm: Int): String {
        val privateKey = RSAKeyGenerator(2048).keyID("123").generate()

        val jwtClaimsSet = JWTClaimsSet.Builder().expirationTime(Date(Date().time + (utgarOm * 1000))).build()
        val signedJwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.keyID).build(), jwtClaimsSet)
        signedJwt.sign(RSASSASigner(privateKey))
        return signedJwt.serialize()
    }
}
