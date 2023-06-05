package no.nav.bidrag.commons.tilgang

import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.util.FeltEkstraherer
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.Saksnummer
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.context.annotation.Configuration

@Aspect
@Configuration
class TilgangAdvice(
    private val tilgangClient: TilgangClient
) {

    @Before("@annotation(auditLog) ")
    fun loggTilgang(joinpoint: JoinPoint, tilgangskontroll: Tilgangskontroll) {
        if (ContextService.erMaskinTilMaskinToken()) {
            return
        }

        if (tilgangskontroll.oppslagsparameter == "") {
            auditForParameter(joinpoint.args.first())
        } else {
            auditForNavngittParameter(joinpoint, tilgangskontroll)
        }
    }

    private fun auditForNavngittParameter(joinpoint: JoinPoint, tilgangskontroll: Tilgangskontroll) {
        val parameternavn: Array<String> = (joinpoint.signature as CodeSignature).parameterNames
        val index = parameternavn.indexOf(tilgangskontroll.oppslagsparameter)
        if (index > -1) {
            auditForParameter(joinpoint.args[index])
        } else {
            finnTilgangForNavngittFeltIRequestBody(joinpoint.args.first(), tilgangskontroll.oppslagsparameter)
        }
    }

    private fun finnTilgangForNavngittFeltIRequestBody(requestBody: Any, feltnavn: String) {
        return finnTilgangForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun finnTilgangForFørsteKonstruktørparameterIRequestBody(requestBody: Any) {
        val feltnavn = FeltEkstraherer.finnNavnPåFørsteKonstruktørParameter(requestBody)
        return finnTilgangForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun auditForParameter(param: Any) {
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param)
            is PersonIdent -> sjekkTilgang(param)
            is String -> sjekkTilgangForString(param)
            else -> finnTilgangForFørsteKonstruktørparameterIRequestBody(param)
        }
    }

    private fun finnTilgangForFeltIRequestBody(requestBody: Any, feltnavn: String) {
        val param = FeltEkstraherer.finnFeltverdiForNavn(requestBody, feltnavn)
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param)
            is PersonIdent -> sjekkTilgang(param)
            is String -> sjekkTilgangForString(param)
            else -> error("Type på konstruktørparameter ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgangForString(s: String) {
        when {
            Saksnummer(s).gyldig() -> tilgangClient.sjekkTilgang(s)
            PersonIdent(s).gyldig() -> tilgangClient.sjekkTilgangPerson(s)
            else -> error("Type på oppslagsfelt ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgang(personIdent: PersonIdent) {
        tilgangClient.sjekkTilgangPerson(personIdent.verdi)
    }

    private fun sjekkTilgangTilSak(saksnummer: Saksnummer) {
        tilgangClient.sjekkTilgang(saksnummer.verdi)
    }
}
