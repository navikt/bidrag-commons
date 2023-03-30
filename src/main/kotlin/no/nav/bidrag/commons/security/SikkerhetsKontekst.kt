package no.nav.bidrag.commons.security

object SikkerhetsKontekst {
    private val ER_I_APPLIKASJONSKONTEKST = ThreadLocal<Boolean>()

    fun <R> medApplikasjonKontekst(func: () -> R): R {
        ER_I_APPLIKASJONSKONTEKST.set(true)
        return try {
            func.invoke()
        } finally {
            ER_I_APPLIKASJONSKONTEKST.set(false)
        }
    }

    fun erIApplikasjonKontekst(): Boolean {
        return ER_I_APPLIKASJONSKONTEKST.get() ?: false
    }
}
