package no.nav.bidrag.commons.security.service

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import java.util.Optional

class OidcTokenManager(private val tokenValidationContextHolder: TokenValidationContextHolder) {
    companion object {
        const val AZURE_ISSUER = "aad"
        const val ISSO_ISSUER = "isso"
        const val STS_ISSUER = "sts"
    }
    fun fetchTokenAsString(): String {
        return fetchToken().tokenAsString
    }

    fun isValidTokenIssuedByAzure(): Boolean {
        return tokenValidationContextHolder.tokenValidationContext.getJwtToken(AZURE_ISSUER) != null
    }

    fun isValidTokenIssuedBySTS(): Boolean {
        return tokenValidationContextHolder.tokenValidationContext.getJwtToken(STS_ISSUER) != null
    }

    fun isValidTokenIssuedByISSO(): Boolean {
        return tokenValidationContextHolder.tokenValidationContext.getJwtToken(ISSO_ISSUER) != null
    }

    fun fetchToken(): JwtToken {
        return Optional.ofNullable<TokenValidationContextHolder>(tokenValidationContextHolder)
            .map { it.tokenValidationContext }
            .map { it.firstValidToken }
            .map { it.get() }
            .orElseThrow { IllegalStateException("Kunne ikke videresende Bearer token") }
    }
}