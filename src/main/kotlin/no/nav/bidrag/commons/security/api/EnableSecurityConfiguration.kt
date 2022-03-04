package no.nav.bidrag.commons.security.api

import no.nav.bidrag.commons.security.SecurityConfig
import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(SecurityConfig::class)
annotation class EnableSecurityConfiguration(
)