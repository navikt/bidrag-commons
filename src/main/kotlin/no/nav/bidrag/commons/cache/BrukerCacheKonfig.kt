package no.nav.bidrag.commons.cache

import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BrukerCacheKonfig {
    @Bean(BrukerCacheNøkkel.GENERATOR_BØNNE)
    fun brukerCacheNøkkelGenerator(): KeyGenerator = BrukerCacheNøkkelGenerator()
}
