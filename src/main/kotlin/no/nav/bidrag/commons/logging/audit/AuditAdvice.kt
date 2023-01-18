package no.nav.bidrag.commons.logging.audit

import no.nav.bidrag.commons.security.ContextService
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

@Aspect
@Configuration
@Import(AuditLogger::class)
class AuditAdvice(
  private val auditLogger: AuditLogger,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val sporingsdataService: SporingsdataService
) {

  private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

  @Before("@annotation(auditLog) ")
  fun sjekkTilgang(joinpoint: JoinPoint, auditLog: AuditLog) {
    if (ContextService.erMaskinTilMaskinToken()) {
      return
    }

    val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

    when (HttpMethod.resolve(httpRequest.method)) {
      HttpMethod.DELETE, HttpMethod.GET ->
        validateFagsystemTilgangIGetRequest(joinpoint.args, auditLog)
      HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH ->
        validateFagsystemTilgangMedBody(joinpoint.args[0], auditLog)
      else ->
        logger.error("${httpRequest.requestURI} støtter ikke audit-logging")
    }
  }

  private fun validateFagsystemTilgangIGetRequest(params: Array<Any>, auditLog: AuditLog) {
    val sporingsdata = sporingsdataService.findSporingsdataForFelt(auditLog.oppslagsparameter, params.first())
    logAccess(auditLog, sporingsdata)
  }

  private fun validateFagsystemTilgangMedBody(requestBody: Any, auditLog: AuditLog) {
    val fields: Collection<KProperty1<out Any, *>> = requestBody::class.declaredMemberProperties
    val oppslagsfeltFraRequest = fields.find { auditLog.oppslagsparameter == it.name }
      ?: error("Feltet ${auditLog.oppslagsparameter} finnes ikke i requestBody.")

    val sporingsdata = sporingsdataService.findSporingsdataForFelt(auditLog.oppslagsparameter, oppslagsfeltFraRequest.getter.call(requestBody) )
    logAccess(auditLog, sporingsdata)
  }

  fun logAccess(auditLog: AuditLog, sporingsdata: Sporingsdata) {
    auditLogger.log(auditLog.auditLoggerEvent, sporingsdata)
  }
}
