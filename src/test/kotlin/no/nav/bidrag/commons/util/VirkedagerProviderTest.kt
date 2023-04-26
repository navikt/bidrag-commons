package no.nav.bidrag.commons.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VirkedagerProviderTest {
    val skjærtorsdag2021 = LocalDate.of(2021, 4, 1)
    val skjærtorsdag2022 = LocalDate.of(2022, 4, 14)

    @Test
    fun `Hent virkedag vanlig mandag`() {
        val vanligMandag = LocalDate.of(2020, 10, 26)
        VirkedagerProvider.nesteVirkedag(vanligMandag) shouldBe vanligMandag.plusDays(1)
    }

    @Test
    fun `Hent virkedag vanlig fredag`() {
        val vanligFredag = LocalDate.of(2020, 10, 30)
        VirkedagerProvider.nesteVirkedag(vanligFredag) shouldBe vanligFredag.plusDays(3)
    }

    @Test
    fun `Hent virkedag skjærtorsdag 2021`() {
        VirkedagerProvider.nesteVirkedag(skjærtorsdag2021) shouldBe skjærtorsdag2021.plusDays(5)
    }

    @Test
    fun `Hent virkedag skjærtorsdag 2022`() {
        VirkedagerProvider.nesteVirkedag(skjærtorsdag2022) shouldBe skjærtorsdag2022.plusDays(5)
    }
}
