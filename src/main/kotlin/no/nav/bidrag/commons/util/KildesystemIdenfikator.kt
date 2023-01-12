package no.nav.bidrag.commons.util

import net.logstash.logback.encoder.org.apache.commons.lang3.builder.ToStringBuilder
import net.logstash.logback.encoder.org.apache.commons.lang3.builder.ToStringStyle
import org.slf4j.LoggerFactory
import java.util.*

class KildesystemIdenfikator(prefiksetJournalpostId: String) {

  val logger = LoggerFactory.getLogger(this::class.java)
  val kildesystem: Kildesystem
  val prefiksetJournalpostId: String
  private var journalpostId: Int? = null

  init {
    this.prefiksetJournalpostId = trimAndUpperCase(prefiksetJournalpostId)
    kildesystem = Kildesystem.hentKildesystem(this.prefiksetJournalpostId)
  }

  private fun trimAndUpperCase(string: String): String {
    return string.trim { it <= ' ' }.uppercase(Locale.getDefault())
  }

  fun erUkjentPrefixEllerHarIkkeTallEtterPrefix(): Boolean {
    val ugyldigPefix = kildesystem.erUkjent() || kildesystem.harIkkeJournalpostIdSomTall(prefiksetJournalpostId)
    if (ugyldigPefix) {
      logger.warn("Id har ikke riktig prefix: $prefiksetJournalpostId")
    }
    return ugyldigPefix
  }

  fun hentJournalpostId(): Int? {
    if (journalpostId == null) {
      journalpostId = kildesystem.hentJournalpostId(prefiksetJournalpostId)
    }
    return journalpostId
  }

  fun hentJournalpostIdLong(): Long? {
    return hentJournalpostId()?.toLong()
  }

  fun erFor(kildesystem: Kildesystem): Boolean {
    return this.kildesystem.er(kildesystem)
  }

  fun erKjentKildesystemMedIdMedIdSomOverstigerInteger(): Boolean {
    if (kildesystem.erUkjent()) {
      logger.warn("Ukjent kildesystem i '$prefiksetJournalpostId'")
      return false
    }
    return kildesystem.idErStorreEnnIntegerMax(prefiksetJournalpostId)
  }

  override fun toString(): String {
    return ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("prefiksetJournalpostId", prefiksetJournalpostId)
      .append("kildesystem", kildesystem)
      .toString()
  }

  enum class Kildesystem(private val prefixMedDelimiter: String) {
    BIDRAG(PREFIX_BIDRAG_COMPLETE), JOARK(PREFIX_JOARK_COMPLETE), FORSENDELSE(PREFIX_FORSENDELSE_COMPLETE), UKJENT("");

    fun er(kildesystem: Kildesystem): Boolean {
      return kildesystem == this
    }

    fun harIkkeJournalpostIdSomTall(prefiksetJournalpostId: String): Boolean {
      val utenPrefix = prefiksetJournalpostId.replace(prefixMedDelimiter.toRegex(), "")
      val bareTall = utenPrefix.replace(NON_DIGITS.toRegex(), "")
      return utenPrefix.length != bareTall.length
    }

    fun erUkjent(): Boolean {
      return er(UKJENT)
    }

    fun hentJournalpostId(prefiksetJournalpostId: String): Int? {
      val ident = prefiksetJournalpostId.replace(prefixMedDelimiter.toRegex(), "")
      return try {
        Integer.valueOf(ident)
      } catch (nfe: NumberFormatException) {
        LOGGER.warn("'$prefiksetJournalpostId' formatert til '$ident' skaper NumberFormatException: $nfe")
        null
      }
    }

    fun idErStorreEnnIntegerMax(prefksetJournalpostId: String): Boolean {
      val bareTall = prefksetJournalpostId.replace(NON_DIGITS.toRegex(), "")
      try {
        val longSomTall = bareTall.toLong()
        if (longSomTall > Int.MAX_VALUE) {
          LOGGER.warn("kan ikke parses til int: '{}'", longSomTall)
          return true
        }
      } catch (nfe: NumberFormatException) {
        LOGGER.warn("kan ikke parses til int: '{}'", bareTall)
        return true
      }
      return false
    }

    companion object {
      private const val NON_DIGITS = "\\D+"
      private val LOGGER = LoggerFactory.getLogger(Kildesystem::class.java)
      fun hentKildesystem(prefiksetJournalpostId: String): Kildesystem {
        return if (prefiksetJournalpostId.startsWith(BIDRAG.prefixMedDelimiter)) BIDRAG
        else if (prefiksetJournalpostId.startsWith(JOARK.prefixMedDelimiter)) JOARK
        else if (prefiksetJournalpostId.startsWith(FORSENDELSE.prefixMedDelimiter)) FORSENDELSE
        else UKJENT
      }
    }
  }

  companion object {
    const val DELIMTER = "-"
    const val PREFIX_BIDRAG = "BID"
    const val PREFIX_BIDRAG_COMPLETE = PREFIX_BIDRAG + DELIMTER
    const val PREFIX_JOARK = "JOARK"
    const val PREFIX_FORSENDELSE = "BIF"
    const val PREFIX_JOARK_COMPLETE = PREFIX_JOARK + DELIMTER
    const val PREFIX_FORSENDELSE_COMPLETE = PREFIX_FORSENDELSE + DELIMTER
  }
}