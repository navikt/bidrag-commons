package no.nav.bidrag.commons.security.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.OidcTokenManager
import org.slf4j.LoggerFactory
import java.text.ParseException

enum class TokenUtsteder {
  AZURE,
  TOKENX,
  STS,
  UKJENT
}

object TokenUtils {
  private val LOGGER = LoggerFactory.getLogger(TokenUtils::class.java)
  private const val ISSUER_AZURE_AD_IDENTIFIER = "login.microsoftonline.com"
  private const val ISSUER_TOKENX_IDENTIFIER = "tokendings"
  private const val ISSUER_IDPORTEN_IDENTIFIER = "idporten"
  private const val ISSUER_STS_IDENTIFIER = "security-token-service"
  @JvmStatic
  fun hentSaksbehandlerIdent(): String? {
    return if (!erApplikasjonBruker()) hentBruker() else null
  }
  @JvmStatic
  fun erApplikasjonBruker(): Boolean {
    return SikkerhetsKontekst.erIApplikasjonKontekst() || erApplikasjonBruker(hentToken())
  }

  @JvmStatic
  fun hentApplikasjonNavn(): String? {
       return hentToken()?.let { hentApplikasjonNavn(it) }
  }

  @JvmStatic
  fun hentBruker(): String? {
    return hentBruker(hentToken())
  }

  @JvmStatic
  fun hentApplikasjonNavn(token: String): String? {
    return try {
      hentApplikasjonNavnFraToken(konverterTokenTilJwt(token))
    } catch (var2: Exception) {
      LOGGER.error("Klarte ikke parse ${token.substring(0, token.length.coerceAtMost(10))}...", var2)
      return null
    }
  }
  @JvmStatic
  fun erTokenUtstedtAv(tokenUtsteder: TokenUtsteder): Boolean {
      return hentToken()?.let { hentTokenUtsteder(it) == tokenUtsteder } ?: false
  }

  private fun hentTokenUtsteder(token: String): TokenUtsteder {
    return try {
      val tokenJWT = konverterTokenTilJwt(token)
      if (erTokenUtstedtAvAzure(tokenJWT)) TokenUtsteder.AZURE
      else if (erTokenUtstedtAvSTS(tokenJWT)) TokenUtsteder.STS
      else if (erTokenUtstedtAvTokenX(tokenJWT)) TokenUtsteder.TOKENX
      else TokenUtsteder.UKJENT
    } catch (var5: ParseException) {
      LOGGER.error("Kunne ikke hente informasjon om tokenets issuer", var5)
      TokenUtsteder.UKJENT
    }
  }

  @JvmStatic
  private fun erApplikasjonBruker(idToken: String?): Boolean {
    return try {
      val claims = konverterTokenTilJwt(idToken).jwtClaimsSet
      val systemRessurs = "Systemressurs" == claims.getStringClaim("identType")
      val roles = claims.getStringListClaim("roles")
      val azureApp = roles != null && roles.contains("access_as_application")
      systemRessurs || azureApp
    } catch (var5: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var5)
    }
  }

  @JvmStatic
  private fun hentBruker(token: String?): String? {
    return try {
      hentBruker(konverterTokenTilJwt(token))
    } catch (var2: Exception) {
      LOGGER.error("Klarte ikke parse ${token?.substring(0, token.length.coerceAtMost(10))}...", var2)
      return null
    }
  }

  private fun erTokenUtstedtAvSTS(signedJWT: SignedJWT): Boolean {
    return try {
      val issuer = signedJWT.jwtClaimsSet.issuer
      erTokenUtstedtAvSTS(issuer)
    } catch (var2: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }
  }

  private fun erTokenUtstedtAvAzure(signedJWT: SignedJWT): Boolean {
    return try {
      val issuer = signedJWT.jwtClaimsSet.issuer
      erTokenUtstedtAvAzure(issuer)
    } catch (var2: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }
  }

  private fun erTokenUtstedtAvIdPorten(signedJWT: SignedJWT): Boolean {
    return try {
      val issuer = signedJWT.jwtClaimsSet.issuer
      val idp = signedJWT.jwtClaimsSet.getStringClaim("idp")
      erTokenUtstedtAvTokenX(issuer) && erIdPorten(idp)
    } catch (var2: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }
  }

  private fun erTokenUtstedtAvTokenX(signedJWT: SignedJWT): Boolean {
    return try {
      val issuer = signedJWT.jwtClaimsSet.issuer
      erTokenUtstedtAvTokenX(issuer)
    } catch (var2: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }
  }

  private fun erTokenUtstedtAvAzure(issuer: String?): Boolean {
    return issuer != null && issuer.contains(ISSUER_AZURE_AD_IDENTIFIER)
  }

  private fun erTokenUtstedtAvSTS(issuer: String?): Boolean {
    return issuer != null && issuer.contains(ISSUER_STS_IDENTIFIER)
  }

  private fun erIdPorten(issuer: String?): Boolean {
    return !issuer.isNullOrEmpty() && issuer.contains(ISSUER_IDPORTEN_IDENTIFIER)
  }

  private fun erTokenUtstedtAvTokenX(issuer: String?): Boolean {
    return !issuer.isNullOrEmpty() && issuer.contains(ISSUER_TOKENX_IDENTIFIER)
  }

  private fun hentApplikasjonNavnFraToken(signedJWT: SignedJWT): String? {
    return try {
      val claims = signedJWT.jwtClaimsSet
      if (erTokenUtstedtAvAzure(signedJWT)) {
        val application = claims.getStringClaim("azp_name") ?: claims.getStringClaim("azp")
        return hentApplikasjonNavnFraAzp(application)
      } else if (erTokenUtstedtAvTokenX(signedJWT)) {
        val application = claims.getStringClaim("client_id")
        return hentApplikasjonNavnFraAzp(application)
      } else claims.audience[0]
    } catch (var4: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
    }
  }

  private fun hentBrukerIdFraIdportenToken(signedJWT: SignedJWT): String? {
    return try {
      val claims = signedJWT.jwtClaimsSet
      claims.getStringClaim("pid")
    } catch (var4: ParseException) {
      throw IllegalStateException("Kunne ikke hente personid fra idporten tokenr", var4)
    }
  }

  private fun hentBrukerIdFraAzureToken(signedJWT: SignedJWT): String? {
    return try {
      val claims = signedJWT.jwtClaimsSet
      val navIdent = claims.getStringClaim("NAVident")
      val application = claims.getStringClaim("azp_name")
      navIdent ?: hentApplikasjonNavnFraAzp(application)
    } catch (var4: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
    }
  }

  private fun hentBruker(signedJWT: SignedJWT): String? {
    return try {
      if (erTokenUtstedtAvAzure(signedJWT)) hentBrukerIdFraAzureToken(signedJWT)
      else if (erTokenUtstedtAvIdPorten(signedJWT)) hentBrukerIdFraIdportenToken(signedJWT)
      else signedJWT.jwtClaimsSet.subject
    } catch (var2: ParseException) {
      throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
    }
  }

  private fun hentApplikasjonNavnFraAzp(azpName: String?): String? {
    return if (azpName == null) {
      null
    } else {
      val azpNameSplit = azpName.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()
      azpNameSplit[azpNameSplit.size - 1]
    }
  }

  private fun hentToken(): String? = try {
    OidcTokenManager().hentToken()
  } catch (_: Exception) { null }

  private fun konverterTokenTilJwt(idToken: String?): SignedJWT {
    return JWTParser.parse(idToken) as SignedJWT
  }
}
