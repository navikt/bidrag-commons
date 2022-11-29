package no.nav.bidrag.commons.security.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import org.slf4j.LoggerFactory
import java.text.ParseException

enum class TokenIssuer {
    AZURE,
    TOKENX,
    STS,
    UNKOWN
}
object TokenUtils {
    private val LOGGER = LoggerFactory.getLogger(TokenUtils::class.java)
    private const val ISSUER_AZURE_AD_IDENTIFIER = "login.microsoftonline.com"
    private const val ISSUER_TOKENX_IDENTIFIER = "tokendings"
    private const val ISSUER_IDPORTEN_IDENTIFIER = "idporten"
    private const val ISSUER_STS_IDENTIFIER = "security-token-service"

    @JvmStatic
    fun fetchSubject(token: String): String? {
        LOGGER.debug("Skal finne subject fra id-token")
        return try {
            fetchSubject(parseIdToken(token))
        } catch (var2: Exception) {
            LOGGER.error("Klarte ikke parse ${token.substring(0, token.length.coerceAtMost(10))}...", var2)
            return null
        }
    }

    @JvmStatic
    fun fetchAppName(token: String): String? {
        return try {
            fetchAppNameFromToken(parseIdToken(token))
        } catch (var2: Exception) {
            LOGGER.error("Klarte ikke parse ${token.substring(0, token.length.coerceAtMost(10))}...", var2)
            return null
        }
    }

    @JvmStatic
    fun isSystemUser(idToken: String?): Boolean {
        return try {
            val claims = parseIdToken(idToken).jwtClaimsSet
            val systemRessurs = "Systemressurs" == claims.getStringClaim("identType")
            val roles = claims.getStringListClaim("roles")
            val azureApp = roles != null && roles.contains("access_as_application")
            systemRessurs || azureApp
        } catch (var5: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var5)
        }
    }

    @JvmStatic
    fun fetchTokenIssuer(token: String): TokenIssuer {
        return try {
            val tokenJWT = parseIdToken(token)
            if (isTokenIssuedByAzure(tokenJWT)) TokenIssuer.AZURE
            else if (isTokenIssuedBySTS(tokenJWT)) TokenIssuer.STS
            else if (isTokenIssuedByTokenX(tokenJWT)) TokenIssuer.TOKENX
            else TokenIssuer.UNKOWN
        } catch (var5: ParseException) {
            LOGGER.error("Kunne ikke hente informasjon om tokenets issuer", var5)
            TokenIssuer.UNKOWN
        }
    }

    private fun isTokenIssuedBySTS(signedJWT: SignedJWT): Boolean {
        return try {
            val issuer = signedJWT.jwtClaimsSet.issuer
            isTokenIssuedBySTS(issuer)
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }

    private fun isTokenIssuedByAzure(signedJWT: SignedJWT): Boolean {
        return try {
            val issuer = signedJWT.jwtClaimsSet.issuer
            isTokenIssuedByAzure(issuer)
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }
    private fun isTokenProviderIdPorten(signedJWT: SignedJWT): Boolean {
        return try {
            val issuer = signedJWT.jwtClaimsSet.issuer
            val idp = signedJWT.jwtClaimsSet.getStringClaim("idp")
            isTokenIssuedByTokenX(issuer) && isIdPorten(idp)
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }

    private fun isTokenIssuedByTokenX(signedJWT: SignedJWT): Boolean {
        return try {
            val issuer = signedJWT.jwtClaimsSet.issuer
            isTokenIssuedByTokenX(issuer)
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }
    private fun isTokenIssuedByAzure(issuer: String?): Boolean {
        return issuer != null && issuer.contains(ISSUER_AZURE_AD_IDENTIFIER)
    }

    private fun isTokenIssuedBySTS(issuer: String?): Boolean {
        return issuer != null && issuer.contains(ISSUER_STS_IDENTIFIER)
    }

    private fun isIdPorten(issuer: String?): Boolean {
        return !issuer.isNullOrEmpty() && issuer.contains(ISSUER_IDPORTEN_IDENTIFIER)
    }

    private fun isTokenIssuedByTokenX(issuer: String?): Boolean {
        return !issuer.isNullOrEmpty() && issuer.contains(ISSUER_TOKENX_IDENTIFIER)
    }

    private fun fetchAppNameFromToken(signedJWT: SignedJWT): String? {
        return try {
            val claims = signedJWT.jwtClaimsSet
            if (isTokenIssuedByAzure(signedJWT)){
                val application = claims.getStringClaim("azp_name") ?: claims.getStringClaim("azp")
                return getApplicationNameFromAzp(application)
            } else {
                claims.audience[0]
            }
        } catch (var4: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
        }
    }

    private fun fetchSubjectIdFromIdportenToken(signedJWT: SignedJWT): String? {
        return try {
            val claims = signedJWT.jwtClaimsSet
            claims.getStringClaim("pid")
        } catch (var4: ParseException) {
            throw IllegalStateException("Kunne ikke hente personid fra idporten tokenr", var4)
        }
    }

    private fun fetchSubjectIdFromAzureToken(signedJWT: SignedJWT): String? {
        return try {
            val claims = signedJWT.jwtClaimsSet
            val navIdent = claims.getStringClaim("NAVident")
            val application = claims.getStringClaim("azp_name")
            navIdent ?: getApplicationNameFromAzp(application)
        } catch (var4: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
        }
    }

    private fun fetchSubject(signedJWT: SignedJWT): String? {
        return try {
            if (isTokenIssuedByAzure(signedJWT)) fetchSubjectIdFromAzureToken(signedJWT)
            else if (isTokenProviderIdPorten(signedJWT)) fetchSubjectIdFromIdportenToken(signedJWT)
            else signedJWT.jwtClaimsSet.subject
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }

    private fun getApplicationNameFromAzp(azpName: String?): String? {
        return if (azpName == null) {
            null
        } else {
            val azpNameSplit = azpName.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            azpNameSplit[azpNameSplit.size - 1]
        }
    }

    @Throws(ParseException::class)
    private fun parseIdToken(idToken: String?): SignedJWT {
        return JWTParser.parse(idToken) as SignedJWT
    }
}