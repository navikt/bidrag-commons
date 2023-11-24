package no.nav.bidrag.commons.tilgang

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.testdata.DummyMedPersonIdentobjekt
import no.nav.bidrag.commons.testdata.DummyMedPersonIdentobjektFørst
import no.nav.bidrag.commons.testdata.DummyMedSaksummerobjekt
import no.nav.bidrag.commons.testdata.DummyMedSaksummerobjektFørst
import no.nav.bidrag.commons.testdata.DummyMedString
import no.nav.bidrag.commons.testdata.DummyMedStringFørst
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.CodeSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilgangAdviceTest {

    private val tilgangClient: TilgangClient = mockk(relaxed = true)

    private val tilgangAdvice = TilgangAdvice(tilgangClient)

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.erMaskinTilMaskinToken() } returns false
        every { tilgangClient.harTilgangSaksnummer(any()) } returns true
        every { tilgangClient.harTilgangPerson(any()) } returns true
    }

    @AfterEach
    fun clearMocks() {
        unmockkStatic(ContextService::class)
        clearAllMocks()
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(Saksnummer("0123456"))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(Personident(fnr))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("0123456")

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(fnr)

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang feiler ved ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("654654")

        shouldThrow<IllegalStateException> { tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll()) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedSaksummerobjektFørst(Saksnummer("0123456")))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedPersonIdentobjektFørst(Personident(fnr)))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedStringFørst("0123456"))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedStringFørst(fnr))

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang feiler for requestBody med ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("852774")

        shouldThrow<IllegalStateException> { tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll()) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for navngitt saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk()
        every { joinPoint.args } returns arrayOf("sdf", 321, Saksnummer("0123456"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("id"))

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for navngitt personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf("sdf", 321, Personident(fnr))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("id"))

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for navngitt saksnummerstring`() {
        val joinPoint: JoinPoint = mockk()
        every { joinPoint.args } returns arrayOf("sdf", 321, "0123456")
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("id"))

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for navngitt personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf("sdf", 321, fnr)
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("id"))

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang feiler ved navngitt ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("sdf", 321, "654987")
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        shouldThrow<IllegalStateException> { tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("id")) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med navngitt saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedSaksummerobjekt(saksnummer = Saksnummer("0123456")))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("saksnummer"))

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med navngitt personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedPersonIdentobjekt(fnr = Personident(fnr)))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("fnr"))

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med navngitt saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = "0123456"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("fnr"))

        verify { tilgangClient.harTilgangSaksnummer("0123456") }
    }

    @Test
    fun `sjekkTilgang sjekker tilgang for requestBody med navngitt personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = fnr))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("fnr"))

        verify { tilgangClient.harTilgangPerson(fnr) }
    }

    @Test
    fun `sjekkTilgang feiler for requestBody med navngitt ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = "852774"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        shouldThrow<IllegalStateException> { tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll("fnr")) }
    }

    @Test
    fun `sjekkTilgang gjør ingenting hvis maskin-til-maskin-kommunikasjon`() {
        every { ContextService.erMaskinTilMaskinToken() } returns true
        val joinPoint: JoinPoint = mockk(relaxed = true)

        tilgangAdvice.sjekkTilgang(joinPoint, Tilgangskontroll())

        verify(exactly = 0) { tilgangClient.harTilgangPerson(any()) }
        verify(exactly = 0) { tilgangClient.harTilgangSaksnummer(any()) }
    }
}
