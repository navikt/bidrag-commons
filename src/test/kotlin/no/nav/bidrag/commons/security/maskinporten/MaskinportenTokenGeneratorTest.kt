package no.nav.bidrag.commons.security.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.math.absoluteValue

class MaskinportenTokenGeneratorTest {
    private val scope = "skatt:testscope.read"
    private val PORT = 8096
    private val TOKEN_PATH = "/token"
    private val MASKINPORTEN_MOCK_HOST = "http://localhost:$PORT"


    val maskinportenConfig = MaskinportenConfig(
        tokenUrl = MASKINPORTEN_MOCK_HOST + TOKEN_PATH,
        audience = MASKINPORTEN_MOCK_HOST,
        clientId = "17b3e4e8-8203-4463-a947-5c24021b7742",
        privateKey = RSAKeyGenerator(2048).keyID("123").generate().toString(),
        validInSeconds = 120,
        scope = "skatt:testscope.read skatt:testscope.write")


    @Test
    fun `Skal sjekke at maskonporten token er signed med privat key i config`() {
        val config = maskinportenConfig
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scope))
        val verifier: JWSVerifier = RSASSAVerifier(RSAKey.parse(config.privateKey).toRSAPublicKey())

        signedJWT.verify(verifier) shouldBe true
    }

    @Test
    fun `Skal sjekke at benyttet algorytme i header er rsa256`() {
        val config = maskinportenConfig
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scope))

        (signedJWT.header.algorithm as JWSAlgorithm).name shouldBe "RS256"
    }

    @Test
    fun `Skal sjekke at scope claims er lagt til i token body`() {
        val config = maskinportenConfig
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scope))

        signedJWT.jwtClaimsSet.audience[0] shouldBe config.audience
        signedJWT.jwtClaimsSet.issuer shouldBe config.clientId
        signedJWT.jwtClaimsSet.getStringClaim("scope") shouldBe scope
    }

    @Test
    fun `Skal sjekke at timestamps blir satt riktig på token body`() {
        val config = maskinportenConfig
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scope))

        val issuedAt = signedJWT.jwtClaimsSet.issueTime
        val expirationTime = signedJWT.jwtClaimsSet.expirationTime

        Date() likInnenEtSekund issuedAt shouldBe true
        Date() plusSekunder config.validInSeconds likInnenEtSekund expirationTime shouldBe true
    }

    private infix fun Date.likInnenEtSekund(date: Date): Boolean = (time - date.time).absoluteValue < 1000L
    private infix fun Date.plusSekunder(seconds: Int): Date = Date(time + seconds * 1000)
}