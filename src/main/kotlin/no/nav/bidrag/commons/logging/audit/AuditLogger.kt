package no.nav.bidrag.commons.logging.audit

import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.MdcConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class AuditLogger(@Value("\${NAIS_APP_NAME}") private val applicationName: String) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val audit = LoggerFactory.getLogger("auditLogger")

    fun log(event: AuditLoggerEvent, data: Sporingsdata) {
        val request = getRequest() ?: throw IllegalArgumentException("Ikke brukt i context av en HTTP request")

        if (!ContextService.erMaskinTilMaskinToken()) {
            audit.info(createAuditLogString(event, data, request))
        } else {
            logger.debug("Maskin til maskin token i request")
        }
    }

    private fun getRequest(): HttpServletRequest? {
        return RequestContextHolder.getRequestAttributes()
            ?.takeIf { it is ServletRequestAttributes }
            ?.let { it as ServletRequestAttributes }
            ?.request
    }

    private fun createAuditLogString(event: AuditLoggerEvent, data: Sporingsdata, request: HttpServletRequest): String {
        val timestamp = System.currentTimeMillis()
        val name = "Saksbehandling"
        return "CEF:0|$applicationName|auditLog|1.0|audit:${event.type}|$name|INFO|end=$timestamp " +
            "suid=${ContextService.hentPåloggetSaksbehandler()} " +
            "duid=${data.personIdent} " +
            "sproc=${getCallId()} " +
            "requestMethod=${request.method} " +
            "request=${request.requestURI} " +
            createCustomString(data)
    }

    private fun createCustomString(data: Sporingsdata): String {
        return listOfNotNull(
            data.custom1?.let { "cs3Label=${it.first} cs3=${it.second}" },
            data.custom2?.let { "cs5Label=${it.first} cs5=${it.second}" },
            data.custom3?.let { "cs6Label=${it.first} cs6=${it.second}" }
        ).joinToString(" ")
    }

    private fun getCallId(): String {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC)
            ?: MDC.get(MdcConstants.MDC_CALL_ID)
            ?: throw IllegalStateException("Mangler correlationId/callId")
    }
}
