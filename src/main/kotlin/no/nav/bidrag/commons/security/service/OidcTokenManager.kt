package no.nav.bidrag.commons.security.service

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import java.util.Optional

open class OidcTokenManager(private val tokenValidationContextHolder: TokenValidationContextHolder) {
    companion object {
        const val AZURE_ISSUER = "aad"
        const val STS_ISSUER = "sts"
    }
    open fun fetchTokenAsString(): String {
        return fetchToken().tokenAsString
    }

    private fun hasIssuers(): Boolean {
        return tokenValidationContextHolder.tokenValidationContext.issuers.isNotEmpty()
    }

    open fun isValidTokenIssuedByAzure(): Boolean {
        return hasIssuers() && tokenValidationContextHolder.tokenValidationContext.getJwtToken(AZURE_ISSUER) != null
    }

    open fun isValidTokenIssuedBySTS(): Boolean {
        return hasIssuers() && tokenValidationContextHolder.tokenValidationContext.getJwtToken(STS_ISSUER) != null
    }

    open fun getIssuer(): String {
        return fetchToken().issuer
    }

    open fun fetchToken(): JwtToken {
        return tokenValidationContextHolder.tokenValidationContext?.firstValidToken?.orElse(null) ?: throw IllegalStateException("Fant ingen gyldig token i kontekst")
    }
}