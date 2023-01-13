package no.nav.bidrag.commons.logging.audit

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuditLog(
    val auditLoggerEvent: AuditLoggerEvent,
    val oppslagsparameter: String
) // brukes kun i GET request/request uten body

enum class AuditLoggerEvent(val type: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACCESS("access")
}
