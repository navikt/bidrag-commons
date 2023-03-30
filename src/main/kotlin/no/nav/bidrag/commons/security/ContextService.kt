package no.nav.bidrag.commons.security

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object ContextService {

    fun hentPåloggetSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
            .fold(
                onSuccess = { it.getClaims("aad")?.get("NAVident")?.toString() ?: error("Finner ikke NAVident i token") },
                onFailure = { error("Finner ikke NAVident i token") }
            )
    }

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().tokenValidationContext.getClaims("aad")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }
}
