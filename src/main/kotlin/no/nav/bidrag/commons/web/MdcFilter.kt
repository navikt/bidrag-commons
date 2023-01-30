package no.nav.bidrag.commons.web

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.IdUtils
import no.nav.bidrag.commons.web.MdcConstants.MDC_CALL_ID
import no.nav.bidrag.commons.web.MdcConstants.MDC_ENHET
import no.nav.bidrag.commons.web.MdcConstants.MDC_USER_ID
import org.slf4j.MDC
import org.springframework.stereotype.Component
import javax.servlet.FilterChain
import javax.servlet.http.HttpFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class MdcFilter : HttpFilter() {

  override fun doFilter(
    httpServletRequest: HttpServletRequest,
    httpServletResponse: HttpServletResponse,
    filterChain: FilterChain
  ) {

    val enhetsnummer = httpServletRequest.getHeader(EnhetFilter.X_ENHET_HEADER) ?: EnhetFilter.fetchForThread()
    val userId = TokenUtils.hentBruker()
    val callId = resolveCallId(httpServletRequest)

    MDC.put(MDC_CALL_ID, callId)
    MDC.put(MDC_USER_ID, userId)
    MDC.put(MDC_ENHET, enhetsnummer)

    httpServletResponse.setHeader(BidragHttpHeaders.NAV_CALL_ID, callId)
    httpServletResponse.setHeader(BidragHttpHeaders.X_ENHET, enhetsnummer)
    try {
      filterChain.doFilter(httpServletRequest, httpServletResponse)
    } finally {
      MDC.clear()
    }
  }

  private fun resolveCallId(httpServletRequest: HttpServletRequest): String {
    return NAV_CALL_ID_HEADER_NAMES
      .mapNotNull { httpServletRequest.getHeader(it) }
      .firstOrNull { it.isNotEmpty() }
      ?: IdUtils.generateId()
  }

  companion object {
    // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
    // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
    val NAV_CALL_ID_HEADER_NAMES =
      arrayOf(
        BidragHttpHeaders.NAV_CALL_ID,
        "Nav-CallId",
        "Nav-Callid",
        "X-Correlation-Id"
      )
  }
}
