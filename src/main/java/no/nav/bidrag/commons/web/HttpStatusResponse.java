package no.nav.bidrag.commons.web;

import java.util.Optional;
import org.springframework.http.HttpStatus;

public class HttpStatusResponse<T> {

  private final HttpStatus httpStatus;
  private final T body;

  public HttpStatusResponse(HttpStatus httpStatus, T body) {
    this.httpStatus = httpStatus;
    this.body = body;
  }

  public Optional<T> fetchOptionalResult() {
    return Optional.ofNullable(body);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public T getBody() {
    return body;
  }
}
