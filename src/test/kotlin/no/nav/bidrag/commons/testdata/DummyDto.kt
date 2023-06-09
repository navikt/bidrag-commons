package no.nav.bidrag.commons.testdata

import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.Saksnummer

data class DummyMedSaksummerobjektFørst(
    val saksnummer: Saksnummer,
    val dummy1: Int? = null,
    val dummy2: String? = null,
    val dummy3: Boolean? = null
)

data class DummyMedPersonIdentobjektFørst(
    val fnr: PersonIdent,
    val dummy1: Int? = null,
    val dummy2: String? = null,
    val dummy3: Boolean? = null
)

data class DummyMedStringFørst(
    val saksnummer: String,
    val dummy1: Int? = null,
    val dummy2: String? = null,
    val dummy3: Boolean? = null
)

data class DummyMedString(
    val dummy1: Int? = null,
    val dummy2: String? = null,
    val fnr: String,
    val dummy3: Boolean? = null
)

data class DummyMedSaksummerobjekt(
    val dummy1: Int? = null,
    val saksnummer: Saksnummer,
    val dummy2: String? = null,
    val dummy3: Boolean? = null
)

data class DummyMedPersonIdentobjekt(
    val dummy1: Int? = null,
    val fnr: PersonIdent,
    val dummy2: String? = null,
    val dummy3: Boolean? = null
)
