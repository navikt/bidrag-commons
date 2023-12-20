package no.nav.bidrag.commons.security.maskinporten

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.commons.security.maskinporten.MaskinportenTestUtils.opprettMaskinportenToken
import org.junit.jupiter.api.Test

internal class MaskinportenTokenCacheTest {
    @Test
    fun `skal ikke returnere token om token utgår om under 20 sekunder`() {
        val tokenCache = MaskinportenTokenCache(opprettMaskinportenToken(19))
        tokenCache.maskinportenToken shouldBe null
    }

    @Test
    fun `skal returnere token om token har lenger gjennværende levetid enn 20 sekunder`() {
        val tokenCache = MaskinportenTokenCache(opprettMaskinportenToken(23))
        tokenCache.maskinportenToken shouldNotBe null
    }

    @Test
    fun `skal ved renew opprette nytt token i cache`() {
        val tokenCache = MaskinportenTokenCache(opprettMaskinportenToken(-10))
        tokenCache.maskinportenToken shouldBe null

        tokenCache.renew(opprettMaskinportenToken(120))
        tokenCache.maskinportenToken shouldNotBe null
    }
}
