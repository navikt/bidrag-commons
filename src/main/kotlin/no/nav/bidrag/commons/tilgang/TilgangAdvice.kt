package no.nav.bidrag.commons.tilgang

import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.util.Feltekstraherer
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.Saksnummer
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpClientErrorException

@Aspect
@Configuration
@Import(TilgangClient::class)
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
        val feltnavn = Feltekstraherer.finnNavnPåFørsteKonstruktørParameter(requestBody)
        return sjekkTilgangForFeltIRequestBody(requestBody, feltnavn)
    }

    private fun sjekkForParameter(param: Any) {
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param.verdi)
            is PersonIdent -> sjekkTilgangTilPerson(param.verdi)
            is String -> sjekkTilgangForString(param)
            else -> sjekkTilgangForFørsteKonstruktørparameterIRequestBody(param)
        }
    }

    private fun sjekkTilgangForFeltIRequestBody(requestBody: Any, feltnavn: String) {
        val param = Feltekstraherer.finnFeltverdiForNavn(requestBody, feltnavn)
        when (param) {
            is Saksnummer -> sjekkTilgangTilSak(param.verdi)
            is PersonIdent -> sjekkTilgangTilPerson(param.verdi)
            is String -> sjekkTilgangForString(param)
            else -> error("Type på konstruktørparameter ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgangForString(s: String) {
        when {
            Saksnummer(s).gyldig() -> sjekkTilgangTilSak(s)
            PersonIdent(s).gyldig() -> sjekkTilgangTilPerson(s)
            else -> error("Type på oppslagsfelt ikke støttet av audit-log")
        }
    }

    private fun sjekkTilgangTilPerson(personIdent: String) {
        val tilgang = tilgangClient.sjekkTilgangPerson(personIdent)
        if (!tilgang) throw HttpClientErrorException(HttpStatusCode.valueOf(403), "Bruker har ikke tilgang til denne personen.")


    }

    private fun sjekkTilgangTilSak(saksnummer: String) {
        val tilgang = tilgangClient.sjekkTilgangSaksnummer(saksnummer)
        if (!tilgang) throw HttpClientErrorException(HttpStatusCode.valueOf(403), "Bruker har ikke tilgang til sak: $saksnummer.")
    }
}
