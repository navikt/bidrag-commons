package no.nav.bidrag.commons.web

import no.nav.bidrag.commons.web.EnhetFilter.Companion.X_ENHET_HEADER
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Type
import java.util.*

open class HttpHeaderRestTemplate : RestTemplate {
  private val headerGenerators: MutableMap<String, () -> String> = HashMap()

  private val log = LoggerFactory.getLogger(this::class.java)

  constructor()
  constructor(requestFactory: ClientHttpRequestFactory) : super(requestFactory) {}
  constructor(messageConverters: List<HttpMessageConverter<*>>) : super(messageConverters) {}

  fun withDefaultHeaders() {
    addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread)
    addHeaderGenerator(X_ENHET_HEADER, EnhetFilter::fetchForThread)
  }

  override fun <T> httpEntityCallback(requestBody: Any?, responseType: Type): RequestCallback {
    if (headerGenerators.isEmpty()) {
      return super.httpEntityCallback<Any>(requestBody, responseType)
    }
    val httpEntity = newEntityWithAdditionalHttpHeaders<T>(requestBody)
    val headerNames: Set<String> = HashSet(headerGenerators.keys)
    if (headerNames.isNotEmpty()) {
      log.debug("Generate header(s): %s".format(headerNames))
    }
    return super.httpEntityCallback<Any>(httpEntity, responseType)
  }

  private fun <T> newEntityWithAdditionalHttpHeaders(o: Any?): HttpEntity<T> {
    if (o != null) {
      val httpEntity = mapToHttpEntity<T>(o)
      val headerNames: Set<String> = HashSet(httpEntity.headers.keys)
      if (!headerNames.isEmpty()) {
        log.debug("Existing header(s): %s".format(headerNames))
      }
      return HttpEntity(httpEntity.body, combineHeaders(httpEntity.headers))
    }
    return HttpEntity(null, combineHeaders(HttpHeaders()))
  }

  private fun <T> mapToHttpEntity(o: Any): HttpEntity<T> {
    return if (o is HttpEntity<*>) {
      o as HttpEntity<T>
    } else HttpEntity(o as T)
  }

  private fun combineHeaders(existingHeaders: HttpHeaders): MultiValueMap<String, String> {
    val allHeaders = HttpHeaders()
    existingHeaders.forEach { name: String, listValue: List<String> ->
      listValue.forEach { value: String ->
        allHeaders.add(name, value)
      }
    }
    headerGenerators.forEach { (key: String, value: () -> String) ->
      val headerValue = value.invoke()
      if (!isXEnhetHeaderAndXEnhetHeaderExists(key, allHeaders) && Objects.nonNull(headerValue)) {
        allHeaders.add(key, headerValue)
      }
    }
    return allHeaders
  }

  // Prevent duplicate X_ENHET headers. Makes it possible to override the header
  private fun isXEnhetHeaderAndXEnhetHeaderExists(key: String, allHeaders: HttpHeaders): Boolean {
    return X_ENHET_HEADER == key && allHeaders[X_ENHET_HEADER] != null
  }

  fun addHeaderGenerator(headerName: String, valueGenerator: () -> String) {
    headerGenerators[headerName] = valueGenerator
  }

  fun addHeaderGenerator(headerName: String, valueGenerator: ValueGenerator) {
    headerGenerators[headerName] = valueGenerator
  }

  fun removeHeaderGenerator(headerName: String) {
    headerGenerators.remove(headerName)
  }

  @FunctionalInterface
  interface ValueGenerator : () -> String {
      fun  generate(): String
  }

}