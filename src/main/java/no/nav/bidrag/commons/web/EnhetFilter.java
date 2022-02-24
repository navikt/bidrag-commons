package no.nav.bidrag.commons.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class EnhetFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnhetFilter.class);
  private static final ThreadLocal<String> ENHETSNUMMER_VALUE = new ThreadLocal<>();
  private static final String ENHET_MDC = "enhet";

  public static final String X_ENHET_HEADER = "X-Enhet";

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      var httpServletRequest = (HttpServletRequest) servletRequest;
      var requestURI = httpServletRequest.getRequestURI();

      if (isNotRequestToActuatorEndpoint(requestURI)) {
        var enhetsnummer = httpServletRequest.getHeader(X_ENHET_HEADER);

        if (enhetsnummer != null) {
          ENHETSNUMMER_VALUE.set(enhetsnummer);
          MDC.put(ENHET_MDC, enhetsnummer);
          ((HttpServletResponse) servletResponse).addHeader(X_ENHET_HEADER, enhetsnummer);
          LOGGER.info("Behandler request '{}' for enhet med enhetsnummer {}", requestURI, enhetsnummer);
        } else {
          LOGGER.info("Behandler request '{}' uten informasjon om enhetsnummer.", requestURI);
        }
      }
    } else {
      String filterRequest = servletRequest != null ? servletRequest.getClass().getSimpleName() : "null";
      LOGGER.error("Filtrering gjøres ikke av en HttpServletRequest: " + filterRequest);
    }

    filterChain.doFilter(servletRequest, servletResponse);
    MDC.remove(ENHET_MDC);
  }

  private boolean isNotRequestToActuatorEndpoint(String requestURI) {
    if (requestURI == null) {
      throw new IllegalStateException("should only use this class in an web environment which receives requestUri!!!");
    }

    return !requestURI.contains("/actuator/");
  }

  public static String fetchForThread() {
    return ENHETSNUMMER_VALUE.get();
  }
}
