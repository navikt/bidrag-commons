package no.nav.bidrag.commons.web;

import java.util.Optional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
