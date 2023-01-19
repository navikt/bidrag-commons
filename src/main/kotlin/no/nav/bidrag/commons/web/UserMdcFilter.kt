package no.nav.bidrag.commons.web

import no.nav.bidrag.commons.security.utils.TokenUtils
import org.slf4j.MDC
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * Adds user to MDC for logging
 */
@Component
class UserMdcFilter : Filter {

  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val user = TokenUtils.hentBruker()
    val appName = TokenUtils.hentApplikasjonNavn()
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