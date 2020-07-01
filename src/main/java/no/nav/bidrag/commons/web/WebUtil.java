package no.nav.bidrag.commons.web;

import org.springframework.http.HttpHeaders;

public final class WebUtil {

  private WebUtil() {
  }

  public static HttpHeaders initHttpHeadersWith(String name, String value) {
    var httpHeaders = new HttpHeaders();
    httpHeaders.add(name, value);

    return httpHeaders;
  }
}
