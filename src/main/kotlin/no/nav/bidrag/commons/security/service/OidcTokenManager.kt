package no.nav.bidrag.commons.security.service

import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

class OidcTokenManager {
    companion object {
        const val AZURE_ISSUER = "aad"
        const val IDPORTEN_ISSUER = "idporten"
        const val STS_ISSUER = "sts"
    }
    fun fetchTokenAsString(): String {
        return fetchToken().tokenAsString
    }

    private fun hasIssuers(): Boolean {
        return SpringTokenValidationContextHolder().tokenValidationContext.issuers.isNotEmpty()
    }

    fun isValidTokenIssuedByAzure(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(AZURE_ISSUER) != null
    }

    fun isValidTokenIssuedByIdporten(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(IDPORTEN_ISSUER) != null
    }

    fun isValidTokenIssuedBySTS(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(STS_ISSUER) != null
    }

    fun getIssuer(): String {
        return fetchToken().issuer
    }

    fun fetchToken(): JwtToken {
        return SpringTokenValidationContextHolder().tokenValidationContext.firstValidToken?.orElse(null) ?: throw IllegalStateException("Fant ingen gyldig token i kontekst")
    }
}