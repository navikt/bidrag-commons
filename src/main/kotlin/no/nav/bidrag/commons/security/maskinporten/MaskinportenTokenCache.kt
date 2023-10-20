package no.nav.bidrag.commons.security.maskinporten

import com.nimbusds.jwt.SignedJWT
import java.util.Date

class MaskinportenTokenCache(maskinportenToken: String) {
    internal var maskinportenToken = SignedJWT.parse(maskinportenToken)
        get() = field?.takeUnless { it.isExpired }

    private val SignedJWT.isExpired: Boolean
        get() = jwtClaimsSet?.expirationTime?.is20SecondsPrior ?: false

    private val Date.is20SecondsPrior: Boolean
        get() = epochSeconds - (Date().epochSeconds + TWENTY_SECONDS) < 0

    private val Date.epochSeconds: Long
        get() = time / 1000

    internal fun renew(newToken: String) = SignedJWT.parse(newToken).also {
        maskinportenToken = it
    }

    companion object {
        private const val TWENTY_SECONDS = 20
    }
}
