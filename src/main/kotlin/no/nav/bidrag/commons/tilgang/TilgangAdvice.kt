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

    @Before("@annotation(tilgangskontroll) ")
    fun sjekkTilgang(joinpoint: JoinPoint, tilgangskontroll: Tilgangskontroll) {
        if (ContextService.erMaskinTilMaskinToken()) {
            return
        }

        if (tilgangskontroll.oppslagsparameter == "") {
            sjekkForParameter(joinpoint.args.first())
        } else {
            sjekkForNavngittParameter(joinpoint, tilgangskontroll)
        }
    }

    private fun sjekkForNavngittParameter(joinpoint: JoinPoint, tilgangskontroll: Tilgangskontroll) {
        val parameternavn: Array<String> = (joinpoint.signature as CodeSignature).parameterNames
        val index = parameternavn.indexOf(tilgangskontroll.oppslagsparameter)
        if (index > -1) {
            sjekkForParameter(joinpoint.args[index])
        } else {
            sjekkTilgangForNavngittFeltIRequestBody(joinpoint.args.first(), tilgangskontroll.oppslagsparameter)
        }
    }

    private fun sjekkTilgangForNavngittFeltIRequestBody(requestBody: Any, feltnavn: String) {
        return sjekkTilgangForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun sjekkTilgangForFørsteKonstruktørparameterIRequestBody(requestBody: Any) {
        val feltnavn = FeltEkstraherer.finnNavnPåFørsteKonstruktørParameter(requestBody)
        return sjekkTilgangForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun sjekkForParameter(param: Any) {
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param)
            is PersonIdent -> sjekkTilgangTilPerson(param)
            is String -> sjekkTilgangForString(param)
            else -> sjekkTilgangForFørsteKonstruktørparameterIRequestBody(param)
        }
    }

    private fun sjekkTilgangForFeltIRequestBody(requestBody: Any, feltnavn: String) {
        val param = FeltEkstraherer.finnFeltverdiForNavn(requestBody, feltnavn)
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param)
            is PersonIdent -> sjekkTilgangTilPerson(param)
            is String -> sjekkTilgangForString(param)
            else -> error("Type på konstruktørparameter ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgangForString(s: String) {
        when {
            Saksnummer(s).gyldig() -> tilgangClient.sjekkTilgangSaksnummer(s)
            PersonIdent(s).gyldig() -> tilgangClient.sjekkTilgangPerson(s)
            else -> error("Type på oppslagsfelt ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgangTilPerson(personIdent: PersonIdent) {
        tilgangClient.sjekkTilgangPerson(personIdent.verdi)
    }

    private fun sjekkTilgangTilSak(saksnummer: Saksnummer) {
        tilgangClient.sjekkTilgangSaksnummer(saksnummer.verdi)
    }
}
