package no.nav.bidrag.commons.service.organisasjon

import no.nav.bidrag.commons.service.AppContext
import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(value = [BidragOrganisasjonConsumer::class, AppContext::class])
annotation class EnableSaksbehandlernavnProvider()
