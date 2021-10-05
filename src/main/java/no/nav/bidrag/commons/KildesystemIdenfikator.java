package no.nav.bidrag.commons;

import net.logstash.logback.encoder.org.apache.commons.lang3.builder.ToStringBuilder;
import net.logstash.logback.encoder.org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KildesystemIdenfikator {

  private static final Logger LOGGER = LoggerFactory.getLogger(KildesystemIdenfikator.class);

  public static final String DELIMTER = "-";
  public static final String PREFIX_BIDRAG = "BID";
  public static final String PREFIX_BIDRAG_COMPLETE = PREFIX_BIDRAG + DELIMTER;
  public static final String PREFIX_JOARK = "JOARK";
  public static final String PREFIX_JOARK_COMPLETE = PREFIX_JOARK + DELIMTER;

  private final Kildesystem kildesystem;
  private final String prefiksetJournalpostId;

  private Integer journalpostId;

  public KildesystemIdenfikator(String prefiksetJournalpostId) {
    if (prefiksetJournalpostId == null) {
      throw new IllegalArgumentException("En prefikset journalpost Id kan ikke være null!");
    }

    this.prefiksetJournalpostId = trimAndUpperCase(prefiksetJournalpostId);
    kildesystem = Kildesystem.hentKildesystem(this.prefiksetJournalpostId);
  }

  private String trimAndUpperCase(String string) {
    return string.trim().toUpperCase();
  }

  public boolean erUkjentPrefixEllerHarIkkeTallEtterPrefix() {
    boolean ugyldigPefix = kildesystem.erUkjent() || kildesystem.harIkkeJournalpostIdSomTall(prefiksetJournalpostId);

    if (ugyldigPefix) {
      LOGGER.warn("Id har ikke riktig prefix: " + prefiksetJournalpostId);
    }

    return ugyldigPefix;
  }

  public Integer hentJournalpostId() {
    if (journalpostId == null) {
      journalpostId = kildesystem.hentJournalpostId(prefiksetJournalpostId);
    }

    return journalpostId;
  }

  public Long hentJournalpostIdLong() {
    return Long.valueOf(this.hentJournalpostId());
  }

  public boolean erFor(Kildesystem kildesystem) {
    return this.kildesystem.er(kildesystem);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("prefiksetJournalpostId", prefiksetJournalpostId)
        .append("kildesystem", kildesystem)
        .toString();
  }

  public String getPrefiksetJournalpostId() {
    return prefiksetJournalpostId;
  }

  public Kildesystem getKildesystem() {
    return kildesystem;
  }

  public enum Kildesystem {
    BIDRAG(PREFIX_BIDRAG_COMPLETE),
    JOARK(PREFIX_JOARK_COMPLETE),
    UKJENT(null);

    private static final String NON_DIGITS = "\\D+";
    private final String prefixMedDelimiter;

    Kildesystem(String prefixMedDelimiter) {
      this.prefixMedDelimiter = prefixMedDelimiter;
    }

    public boolean er(Kildesystem kildesystem) {
      return kildesystem == this;
    }

    boolean harIkkeJournalpostIdSomTall(String prefiksetJournalpostId) {
      String utenPrefix = prefiksetJournalpostId.replaceAll(prefixMedDelimiter, "");
      String bareTall = utenPrefix.replaceAll(NON_DIGITS, "");

      return utenPrefix.length() != bareTall.length();
    }

    boolean erUkjent() {
      return er(UKJENT);
    }

    static Kildesystem hentKildesystem(String prefiksetJournalpostId) {
      if (prefiksetJournalpostId.startsWith(BIDRAG.prefixMedDelimiter)) {
        return BIDRAG;
      }

      if (prefiksetJournalpostId.startsWith(JOARK.prefixMedDelimiter)) {
        return JOARK;
      }

      return UKJENT;
    }

    public Integer hentJournalpostId(String prefiksetJournalpostId) {
      String ident = prefiksetJournalpostId.replaceAll(prefixMedDelimiter, "");

      try {
        return Integer.valueOf(ident);
      } catch (NumberFormatException nfe) {
        LOGGER.warn("'{}' formatert til '{}' skaper NumberFormatException: {}", prefiksetJournalpostId, ident, nfe);

        return null;
      }
    }
  }
}
