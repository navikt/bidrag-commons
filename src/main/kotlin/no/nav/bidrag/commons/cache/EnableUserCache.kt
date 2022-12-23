package no.nav.bidrag.commons.cache

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(BrukerCacheKonfig::class)
annotation class EnableUserCache