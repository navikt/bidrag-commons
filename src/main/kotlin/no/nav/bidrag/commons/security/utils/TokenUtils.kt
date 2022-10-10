package no.nav.bidrag.commons.security.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import org.slf4j.LoggerFactory
import java.text.ParseException

object TokenUtils {
    private val LOGGER = LoggerFactory.getLogger(TokenUtils::class.java)
    private const val ISSUER_AZURE_AD_IDENTIFIER = "login.microsoftonline.com"

    @Throws(ParseException::class)
    fun parseIdToken(idToken: String?): SignedJWT {
        return JWTParser.parse(idToken) as SignedJWT
    }

    @JvmStatic
    fun henteSubject(idToken: String): String {
        LOGGER.info("Skal finne subject fra id-token")
        return try {
            henteSubject(parseIdToken(idToken))
        } catch (var2: Exception) {
            LOGGER.error("Klarte ikke parse $idToken", var2)
            if (var2 is RuntimeException) {
                throw var2
            } else {
                throw IllegalArgumentException("Klarte ikke å parse $idToken", var2)
            }
        }
    }

    private fun henteSubject(signedJWT: SignedJWT): String {
        return try {
            if (isTokenIssuedByAzure(signedJWT)) hentSubjectIdFraAzureToken(signedJWT) else signedJWT.jwtClaimsSet.subject
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }

    @JvmStatic
    fun isTokenIssuedByAzure(signedJWT: SignedJWT): Boolean {
        return try {
            val issuer = signedJWT.jwtClaimsSet.issuer
            isTokenIssuedByAzure(issuer)
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets subject", var2)
        }
    }

    @JvmStatic
    fun isTokenIssuedByAzure(issuer: String?): Boolean {
        return issuer != null && issuer.contains("login.microsoftonline.com")
    }

    private fun henteIssuer(idToken: String?): String {
        return try {
            val t = parseIdToken(idToken)
            parseIdToken(idToken).jwtClaimsSet.issuer
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var2)
        }
    }

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

    private fun hentSubjectIdFraAzureToken(signedJWT: SignedJWT): String {
        return try {
            val claims = signedJWT.jwtClaimsSet
            val navIdent = claims.getStringClaim("NAVident")
            val application = claims.getStringClaim("azp_name")
            navIdent ?: getApplicationNameFromAzp(application)!!
        } catch (var4: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var4)
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

    fun hentSubjectIdFraAzureToken(idToken: String?): String {
        return try {
            hentSubjectIdFraAzureToken(parseIdToken(idToken))
        } catch (var2: ParseException) {
            throw IllegalStateException("Kunne ikke hente informasjon om tokenets issuer", var2)
        }
    }
}
