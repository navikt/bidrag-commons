package no.nav.bidrag.commons.cache

import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Annotering som indikerer at resultatet av metodekallet bare blir cachet begrenset til nåværende bruker.
 *
 * @see Cacheable
 *
 * @see BrukerCacheNøkkel
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
@Cacheable(keyGenerator = BrukerCacheNøkkel.GENERATOR_BØNNE)
annotation class BrukerCacheable(
  @get:AliasFor(annotation = Cacheable::class) vararg val value: String = [],
  @get:AliasFor(annotation = Cacheable::class) val cacheManager: String = "",
  @get:AliasFor(annotation = Cacheable::class) val cacheNames: Array<String> = []
)