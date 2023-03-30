package no.nav.bidrag.commons.logging.audit

import no.nav.bidrag.commons.security.ContextService
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
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

        val parameter = joinpoint.args.first()

        when (auditLog.parametertype) {
            Parametertype.DEFAULT -> {
                when (auditLog.auditLoggerEvent) {
                    AuditLoggerEvent.ACCESS, AuditLoggerEvent.DELETE -> auditForEnkeltverdi(parameter, joinpoint, auditLog)
                    AuditLoggerEvent.CREATE, AuditLoggerEvent.UPDATE -> auditForBody(parameter, auditLog)
                }
            }
            Parametertype.ENKELTVERDI -> auditForEnkeltverdi(parameter, joinpoint, auditLog)
            Parametertype.REQUEST_BODY -> auditForBody(parameter, auditLog)
        }
    }

    private fun auditForEnkeltverdi(parameter: Any, joinpoint: JoinPoint, auditLog: AuditLog) {
        val parameternavn = (joinpoint.signature as CodeSignature).parameterNames.first()
        val sporingsdata = sporingsdataService.findSporingsdataForFelt(parameternavn, parameter)
        logAccess(auditLog, sporingsdata)
    }

    private fun auditForBody(requestBody: Any, auditLog: AuditLog) {
        val fields: Collection<KProperty1<out Any, *>> = requestBody::class.declaredMemberProperties
        val oppslagsfeltFraRequest =
            fields.find { auditLog.oppslagsparameter == it.name }
                ?: error("Feltet ${auditLog.oppslagsparameter} finnes ikke i requestBody.")

        val oppslagsfelt = oppslagsfeltFraRequest.getter.call(requestBody)
            ?: error("Uthenting av verdien til ${auditLog.oppslagsparameter} for audit-logging feilet!")
        val sporingsdata = sporingsdataService.findSporingsdataForFelt(auditLog.oppslagsparameter, oppslagsfelt)
        logAccess(auditLog, sporingsdata)
    }

    fun logAccess(auditLog: AuditLog, sporingsdata: Sporingsdata) {
        auditLogger.log(auditLog.auditLoggerEvent, sporingsdata)
    }
}
