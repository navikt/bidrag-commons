package no.nav.bidrag.commons.web;

import java.util.Optional;
import org.springframework.http.HttpStatus;

public class HttpStatusResponse<T> {

  private final HttpStatus httpStatus;
  private final T body;

  public HttpStatusResponse(HttpStatus httpStatus) {
    this(httpStatus, null);
  }

  public HttpStatusResponse(HttpStatus httpStatus, T body) {
    this.httpStatus = httpStatus;
    this.body = body;
  }

  public Optional<T> fetchOptionalResult() {
    return Optional.ofNullable(body);
  }

  public boolean isNotSuccessful() {
    return !httpStatus.is2xxSuccessful();
  }

  @Override
  public String toString() {
    return "HttpStatusResponse{" +
        "httpStatus=" + httpStatus +
        ", body=" + body +
        '}';
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public boolean isBodyEmpty() {
    return body == null;
  }

  public boolean isBodyPresent() {
    return body != null;
  }

  public T getBody() {
    return body;
  }
}
