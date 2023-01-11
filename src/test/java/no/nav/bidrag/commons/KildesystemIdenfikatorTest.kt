package no.nav.bidrag.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.stream.Stream;
import no.nav.bidrag.commons.util.KildesystemIdenfikator;
import no.nav.bidrag.commons.util.KildesystemIdenfikator.Kildesystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class KildesystemIdenfikatorTest {

  @Test
  @DisplayName("skal validere prefix")
  void skalValiderePrefix() {
    assertAll(
        () -> assertPrefix("123", "Mangler prefix", PrefixIdent.UGYLDIG),
        () -> assertPrefix("NA-123", "ukjent prefix", PrefixIdent.UGYLDIG),
        () -> assertPrefix("BID-xyz", "ikke tall", PrefixIdent.UGYLDIG),
        () -> assertPrefix("BID-123", "bidrag prefix", PrefixIdent.GYLDIG),
        () -> assertPrefix("JOARK-123", "joark prefix", PrefixIdent.GYLDIG)
    );
  }

  private void assertPrefix(String identifikator, String beskrivelse, PrefixIdent prefixIdent) {
    assertThat(new KildesystemIdenfikator(identifikator).erUkjentPrefixEllerHarIkkeTallEtterPrefix()).as(beskrivelse)
        .isEqualTo(prefixIdent.erUgyldig());
  }

  private enum PrefixIdent {
    GYLDIG, UGYLDIG;

    boolean erUgyldig() {
      return this == UGYLDIG;
    }
  }

  @Test
  void skalHenteJournalpostId() {
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator("BID-666");

    assertThat(kildesystemIdenfikator.hentJournalpostId()).isEqualTo(666);
  }

  @Test
  void skalHenteJournalpostIdLong() {
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator("BID-666");

    assertThat(kildesystemIdenfikator.hentJournalpostIdLong()).isEqualTo(666L);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "BID-3757282865", // journalpost Id er ikke gyldig (Long), men det er et gyldig prefix på et tall - henter integer som null (!!!)
      "BID-3757282443", // journalpost Id er ikke gyldig (Long), men det er et gyldig prefix på et tall - henter integer som null (!!!)
      "BID-37576108"
  })
  @DisplayName("skal ikke være ugyldige kildesystemidentifikatorer")
  void skalIkkeVaereUgyldig(String identifikator) {
    assertThat(new KildesystemIdenfikator(identifikator).erUkjentPrefixEllerHarIkkeTallEtterPrefix())
        .as("er %s gyldig", identifikator).isEqualTo(false);
  }

  @Test
  @DisplayName("skal hente kildesystem")
  void skalHenteKildesystem() {
    assertAll(
        () -> assertKildesystem(new KildesystemIdenfikator("joark-2"), Kildesystem.JOARK),
        () -> assertKildesystem(new KildesystemIdenfikator("bid-123456789012345"), Kildesystem.BIDRAG)
    );
  }

  private void assertKildesystem(KildesystemIdenfikator kildesystemIdenfikator, Kildesystem kildesystem) {
    assertAll(
        () -> assertThat(kildesystemIdenfikator.kildesystem).as(kildesystemIdenfikator.prefiksetJournalpostId).isEqualTo(kildesystem),
        () -> assertThat(kildesystemIdenfikator.erFor(kildesystem)).as(kildesystemIdenfikator.prefiksetJournalpostId).isTrue()
    );
  }

  @MethodSource
  private static Stream<Arguments> initPrefixsetIdForTest() {
    return Stream.of(
        Arguments.of("BID-3757282865", true),
        Arguments.of("BID-9999999", false),
        Arguments.of("JOARK-3757282443", true),
        Arguments.of("JOARK-9999999", false),
        Arguments.of("UKJENT-3757282443", false),
        Arguments.of("UKJENT-9999999", false)
    );
  }

  @ParameterizedTest
  @MethodSource("initPrefixsetIdForTest")
  @DisplayName("skal si om kjent kilkdesystem har id som overstiger int som heltall")
  void skalSiOmKjentKildesystemHarIdSomOverstigerIntSomHeltall(String prefiksetId, Boolean kjentPrefixOgIdOverstigerInt) {
    assertThat(new KildesystemIdenfikator(prefiksetId).erKjentKildesystemMedIdMedIdSomOverstigerInteger()).as(prefiksetId)
        .isEqualTo(kjentPrefixOgIdOverstigerInt);
  }
}