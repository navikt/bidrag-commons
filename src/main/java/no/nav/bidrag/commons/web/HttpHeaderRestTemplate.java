package no.nav.bidrag.commons.web;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

public class HttpHeaderRestTemplate extends RestTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderRestTemplate.class);

  private Set<HeaderGenerator> headerGenerators = new HashSet<>();

  @Override
  public <T> RequestCallback httpEntityCallback(Object requestBody, Type responseType) {
    if (headerGenerators.isEmpty()) {
      return super.httpEntityCallback(requestBody, responseType);
    }

    HttpEntity<T> httpEntity = newEntityWithAdditionalHttpHeaders(requestBody);
    httpEntity.getHeaders().forEach((name, values) -> LOGGER.info("Using {}: {}", name, values.get(0)));

    return super.httpEntityCallback(httpEntity, responseType);
  }

  private <T> HttpEntity<T> newEntityWithAdditionalHttpHeaders(Object o) {
    if (o != null) {
      HttpEntity<T> httpEntity = mapToHttpEntity(o);
      return new HttpEntity<>(httpEntity.getBody(), combineHeaders(httpEntity.getHeaders()));
    }

    return new HttpEntity<>(null, combineHeaders(new HttpHeaders()));
  }

  @SuppressWarnings("unchecked")
  private <T> HttpEntity<T> mapToHttpEntity(Object o) {
    return Stream.of(o)
        .filter(obj -> obj instanceof HttpEntity)
        .map(obj -> (HttpEntity<T>) obj)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            String.format("%s cannot be used as a request body for a HttpEntityCallback", o.getClass().getSimpleName())
        ));
  }

  private MultiValueMap<String, String> combineHeaders(HttpHeaders existingHeaders) {
    HttpHeaders allHeaders = new HttpHeaders();
    existingHeaders.forEach((name, listValue) -> listValue.forEach(value -> allHeaders.add(name, value)));

    headerGenerators.stream()
        .map(HeaderGenerator::generate)
        .forEach(header -> allHeaders.add(header.name, header.valueGenerator.generate()));

    return allHeaders;
  }

  public void addHeaderGenerator(HeaderGenerator headerGenerator) {
    headerGenerators.add(headerGenerator);
  }

  @FunctionalInterface
  public interface HeaderGenerator {

    Header generate();
  }

  public static class Header {

    final String name;
    final ValueGenerator valueGenerator;

    public Header(String name, ValueGenerator valueGenerator) {
      this.name = name;
      this.valueGenerator = valueGenerator;
    }
  }

  @FunctionalInterface
  public interface ValueGenerator {

    String generate();
  }
}
