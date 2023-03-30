package no.nav.bidrag.commons.web

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import java.lang.reflect.Type

internal class HttpHeaderRestTemplateTest {
    private val httpHeaderRestTemplate = HttpHeaderRestTemplate()

    private val appenderMock: Appender<ILoggingEvent> = mockk(relaxed = true)

    private val typeMock: Type = mockk(relaxed = true)

    @BeforeEach
    fun initMocks() {
        val logger = LoggerFactory.getLogger(HttpHeaderRestTemplate::class.java) as Logger
        every { appenderMock.name } returns "MOCK"
        every { appenderMock.isStarted } returns true
        logger.addAppender(appenderMock)
    }

    @Test
    fun `skal logge hvilke http headers den lager`() {
        httpHeaderRestTemplate.addHeaderGenerator("JUNIT_HEADER") { "header value" }
        httpHeaderRestTemplate.httpEntityCallback<Any>(null, typeMock)
        val argCapture = slot<ILoggingEvent>()
        verify { appenderMock.doAppend(capture(argCapture)) }
        val logMsg = argCapture.captured.toString()
        logMsg shouldContain "Generate header(s): [JUNIT_HEADER]"
    }

    @Test
    fun `skal logge navnet på eksisterende headers fra gitt request object`() {
        val existingHttpHeaders = HttpHeaders()
        existingHttpHeaders.add("EXISTING_HEADER", "existing value")
        httpHeaderRestTemplate.addHeaderGenerator("ADDITIONAL_HEADER") { "additional value" }
        httpHeaderRestTemplate.httpEntityCallback<Any>(HttpEntity<Any>(null, existingHttpHeaders), typeMock)
        val argCapture = ArrayList<ILoggingEvent>()
        verify(atLeast = 1) { appenderMock.doAppend(capture(argCapture)) }
        val logMsgs = argCapture.joinToString("\n")
        logMsgs shouldContain "Existing header(s): [EXISTING_HEADER]"
    }

    @Test
    fun `skal ikke feile når httpEntityCallback brukes med request body som er annet enn HttpEntity`() {
        httpHeaderRestTemplate.addHeaderGenerator("na") { "na" }
        httpHeaderRestTemplate.httpEntityCallback<Any>("a request body", typeMock)
        httpHeaderRestTemplate.httpEntityCallback<Any>(Any(), typeMock)
    }

    @Test
    fun `skal fjerne header generator`() {
        httpHeaderRestTemplate.addHeaderGenerator("EN_HEADER") { "header value" }
        httpHeaderRestTemplate.addHeaderGenerator("EN_ANNEN_HEADER") { "en annen header value" }
        httpHeaderRestTemplate.removeHeaderGenerator("EN_ANNEN_HEADER")
        httpHeaderRestTemplate.httpEntityCallback<Any>(null, typeMock)
        val argCapture = slot<ILoggingEvent>()
        verify { appenderMock.doAppend(capture(argCapture)) }
        val logMsg = argCapture.captured.toString()
        logMsg shouldContain "EN_HEADER" shouldNotContain "EN_ANNEN_HEADER"
    }
}
