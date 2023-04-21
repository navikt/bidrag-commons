package no.nav.bidrag.commons.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import no.nav.bidrag.commons.security.utils.TokenUtils
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Adds user to MDC for logging
 */
@Component
class UserMdcFilter : Filter {

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val user = TokenUtils.hentBruker()
        val appName = TokenUtils.hentApplikasjonsnavn()
        user?.apply { MDC.put(USER_MDC, user) }
        appName?.apply { MDC.put(APP_NAME_MDC, appName) }

        filterChain.doFilter(servletRequest, servletResponse)
        MDC.clear()
    }

    companion object {
        private const val USER_MDC = "user"
        private const val APP_NAME_MDC = "applicationKey"
    }
}
