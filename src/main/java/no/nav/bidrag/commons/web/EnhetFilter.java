package no.nav.bidrag.commons.web;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhetFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnhetFilter.class);
  public static final String X_ENHETSNR_HEADER = "X-Enhetsnummer";

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
    if (servletRequest instanceof HttpServletRequest) {
      var httpServletRequest = (HttpServletRequest) servletRequest;
      var enhetsnummer = httpServletRequest.getHeader(X_ENHETSNR_HEADER);

      if (enhetsnummer != null) {
        ((HttpServletResponse) servletResponse).addHeader(X_ENHETSNR_HEADER, enhetsnummer);
        LOGGER.info("Behandler request '{}' for enhet med enhetsnummer {}", httpServletRequest.getRequestURI(), enhetsnummer);
      } else {
        LOGGER.info("Behandler request '{}' uten informasjon om enhetsnummer.", httpServletRequest.getRequestURI());
      }
    }
  }
}
