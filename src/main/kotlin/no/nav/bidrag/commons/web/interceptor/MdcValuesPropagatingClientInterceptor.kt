package no.nav.bidrag.commons.web.interceptor

import no.nav.bidrag.commons.util.IdUtils
import no.nav.bidrag.commons.web.BidragHttpHeaders
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.MdcConstants
import no.nav.bidrag.commons.web.MdcFilter.Companion.NAV_CALL_ID_HEADER_NAMES
import org.slf4j.MDC
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.util.*

@Component
@Import(IdUtils::class)
class MdcValuesPropagatingClientInterceptor(private val idUtils: IdUtils) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val callId = MDC.get(MdcConstants.MDC_CALL_ID) ?: idUtils.generateId()
        // Propagerer alle alternativer for callId inntil alle applikasjonene våre er samkjørte.
        NAV_CALL_ID_HEADER_NAMES.forEach {
            request.headers.add(it, callId)
        }

        val enhet = MDC.get(EnhetFilter.X_ENHET_HEADER) ?: EnhetFilter.fetchForThread()
        request.headers.add(BidragHttpHeaders.X_ENHET, enhet)

        return execution.execute(request, body)
    }
}
