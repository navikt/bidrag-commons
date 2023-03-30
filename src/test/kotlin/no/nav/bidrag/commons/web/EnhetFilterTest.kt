package no.nav.bidrag.commons.web

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponseWrapper

internal class EnhetFilterTest {
    private val enhetFilter = EnhetFilter()

    private val appenderMock: Appender<ILoggingEvent> = mockk(relaxed = true)
    private val filterChainMock: FilterChain = mockk(relaxed = true)
    private val httpServletRequestMock: HttpServletRequest = mockk(relaxed = true)
    private val httpServletResponseMock: HttpServletResponseWrapper = mockk(relaxed = true)

    @BeforeEach
    fun initMocksAndMockLogAppender() {
        mockLogAppender()
        mockRequestUri()
    }

    private fun mockLogAppender() {
        val logger = LoggerFactory.getLogger(EnhetFilter::class.java) as Logger
        every { appenderMock.name } returns "MOCK"
        every { appenderMock.isStarted } returns true
        logger.addAppender(appenderMock)
    }

    private fun mockRequestUri() {
        every { httpServletRequestMock.requestURI } returns "some url"
    }

    @Test
    fun `skal videresende X_ENHETSNR_HEADER fra request til response`() {
        every { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) } returns "enhetsnummer"
        enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) }
        verify { httpServletResponseMock.addHeader(EnhetFilter.X_ENHET_HEADER, "enhetsnummer") }
    }

    @Test
    fun `skal ikke videresende X_ENHETSNR_HEADER fra request til response når headerverdi ikke finnes på request`() {
        every { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) } returns null
        enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) }
        verify(exactly = 0) { httpServletResponseMock.addHeader(any(), any()) }
    }

    @Test
    fun `skal logge enhetsnummer som skal videresendes`() {
        every { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) } returns "007"
        enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        val logCaptor = slot<ILoggingEvent>()
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage shouldContain "Behandler request 'some url' for enhet med enhetsnummer 007"
        EnhetFilter.Companion.fetchForThread() shouldBe "007"
    }

    @Test
    fun `skal logge når et enhetsnummer ikke kan videresendes`() {
        every { httpServletRequestMock.getHeader(EnhetFilter.X_ENHET_HEADER) } returns null

        enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)

        val logCaptor = slot<ILoggingEvent>()
        verify { appenderMock.doAppend(capture(logCaptor)) }
        val loggingEvent = logCaptor.captured
        loggingEvent shouldNotBe null
        loggingEvent.formattedMessage shouldContain "Behandler request 'some url' uten informasjon om enhetsnummer"
    }

    @Test
    fun `skal fortsette filtrering av request etter at filter er kjørt`() {
        enhetFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify { filterChainMock.doFilter(httpServletRequestMock, httpServletResponseMock) }
    }
}
