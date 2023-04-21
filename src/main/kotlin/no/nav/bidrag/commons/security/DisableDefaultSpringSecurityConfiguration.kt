package no.nav.bidrag.commons.security

import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity
import org.springframework.security.config.annotation.web.WebSecurityConfigurer
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@ConditionalOnDefaultWebSecurity
@EnableWebSecurity
class DisableDefaultSpringSecurityConfiguration : WebSecurityConfigurer<WebSecurity> {

    override fun init(builder: WebSecurity) {
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().requestMatchers("/**")
    }
}
