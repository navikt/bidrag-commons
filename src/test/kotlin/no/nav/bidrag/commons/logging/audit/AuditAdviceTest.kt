package no.nav.bidrag.commons.logging.audit

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import no.nav.bidrag.commons.security.ContextService
import no.nav.bidrag.commons.testdata.*
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.streng.Saksnummer
import no.nav.bidrag.transport.tilgang.Sporingsdata
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.CodeSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditAdviceTest {

    private val tilgangClient: TilgangClient = mockk(relaxed = true)
    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private val auditAdvice = AuditAdvice(auditLogger, tilgangClient)

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.erMaskinTilMaskinToken() } returns false
        every { tilgangClient.hentSporingsdataPerson(any()) } returns Sporingsdata("98754321", true)
        every { tilgangClient.hentSporingsdataSak(any()) } returns Sporingsdata("987654321", true)
    }

    @AfterEach
    fun clearMocks() {
        unmockkStatic(ContextService::class)
        clearAllMocks()
    }

    @Test
    fun `loggTilgang logger tilgang for saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(Saksnummer("0123456"))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(Personident(fnr))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.DELETE))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.DELETE, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("0123456")

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.CREATE))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.CREATE, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(fnr)

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.UPDATE))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.UPDATE, any()) }
    }

    @Test
    fun `loggTilgang feiler ved ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("654654")

        shouldThrow<IllegalStateException> { auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS)) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedSaksummerobjektFørst(Saksnummer("0123456")))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedPersonIdentobjektFørst(Personident(fnr)))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedStringFørst("0123456"))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedStringFørst(fnr))

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang feiler for requestBody med ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("852774")

        shouldThrow<IllegalStateException> { auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS)) }
    }

    @Test
    fun `loggTilgang logger tilgang for navngitt saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk()
        every { joinPoint.args } returns arrayOf("sdf", 321, Saksnummer("0123456"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "id"))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for navngitt personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf("sdf", 321, Personident(fnr))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "id"))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for navngitt saksnummerstring`() {
        val joinPoint: JoinPoint = mockk()
        every { joinPoint.args } returns arrayOf("sdf", 321, "0123456")
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "id"))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for navngitt personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf("sdf", 321, fnr)
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "id"))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang feiler ved navngitt ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf("sdf", 321, "654987")
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        shouldThrow<IllegalStateException> { auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "id")) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med navngitt saksnummerobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedSaksummerobjekt(saksnummer = Saksnummer("0123456")))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "saksnummer"))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med navngitt personIdentobjekt`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedPersonIdentobjekt(fnr = Personident(fnr)))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "fnr"))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med navngitt saksnummerstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = "0123456"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "fnr"))

        verify { tilgangClient.hentSporingsdataSak("0123456") }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang logger tilgang for requestBody med navngitt personIdentstring`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        val fnr = PersonidentGenerator.genererFødselsnummer()
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = fnr))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "fnr"))

        verify { tilgangClient.hentSporingsdataPerson(fnr) }
        verify { auditLogger.log(AuditLoggerEvent.ACCESS, any()) }
    }

    @Test
    fun `loggTilgang feiler for requestBody med navngitt ugyldig id-string`() {
        val joinPoint: JoinPoint = mockk(relaxed = true)
        every { joinPoint.args } returns arrayOf(DummyMedString(fnr = "852774"))
        val codeSignature: CodeSignature = mockk()
        every { codeSignature.parameterNames } returns arrayOf("dill", "dall", "id")
        every { joinPoint.signature } returns codeSignature

        shouldThrow<IllegalStateException> { auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS, "fnr")) }
    }

    @Test
    fun `loggTilgang gjør ingenting hvis maskin-til-maskin-kommunikasjon`() {
        every { ContextService.erMaskinTilMaskinToken() } returns true
        val joinPoint: JoinPoint = mockk(relaxed = true)

        auditAdvice.loggTilgang(joinPoint, AuditLog(AuditLoggerEvent.ACCESS))

        verify(exactly = 0) { tilgangClient.hentSporingsdataPerson(any()) }
        verify(exactly = 0) { tilgangClient.hentSporingsdataSak(any()) }
        verify(exactly = 0) { auditLogger.log(any(), any()) }
    }
}
