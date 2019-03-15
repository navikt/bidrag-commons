package no.nav.bidrag.commons.web;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.nav.bidrag.commons.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
  private static final String CORRELATION_ID_MDC = "correlationId";

  public static final String CORRELATION_ID_HEADER = CorrelationId.CORRELATION_ID_HEADER;

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    var httpServletRequest = (HttpServletRequest) servletRequest;
    var httpServletResponse = (HttpServletResponse) servletResponse;
    var method = httpServletRequest.getMethod();
    var requestURI = httpServletRequest.getRequestURI();

    if (isNotRequestToActuatorEndpoint(requestURI)) {
      CorrelationId correlationId;

      if (Optional.ofNullable(httpServletRequest.getHeader(CORRELATION_ID_HEADER)).isPresent()) {
        correlationId = CorrelationId.existing(httpServletRequest.getHeader(CORRELATION_ID_HEADER));
      } else {
        correlationId = generateCorreleationIdToHttpHeaderOnResponse(
            httpServletResponse, CorrelationId.generateTimestamped(fetchLastPartOfRequestUri(requestURI))
        );
      }

      MDC.put(CORRELATION_ID_MDC, correlationId.get());

      LOGGER.info("Prosessing {} {}", method, requestURI);
    }

    filterChain.doFilter(servletRequest, servletResponse);
    MDC.clear();
  }

  private boolean isNotRequestToActuatorEndpoint(String requestURI) {
    if (requestURI == null) {
      throw new IllegalStateException("should only use this class in an web environment which receives requestUri!!!");
    }

    return !requestURI.contains("/actuator/");
  }

  private CorrelationId generateCorreleationIdToHttpHeaderOnResponse(HttpServletResponse httpServletResponse, CorrelationId correlationId) {
    httpServletResponse.addHeader(CORRELATION_ID_HEADER, correlationId.get());

    return correlationId;
  }

  private String fetchLastPartOfRequestUri(String requestUri) {
    if (requestUri.contains("/")) {
      return fetchLastPartOfRequestUriContainingPlainText(requestUri);
    }

    return requestUri;
  }

  private String fetchLastPartOfRequestUriContainingPlainText(String requestUri) {
    ArrayList<String> reversedUriParts = reverseUriPartsBySlash(requestUri);
    String lastUriPsty = reversedUriParts.get(0);

    if (lastUriPsty.matches("^[a-zA-Z]+$")) {
      return lastUriPsty;
    }

    return reversedUriParts.get(1) + '/' + lastUriPsty;
  }

  private ArrayList<String> reverseUriPartsBySlash(String requestUri) {
    String[] uriArray = requestUri.split("/");
    var uriParts = new ArrayList<>(asList(uriArray));
    Collections.reverse(uriParts);

    return uriParts;
  }

  public static String fetchCorrelationIdForThread() {
    return CorrelationId.fetchCorrelationIdForThread();
  }
}
