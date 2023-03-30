package no.nav.bidrag.commons

import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.commons.util.KildesystemIdenfikator.Kildesystem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

internal class KildesystemIdenfikatorTest {
    @Test
    fun `skal validere prefix`() {
        assertPrefix("123", "Mangler prefix", PrefixIdent.UGYLDIG)
        assertPrefix("NA-123", "ukjent prefix", PrefixIdent.UGYLDIG)
        assertPrefix("BID-xyz", "ikke tall", PrefixIdent.UGYLDIG)
        assertPrefix("BID-123", "bidrag prefix", PrefixIdent.GYLDIG)
        assertPrefix("JOARK-123", "joark prefix", PrefixIdent.GYLDIG)
    }

    private fun assertPrefix(identifikator: String, beskrivelse: String, prefixIdent: PrefixIdent) {
        KildesystemIdenfikator(identifikator).erUkjentPrefixEllerHarIkkeTallEtterPrefix() shouldBe prefixIdent.erUgyldig()
    }

    private enum class PrefixIdent {
        GYLDIG, UGYLDIG;

        fun erUgyldig(): Boolean {
            return this == UGYLDIG
        }
    }

    @Test
    fun skalHenteJournalpostId() {
        val kildesystemIdenfikator = KildesystemIdenfikator("BID-666")
        kildesystemIdenfikator.hentJournalpostId() shouldBe 666
    }

    @Test
    fun skalHenteJournalpostIdLong() {
        val kildesystemIdenfikator = KildesystemIdenfikator("BID-666")
        kildesystemIdenfikator.hentJournalpostIdLong() shouldBe 666L
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "BID-3757282865", // journalpost Id er ikke gyldig Long, men det er et gyldig prefix på et tall - henter integer som null !!!
            "BID-3757282443", // journalpost Id er ikke gyldig Long, men det er et gyldig prefix på et tall - henter integer som null !!!
            "BID-37576108"
        ]
    )
    @DisplayName("skal ikke være ugyldige kildesystemidentifikatorer")
    fun skalIkkeVaereUgyldig(identifikator: String?) {
        KildesystemIdenfikator(identifikator!!).erUkjentPrefixEllerHarIkkeTallEtterPrefix() shouldBe false
    }

    @Test
    fun `skal hente kildesystem`() {
        assertKildesystem(KildesystemIdenfikator("joark-2"), Kildesystem.JOARK)
        assertKildesystem(KildesystemIdenfikator("bid-123456789012345"), Kildesystem.BIDRAG)
    }

    private fun assertKildesystem(kildesystemIdenfikator: KildesystemIdenfikator, kildesystem: Kildesystem) {
        kildesystemIdenfikator.kildesystem shouldBe kildesystem
        kildesystemIdenfikator.erFor(kildesystem) shouldBe true
    }

    @ParameterizedTest
    @MethodSource("initPrefixsetIdForTest")
    @DisplayName("skal si om kjent kilkdesystem har id som overstiger int som heltall")
    fun skalSiOmKjentKildesystemHarIdSomOverstigerIntSomHeltall(prefiksetId: String?, kjentPrefixOgIdOverstigerInt: Boolean?) {
        KildesystemIdenfikator(prefiksetId!!).erKjentKildesystemMedIdMedIdSomOverstigerInteger() shouldBe kjentPrefixOgIdOverstigerInt
    }

    companion object {
        @JvmStatic
        @MethodSource
        private fun initPrefixsetIdForTest(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("BID-3757282865", true),
                Arguments.of("BID-9999999", false),
                Arguments.of("JOARK-3757282443", true),
                Arguments.of("JOARK-9999999", false),
                Arguments.of("UKJENT-3757282443", false),
                Arguments.of("UKJENT-9999999", false)
            )
        }
    }
}
