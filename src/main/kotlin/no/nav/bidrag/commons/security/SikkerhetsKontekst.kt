package no.nav.bidrag.commons.security

object SikkerhetsKontekst {
  private val ER_I_APPLIKASJON_KONTEKST = ThreadLocal<Boolean>()

  fun <R> medApplikasjonKontekst(func: () -> R): R {
    ER_I_APPLIKASJON_KONTEKST.set(true)
    return try {
      func.invoke()
    } finally {
      ER_I_APPLIKASJON_KONTEKST.set(false)
    }
  }

  fun erIApplikasjonKontekst(): Boolean {
    return ER_I_APPLIKASJON_KONTEKST.get() ?: false
  }

}