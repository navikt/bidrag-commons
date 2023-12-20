package no.nav.bidrag.commons.util

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

object Feltekstraherer {
    fun finnFeltverdiForNavn(
        entitet: Any,
        feltnavn: String,
    ): Any {
        val felt = finnFeltForFeltnavn(entitet, feltnavn) ?: error("Fant ikke felt ved navn $feltnavn i ${entitet.javaClass.name}")
        return getFeltverdi(felt, entitet)
    }

    fun finnNavnPåFørsteKonstruktørParameter(entitet: Any): String {
        val førsteKonstruktørparameter =
            konstruktørparametere(entitet)?.firstOrNull()
                ?: error("Fant ingen konstruktørparametere i ${entitet.javaClass.name}")
        return førsteKonstruktørparameter.name ?: error("Konstruktørparamet i ${entitet.javaClass.name} mangler navn.")
    }

    private fun finnFeltForFeltnavn(
        entitet: Any,
        feltnavn: String,
    ) = entitet::class.declaredMemberProperties.find { it.name == feltnavn }

    private fun konstruktørparametere(entity: Any) = entity::class.primaryConstructor?.parameters

    private fun getFeltverdi(
        felt: KProperty1<out Any, Any?>,
        entitet: Any,
    ) = felt.getter.call(entitet) ?: error("Uthenting av verdien til ${felt.name} i ${entitet.javaClass.name} feilet!")
}
