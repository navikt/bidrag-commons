package no.nav.bidrag.commons.util

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domain.ident.Ident
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.CodeSignature
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class SjekkForNyIdent(vararg val parameterNavn: String)

@Aspect
@Component
class SjekkForNyIdentAspect(private val identConsumer: IdentConsumer) {

    /**
     * Denne metoden prosesserer de tilfellene hvor @SjekkForNyIdent brukes på en funksjon.
     * @SjekkForNyIdent benyttes i disse tilfellene med parameter tilhørende navnet på verdien som ønskes å sjekkes.
     * F.eks.
     * @SjekkForNyIdent("ident1", "ident2")
     * fun fus(ident1: String, ident2: String, ident2: String) {}
     */
    @Around("@annotation(sjekkForNyIdent)")
    fun prosseserNyIdent(joinPoint: ProceedingJoinPoint, sjekkForNyIdent: SjekkForNyIdent): Any? {
        val parametere = joinPoint.args
        val codeSignature = joinPoint.signature as CodeSignature
        val parametermap: Map<String, Any> = codeSignature.parameterNames.zip(parametere).toMap()

        for (parameterNavn in sjekkForNyIdent.parameterNavn) {
            val ident = parametermap[parameterNavn]
            when (ident) {
                is PersonIdent -> {
                    if (ident.gyldig()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = PersonIdent(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is Ident -> {
                    if (ident.erPersonIdent()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = Ident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is String -> {
                    if (PersonIdent(ident).gyldig()) {
                        val parameterIndex = parametere.indexOf(ident)
                        parametere[parameterIndex] = identConsumer.sjekkIdent(ident)
                    }
                }
            }
        }
        return joinPoint.proceed(parametere)
    }

    /**
     * Denne metoden prosesserer de tilfellene hvor @SjekkForNyIdent brukes på parameteret i en funksjon.
     * @SjekkForNyIdent benyttes i disse tilfellene uten parameter.
     * F.eks.
     * fun fus(@SjekkForNyIdent ident1: String, ident2: String) {}
     */
    @Around("execution(* *(.., @SjekkForNyIdent (*), ..))")
    fun prosseserNyIdent(joinPoint: ProceedingJoinPoint): Any? {
        val parametere = joinPoint.args
        val methodSignature = joinPoint.signature as MethodSignature

        for (i in parametere.indices) {
            val ident = parametere[i]
            when (ident) {
                is PersonIdent -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        ident.gyldig()
                    ) {
                        parametere[i] = PersonIdent(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is Ident -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        ident.erPersonIdent()
                    ) {
                        parametere[i] = Ident(identConsumer.sjekkIdent(ident.verdi))
                    }
                }

                is String -> {
                    if (harSjekkForNyIdentAnnotation(methodSignature.method.parameterAnnotations[i]) &&
                        PersonIdent(ident).gyldig()
                    ) {
                        parametere[i] = identConsumer.sjekkIdent(ident)
                    }
                }
            }
        }
        return joinPoint.proceed(parametere)
    }

    private fun harSjekkForNyIdentAnnotation(annotations: Array<Annotation>): Boolean {
        return annotations.any { it is SjekkForNyIdent }
    }
}

@Component
class IdentConsumer(
    @Value("\${PERSON_URL}") private val personUrl: String,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "\${NAIS_APP_NAME}") {

    companion object {
        const val PERSON_PATH = "/personidenter"
        private val LOGGER = LoggerFactory.getLogger(SjekkForNyIdentAspect::class.java)
    }

    @Cacheable(value = ["bidrag-commons_sjekkIdent_cache"], key = "#ident")
    fun sjekkIdent(ident: String): String {
        if (Ident(ident).erPersonIdent()) {
            return try {
                restTemplate.postForEntity(
                    "$personUrl${PERSON_PATH}",
                    HentePersonidenterRequest(ident, setOf(Identgruppe.FOLKEREGISTERIDENT), false),
                    Array<PersonidentDto>::class.java
                ).body?.first()?.ident ?: ident
            } catch (e: Exception) {
                LOGGER.error("Noe gikk galt i kall mot bidrag-person: ${e.message} \n${e.stackTrace}")
                ident
            }
        }
        return ident
    }
}
