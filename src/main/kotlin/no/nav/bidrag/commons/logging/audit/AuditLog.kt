package no.nav.bidrag.commons.logging.audit

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuditLog(
    val auditLoggerEvent: AuditLoggerEvent,
    // Kun nødvendig hvis det ikke er første verdi i parmetere eller body som er nøkkelentitet.
    val oppslagsparameter: String = "",
)

enum class AuditLoggerEvent(val type: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACCESS("access"),
}
