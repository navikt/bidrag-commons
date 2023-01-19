package no.nav.bidrag.commons.logging.audit

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuditLog(
    val auditLoggerEvent: AuditLoggerEvent,
    val oppslagsparameter: String,
    val parametertype: Parametertype = Parametertype.DEFAULT
)

/**
 * Brukes for å overstyre parametertype som utledes av requesttype
 */
enum class Parametertype {
    DEFAULT,
    ENKELTVERDI,
    REQUEST_BODY
}

enum class AuditLoggerEvent(val type: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACCESS("access")
}
