package no.nav.bidrag.commons.web

import org.springframework.http.HttpStatus
import java.util.Optional

@Deprecated("")
class HttpStatusResponse<T : Any>
    @JvmOverloads
    constructor(val httpStatus: HttpStatus, val body: T? = null) {
        fun fetchOptionalResult(): Optional<T> {
            return Optional.ofNullable(body)
        }

        val isNotSuccessful: Boolean
            get() = !httpStatus.is2xxSuccessful

        override fun toString(): String {
            return "HttpStatusResponse{httpStatus=$httpStatus, body=$body}"
        }

        val isBodyEmpty: Boolean
            get() = body == null
        val isBodyPresent: Boolean
            get() = body != null
    }
