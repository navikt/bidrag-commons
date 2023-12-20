package no.nav.bidrag.commons.tilgang

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Tilgangskontroll(
    // Kun nødvendig hvis det ikke er første verdi i parmetere eller body som er nøkkelentitet.
    val oppslagsparameter: String = "",
)
