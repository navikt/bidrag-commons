package no.nav.bidrag.commons.web

import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.utils.TokenUtils
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * Adds user to MDC for logging
 */
@Component
class UserMdcFilter : Filter {

    val oidcTokenManager: OidcTokenManager = OidcTokenManager()
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val token = try {oidcTokenManager.fetchTokenAsString()} catch (_: Exception) { null }
        val user = token?.let { TokenUtils.fetchSubject(it) }
        val appName = token?.let { TokenUtils.fetchAppName(it) }
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