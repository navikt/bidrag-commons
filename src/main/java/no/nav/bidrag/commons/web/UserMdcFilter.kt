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
class UserMdcFilter(var oidcTokenManager: OidcTokenManager) : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
       val user = try {
           TokenUtils.fetchSubject(oidcTokenManager.fetchTokenAsString())
        } catch (e: Exception){
            "UKJENT"
        }

        MDC.put(USER_MDC, user)
        filterChain.doFilter(servletRequest, servletResponse)
        MDC.clear()
    }

    companion object {
        private const val USER_MDC = "user"
    }
}