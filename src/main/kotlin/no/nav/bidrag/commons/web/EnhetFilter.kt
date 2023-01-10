package no.nav.bidrag.commons.web

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class EnhetFilter : Filter {

  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    if (servletRequest is HttpServletRequest) {
      val requestURI = servletRequest.requestURI
      if (isNotRequestToActuatorEndpoint(requestURI)) {
        val enhetsnummer = servletRequest.getHeader(X_ENHET_HEADER)
        if (enhetsnummer != null) {
          ENHETSNUMMER_VALUE.set(enhetsnummer)
          MDC.put(ENHET_MDC, enhetsnummer)
          (servletResponse as HttpServletResponse).addHeader(X_ENHET_HEADER, enhetsnummer)
          LOGGER.info("Behandler request '{}' for enhet med enhetsnummer {}", requestURI, enhetsnummer)
        } else {
          ENHETSNUMMER_VALUE.set(null)
          LOGGER.info("Behandler request '{}' uten informasjon om enhetsnummer.", requestURI)
        }
      }
    } else {
      val filterRequest = servletRequest.javaClass.simpleName
      LOGGER.error("Filtrering gjøres ikke av en HttpServletRequest: $filterRequest")
    }
    filterChain.doFilter(servletRequest, servletResponse)
    MDC.clear()
  }

  private fun isNotRequestToActuatorEndpoint(requestURI: String?): Boolean {
    checkNotNull(requestURI) { "should only use this class in an web environment which receives requestUri!!!" }
    return !requestURI.contains("/actuator/")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EnhetFilter::class.java)
    private val ENHETSNUMMER_VALUE = ThreadLocal<String>()
    private const val ENHET_MDC = "enhet"
    const val X_ENHET_HEADER = "X-Enhet"
    fun fetchForThread(): String {
      return ENHETSNUMMER_VALUE.get()
    }
  }
}