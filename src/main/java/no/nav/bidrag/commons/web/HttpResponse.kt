package no.nav.bidrag.commons.web;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

/**
 * Redirecting {@link ResponseEntity} from one service to another
 *
 * @param <T> type of http payload
 */
public class HttpResponse<T> {

  private final ResponseEntity<T> responseEntity;

  public HttpResponse(ResponseEntity<T> responseEntity) {
    this.responseEntity = responseEntity;
  }

  public Optional<T> fetchBody() {
    return Optional.ofNullable(responseEntity.getBody());
  }

  public boolean is2xxSuccessful() {
    return responseEntity.getStatusCode().is2xxSuccessful();
  }

  public HttpHeaders fetchHeaders() {
    return responseEntity.getHeaders();
  }

  public HttpResponse<T> clearContentHeaders() {
    var headersMap = responseEntity.getHeaders().entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    var headers = new HttpHeaders(CollectionUtils.toMultiValueMap(headersMap));
    headers.clearContentHeaders();

    return from(responseEntity.getBody(), headers, responseEntity.getStatusCode());
  }

  public ResponseEntity<T> getResponseEntity() {
    return responseEntity;
  }

  public static <E> HttpResponse<E> from(HttpStatus httpStatus) {
    var responseEntity = new ResponseEntity<E>(httpStatus);
    return new HttpResponse<>(responseEntity);
  }

  public static <E> HttpResponse<E> from(HttpStatus httpStatus, E body) {
    var responseEntity = new ResponseEntity<>(body, httpStatus);
    return new HttpResponse<>(responseEntity);
  }

  public static <E> HttpResponse<E> from(HttpHeaders httpHeaders, HttpStatus httpStatus) {
    var responseEntity = new ResponseEntity<E>(httpHeaders, httpStatus);
    return new HttpResponse<>(responseEntity);
  }

  public static <E> HttpResponse<E> from(E body, HttpHeaders httpHeaders, HttpStatus httpStatus) {
    var responseEntity = new ResponseEntity<>(body, httpHeaders, httpStatus);
    return new HttpResponse<>(responseEntity);
  }
}
