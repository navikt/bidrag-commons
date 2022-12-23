package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

class OidcTokenManager {
    companion object {
        const val ISSO_ISSUER = "isso"
        const val AZURE_ISSUER = "aad"
        const val TOKENX_ISSUER = "tokenx"
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

    fun isValidTokenIssuedByTokenX(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(TOKENX_ISSUER) != null
    }

    fun isValidTokenIssuedByOpenAm(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(ISSO_ISSUER) != null
    }

    fun isValidTokenIssuedBySTS(): Boolean {
        return hasIssuers() && SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken(STS_ISSUER) != null
    }
    
    private fun getToken(): String? {
        if (SpringTokenValidationContextHolder().tokenValidationContext.hasValidToken()){
            return SpringTokenValidationContextHolder().tokenValidationContext.firstValidToken.get().tokenAsString
        }
        return null
    }

    fun hentSaksbehandlerIdentFraToken(): String? {
        return getToken()?.let { TokenUtils.fetchSubject(it) }
    }
    
    fun erApplikasjonBruker(): Boolean {
        return TokenUtils.isSystemUser(getToken())
    }

    fun getIssuer(): String {
        return fetchToken().issuer
    }

    fun fetchToken(): JwtToken {
        return SpringTokenValidationContextHolder().tokenValidationContext.firstValidToken?.orElse(null) ?: throw IllegalStateException("Fant ingen gyldig token i kontekst")
    }
}