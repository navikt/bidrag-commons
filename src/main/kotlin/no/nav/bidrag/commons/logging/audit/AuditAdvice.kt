package no.nav.bidrag.commons.logging.audit

import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.commons.util.FeltEkstraherer
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.Saksnummer
import no.nav.bidrag.transport.tilgang.Sporingsdata
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Aspect
@Configuration
@Import(AuditLogger::class)
class AuditAdvice(
    private val auditLogger: AuditLogger,
    private val tilgangClient: TilgangClient
) {

    @Before("@annotation(auditLog) ")
    fun loggTilgang(joinpoint: JoinPoint, auditLog: AuditLog) {
        if (ContextService.erMaskinTilMaskinToken()) {
            return
        }

        if (auditLog.oppslagsparameter == "") {
            auditForParameter(joinpoint.args.first(), auditLog.auditLoggerEvent)
        } else {
            auditForNavngittParameter(joinpoint, auditLog)
        }
    }

    private fun auditForNavngittParameter(joinpoint: JoinPoint, auditLog: AuditLog) {
        val parameternavn: Array<String> = (joinpoint.signature as CodeSignature).parameterNames
        val index = parameternavn.indexOf(auditLog.oppslagsparameter)
        if (index > -1) {
            auditForParameter(joinpoint.args[index], auditLog.auditLoggerEvent)
        } else {
            val sporingsdata = finnSporingsdataForNavngittFeltIRequestBody(joinpoint.args.first(), auditLog.oppslagsparameter)
            auditLogger.log(auditLog.auditLoggerEvent, sporingsdata)
        }
    }

    private fun finnSporingsdataForNavngittFeltIRequestBody(requestBody: Any, feltnavn: String): Sporingsdata {
        return finnSporingsdataForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun finnSporingsdataForFørsteKonstruktørparameterIRequestBody(requestBody: Any): Sporingsdata {
        val feltnavn = FeltEkstraherer.finnNavnPåFørsteKonstruktørParameter(requestBody)
        return finnSporingsdataForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun auditForParameter(param: Any, auditLoggerEvent: AuditLoggerEvent) {
        val sporingsdata: Sporingsdata = when (param) {
            is Saksnummer -> finnSporingsdataForSaksnummer(param)
            is PersonIdent -> finnSporingsdataForPersonIdent(param)
            is String -> finnSporingsdataForString(param)
            else -> finnSporingsdataForFørsteKonstruktørparameterIRequestBody(param)
        }
        auditLogger.log(auditLoggerEvent, sporingsdata)
    }

    private fun finnSporingsdataForFeltIRequestBody(requestBody: Any, feltnavn: String): Sporingsdata {
        val param = FeltEkstraherer.finnFeltverdiForNavn(requestBody, feltnavn)
        return when (param) {
            is Saksnummer -> finnSporingsdataForSaksnummer(param)
            is PersonIdent -> finnSporingsdataForPersonIdent(param)
            is String -> finnSporingsdataForString(param)
            else -> error("Type på konstruktørparameter ikke støttet av audit-log")
        }
    }

    private fun finnSporingsdataForString(s: String): Sporingsdata {
        return when {
            Saksnummer(s).gyldig() -> tilgangClient.hentSporingsdataSak(s)
            PersonIdent(s).gyldig() -> tilgangClient.hentSporingsdataPerson(s)
            else -> error("Type på oppslagsfelt ikke støttet av audit-log")
        }
    }

    private fun finnSporingsdataForPersonIdent(personIdent: PersonIdent): Sporingsdata {
        return tilgangClient.hentSporingsdataPerson(personIdent.verdi)
    }

    private fun finnSporingsdataForSaksnummer(saksnummer: Saksnummer): Sporingsdata {
        return tilgangClient.hentSporingsdataSak(saksnummer.verdi)
    }
}
