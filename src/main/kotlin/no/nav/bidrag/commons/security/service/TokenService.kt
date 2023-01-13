package no.nav.bidrag.commons.security.service

import no.nav.bidrag.commons.security.model.TokenException
import no.nav.security.token.support.core.jwt.JwtToken

open class TokenService(var name: String = "Unknown") {
  open fun isEnabled() = false
  open fun fetchToken(): String {
    throw TokenException("Tokenservice for provider $name not initialized")
  }

  open fun fetchToken(clientRegistrationId: String, token: JwtToken?): String {
    throw TokenException("Tokenservice for provider $name not initialized")
  }

  open fun fetchToken(clientRegistrationId: String): String {
    throw TokenException("Tokenservice for provider $name not initialized")
  }
}