package no.nav.bidrag.commons.web;

import static no.nav.bidrag.commons.web.EnhetFilter.X_ENHET_HEADER;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

public class HttpHeaderRestTemplate extends RestTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderRestTemplate.class);

  private final Map<String, ValueGenerator> headerGenerators = new HashMap<>();

  public HttpHeaderRestTemplate() {
  }

  public HttpHeaderRestTemplate(ClientHttpRequestFactory requestFactory) {
    super(requestFactory);
  }

  public HttpHeaderRestTemplate(List<HttpMessageConverter<?>> messageConverters) {
    super(messageConverters);
  }

  public void withDefaultHeaders() {
    addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);
    addHeaderGenerator(X_ENHET_HEADER, EnhetFilter::fetchForThread);
  }

  @Override
  public <T> RequestCallback httpEntityCallback(Object requestBody, Type responseType) {
    if (headerGenerators.isEmpty()) {
      return super.httpEntityCallback(requestBody, responseType);
    }

    HttpEntity<T> httpEntity = newEntityWithAdditionalHttpHeaders(requestBody);
    Set<String> headerNames = new HashSet<>(headerGenerators.keySet());

    if (!headerNames.isEmpty()) {
      LOGGER.info("Generate header(s): %s".formatted(headerNames));
    }

    return super.httpEntityCallback(httpEntity, responseType);
  }

  private <T> HttpEntity<T> newEntityWithAdditionalHttpHeaders(Object o) {
    if (o != null) {
      HttpEntity<T> httpEntity = mapToHttpEntity(o);
      Set<String> headerNames = new HashSet<>(httpEntity.getHeaders().keySet());

      if (!headerNames.isEmpty()) {
        LOGGER.info("Existing header(s): %s".formatted(headerNames));
      }

      return new HttpEntity<>(httpEntity.getBody(), combineHeaders(httpEntity.getHeaders()));
    }

    return new HttpEntity<>(null, combineHeaders(new HttpHeaders()));
  }

  @SuppressWarnings("unchecked")
  private <T> HttpEntity<T> mapToHttpEntity(Object o) {
    if (o instanceof HttpEntity) {
      return (HttpEntity<T>) o;
    }

    return new HttpEntity<>((T) o);
  }

  private MultiValueMap<String, String> combineHeaders(HttpHeaders existingHeaders) {
    HttpHeaders allHeaders = new HttpHeaders();
    existingHeaders.forEach((name, listValue) -> listValue.forEach(value -> {
      if (Objects.nonNull(value)){
        allHeaders.add(name, value);
      }
    }));

    headerGenerators.forEach((key, value) -> {
      var headerValue = value.generate();
      if (!isXEnhetHeaderAndXEnhetHeaderExists(key, allHeaders) && Objects.nonNull(headerValue)) {
        allHeaders.add(key, headerValue);
      }
    });

    return allHeaders;
  }

  // Prevent duplicate X_ENHET headers. Makes it possible to override the header
  private boolean isXEnhetHeaderAndXEnhetHeaderExists(String key, HttpHeaders allHeaders) {
    return X_ENHET_HEADER.equals(key) && allHeaders.get(X_ENHET_HEADER) != null;
  }

  public void addHeaderGenerator(String headerName, ValueGenerator valueGenerator) {
    headerGenerators.put(headerName, valueGenerator);
  }

  public void removeHeaderGenerator(String headerName) {
    headerGenerators.remove(headerName);
  }

  @FunctionalInterface
  public interface ValueGenerator {

    String generate();
  }
}
