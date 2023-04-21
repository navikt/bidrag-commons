package no.nav.bidrag.commons.web

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import java.util.*

/**
 * Redirecting [ResponseEntity] from one service to another
 *
 * @param <T> type of http payload
</T> */
class HttpResponse<T>(val responseEntity: ResponseEntity<T>) {

    fun fetchBody(): Optional<T> {
        return Optional.ofNullable(responseEntity.body)
    }

    fun is2xxSuccessful(): Boolean {
        return responseEntity.statusCode.is2xxSuccessful
    }

    fun fetchHeaders(): HttpHeaders {
        return responseEntity.headers
    }

    fun clearContentHeaders(): HttpResponse<T?> {
        val headersMap: Map<String, List<String>> =
            responseEntity.headers
                .entries
                .associateBy({ (key) -> key }, { (_, value) -> value })

        val headers = HttpHeaders(CollectionUtils.toMultiValueMap(headersMap))
        headers.clearContentHeaders()
        return from(responseEntity.body, headers, HttpStatus.valueOf(responseEntity.statusCode.value()))
    }

    companion object {
        fun <E> from(httpStatus: HttpStatus?): HttpResponse<E> {
            val responseEntity = ResponseEntity<E>(httpStatus!!)
            return HttpResponse(responseEntity)
        }

        fun <E> from(httpStatus: HttpStatus?, body: E): HttpResponse<E> {
            val responseEntity = ResponseEntity(body, httpStatus!!)
            return HttpResponse(responseEntity)
        }

        fun <E> from(httpHeaders: HttpHeaders?, httpStatus: HttpStatus?): HttpResponse<E> {
            val responseEntity = ResponseEntity<E>(httpHeaders!!, httpStatus!!)
            return HttpResponse(responseEntity)
        }

        fun <E> from(body: E, httpHeaders: HttpHeaders?, httpStatus: HttpStatus?): HttpResponse<E> {
            val responseEntity = ResponseEntity(body, httpHeaders, httpStatus!!)
            return HttpResponse(responseEntity)
        }
    }
}
