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

public class EnhetFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnhetFilter.class);
  private static final ThreadLocal<String> ENHETSNUMMER_VALUE = new ThreadLocal<>();

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
          ((HttpServletResponse) servletResponse).addHeader(X_ENHET_HEADER, enhetsnummer);
          LOGGER.info("{} behandler request '{}' for enhet med enhetsnummer {}", EnhetFilter.class.getSimpleName(), requestURI, enhetsnummer);
        } else {
          LOGGER.info("{} bBehandler request '{}' uten informasjon om enhetsnummer.", EnhetFilter.class.getSimpleName(), requestURI);
        }
      }
    } else {
      String filterRequest = servletRequest != null ? servletRequest.getClass().getSimpleName() : "null";
      LOGGER.error("Filtrering gjøres ikke av en HttpServletRequest: " + filterRequest);
    }

    filterChain.doFilter(servletRequest, servletResponse);
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
