package no.nav.bidrag.commons.cache

import no.nav.bidrag.commons.security.service.OidcTokenManager
import org.springframework.cache.interceptor.SimpleKeyGenerator
import java.lang.reflect.Method

class BrukerCacheNøkkelGenerator(private val oidcTokenManager: OidcTokenManager = OidcTokenManager()) : SimpleKeyGenerator() {
  companion object {
    const val SYSTEMBRUKER_ID = "SYSTEM"
  }

  override fun generate(target: Any, method: Method, vararg params: Any): Any {
    return tilBrukerCacheKey(super.generate(target, method, *params))
  }

  private fun tilBrukerCacheKey(key: Any): BrukerCacheNøkkel {
    val userId = if (oidcTokenManager.erApplikasjonBruker()) SYSTEMBRUKER_ID else oidcTokenManager.hentSaksbehandlerIdentFraToken()
    return BrukerCacheNøkkel(userId ?: "UKJENT", key)
  }
}