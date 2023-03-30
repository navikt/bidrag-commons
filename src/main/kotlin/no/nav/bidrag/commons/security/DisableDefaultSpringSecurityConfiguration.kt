package no.nav.bidrag.commons.security

import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@ConditionalOnDefaultWebSecurity
@EnableWebSecurity
class DisableDefaultSpringSecurityConfiguration : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers("/**")
    }
}
