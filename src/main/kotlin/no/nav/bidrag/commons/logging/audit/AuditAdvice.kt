package no.nav.bidrag.commons.logging.audit

import no.nav.bidrag.commons.security.ContextService
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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

  @Before("@annotation(auditLog) ")
  fun loggTilgang(joinpoint: JoinPoint, auditLog: AuditLog) {
    if (ContextService.erMaskinTilMaskinToken()) {
      return
    }

    if (auditLog.parametertype == Parametertype.DEFAULT) {
      when (auditLog.auditLoggerEvent) {
        AuditLoggerEvent.ACCESS, AuditLoggerEvent.DELETE ->
          auditForEnkeltverdi(joinpoint.args, auditLog)
        AuditLoggerEvent.CREATE, AuditLoggerEvent.UPDATE ->
          auditForBody(joinpoint.args[0], auditLog)
      }
    } else {
      if (auditLog.parametertype == Parametertype.ENKELTVERDI) {
        auditForEnkeltverdi(joinpoint.args, auditLog)
      } else {
        auditForBody(joinpoint.args[0], auditLog)
      }
    }
  }

  private fun auditForEnkeltverdi(params: Array<Any>, auditLog: AuditLog) {
    val sporingsdata = sporingsdataService.findSporingsdataForFelt(auditLog.oppslagsparameter, params.first())
    logAccess(auditLog, sporingsdata)
  }

  private fun auditForBody(requestBody: Any, auditLog: AuditLog) {
    val fields: Collection<KProperty1<out Any, *>> = requestBody::class.declaredMemberProperties
    val oppslagsfeltFraRequest = fields.find { auditLog.oppslagsparameter == it.name }
      ?: error("Feltet ${auditLog.oppslagsparameter} finnes ikke i requestBody.")

    val sporingsdata = sporingsdataService.findSporingsdataForFelt(auditLog.oppslagsparameter, oppslagsfeltFraRequest.getter.call(requestBody))
    logAccess(auditLog, sporingsdata)
  }

  fun logAccess(auditLog: AuditLog, sporingsdata: Sporingsdata) {
    auditLogger.log(auditLog.auditLoggerEvent, sporingsdata)
  }
}
