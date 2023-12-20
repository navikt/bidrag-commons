package no.nav.bidrag.commons.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Random

private val K1_VEKT = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
private val K2_VEKT = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

object PersonidentGenerator {
    fun genererFødselsnummer(
        innsendtFodselsdato: LocalDate? = null,
        innsendtKjonn: Kjonn? = null,
    ): String {
        val fodselsdato = innsendtFodselsdato ?: opprettTilfeldigFodselsdato()
        val kjonn = innsendtKjonn ?: hentTilfeldigKjonn()

        var fodselsnummer = opprettIndividnummer(fodselsdato, kjonn)

        try {
            fodselsnummer = opprettKontrollsiffer(K1_VEKT, fodselsnummer)
        } catch (e: IllegalStateException) {
            return genererFødselsnummer(fodselsdato, kjonn)
        }
        try {
            fodselsnummer = opprettKontrollsiffer(K2_VEKT, fodselsnummer)
        } catch (e: IllegalStateException) {
            return genererFødselsnummer(fodselsdato, kjonn)
        }

        return fodselsnummer
    }

    private fun opprettIndividnummer(
        fodselsdato: LocalDate,
        kjonn: Kjonn,
    ): String {
        return fodselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + genererIndividnummer(fodselsdato.year, kjonn)
    }

    private fun opprettTilfeldigFodselsdato(): LocalDate {
        return LocalDate.now().minus(Period.ofDays(Random().nextInt(365 * 120)))
    }

    private fun genererIndividnummer(
        fodselsAr: Int,
        kjonn: Kjonn,
    ): String {
        return when (fodselsAr) {
            in 1940..1999 -> opprettTilfeldigIndividnummer(kjonn, 900, 999)
            in 1854..1899 -> opprettTilfeldigIndividnummer(kjonn, 500, 749)
            in 1900..1999 -> opprettTilfeldigIndividnummer(kjonn, 0, 499)
            in 2000..2039 -> opprettTilfeldigIndividnummer(kjonn, 500, 999)
            else -> throw IllegalArgumentException("Fant ikke gyldig serie for årstallet $fodselsAr")
        }
    }

    private fun opprettTilfeldigIndividnummer(
        kjonn: Kjonn,
        fraInklusiv: Int,
        tilInklusiv: Int,
    ): String {
        val antall = (tilInklusiv - fraInklusiv + 1) / 2
        var individNr = (Random().nextInt(antall) * 2 + fraInklusiv + if (Kjonn.MANN == kjonn) 1 else 0).toString()

        while (individNr.length < 3) {
            individNr = ("0$individNr")
        }

        return individNr
    }

    private fun hentTilfeldigKjonn(): Kjonn {
        val alleKjonn = Kjonn.values()
        return alleKjonn[Random().nextInt(alleKjonn.size)]
    }

    private fun opprettKontrollsiffer(
        vekting: IntArray,
        fodselsnummer: String,
    ): String {
        var sum = 0
        for (i in vekting.indices) {
            sum += fodselsnummer.substring(i, i + 1).toInt() * vekting[i]
        }
        var kontrollsiffer = 11 - sum % 11
        if (kontrollsiffer == 11) {
            kontrollsiffer = 0
        }
        check(kontrollsiffer <= 9)

        return fodselsnummer + kontrollsiffer.toString()
    }
}

enum class Kjonn {
    MANN,
    KVINNE,
}
