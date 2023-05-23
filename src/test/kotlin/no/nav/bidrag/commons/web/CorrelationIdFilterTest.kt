package no.nav.bidrag.commons.web

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponseWrapper
import no.nav.bidrag.commons.web.CorrelationIdFilter.Companion.fetchCorrelationIdForThread
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException

internal class CorrelationIdFilterTest {
    private val correlationIdFilter = CorrelationIdFilter()
    private val logMeldinger: MutableList<ILoggingEvent> = ArrayList()

    private val appenderMock: Appender<ILoggingEvent> = mockk(relaxed = true)

    private val filterChainMock: FilterChain = mockk(relaxed = true)

    private val httpServletRequestMock: HttpServletRequest = mockk(relaxed = true)

    private val httpServletResponseMock: HttpServletResponseWrapper = mockk(relaxed = true)

    @BeforeEach
    fun mockLogAppender() {
        val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java) as Logger
        logger.level = Level.DEBUG
        every { appenderMock.name } returns "MOCK"
        every { appenderMock.isStarted } returns true
        logger.addAppender(appenderMock)
    }

    @Test
    fun `skal logge requests mot servlet`() {
        every { httpServletRequestMock.requestURI } returns "something"
        every { httpServletRequestMock.method } returns "GET"
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify(atLeast = 1) { appenderMock.doAppend(capture(logMeldinger)) }
        logMeldinger.joinToString { it.formattedMessage } shouldContain "prosessing GET something"
    }

    @Test
    fun `skal ikke logge requests mot actuator endpoints`() {
        every { httpServletRequestMock.requestURI } returns "/actuator/health" andThen "/actuator/something"
        every { httpServletRequestMock.method } returns "GET"
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify(exactly = 0) { appenderMock.doAppend(any()) }
    }

    @Test
    fun `skal ikke logge requests etter api-docs`() {
        every { httpServletRequestMock.requestURI } returns "/v3/api-docs/" andThen "/v2/api-docs/"
        every { httpServletRequestMock.method } returns "GET"
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify(exactly = 0) { appenderMock.doAppend(any()) }
    }

    @Test
    fun `skal ikke logge requests mot swagger resources`() {
        every { httpServletRequestMock.requestURI } returns "/swagger-ui-bundle.js" andThen "/swagger-config"
        every { httpServletRequestMock.method } returns "GET"
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify(exactly = 0) { appenderMock.doAppend(any()) }
    }

    @Test
    fun `skal legge HttpHeader CORRELATION_ID på response`() {
        every { httpServletRequestMock.requestURI } returns "somewhere"
        every { httpServletRequestMock.method } returns "GET"
        every { httpServletRequestMock.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify { httpServletResponseMock.addHeader(CORRELATION_ID, any()) }
    }

    @Test
    fun `skal ikke legge HttpHeader CORRELATION_ID på response når den allerede eksisterer`() {
        every { httpServletRequestMock.requestURI } returns "somewhere else"
        every { httpServletRequestMock.method } returns "GET"
        every { httpServletRequestMock.getHeader(CORRELATION_ID) } returns "svada"
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        verify(exactly = 0) { httpServletResponseMock.addHeader(any(), any()) }
    }

    @Test
    fun `skal bruke request uri som correlation id`() {
        every { httpServletRequestMock.requestURI } returns "somewhere"
        every { httpServletRequestMock.method } returns "GET"
        every { httpServletRequestMock.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        val correlationCaptor = slot<String>()
        verify { httpServletResponseMock.addHeader(CORRELATION_ID, capture(correlationCaptor)) }
        correlationCaptor.captured shouldContain "-somewhere"
    }

    @Test
    fun `skal lage correlation id av siste del fra request uri`() {
        every { httpServletRequestMock.requestURI } returns "/en/forbanna/journalpost"
        every { httpServletRequestMock.method } returns "GET"
        every { httpServletRequestMock.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        val correlationCaptor = slot<String>()
        verify { httpServletResponseMock.addHeader(CORRELATION_ID, capture(correlationCaptor)) }
        correlationCaptor.captured shouldContain "-journalpost"
    }

    @Test
    fun `skal lage correlation id av siste del fra request uri som er ren tekst, samt legge på eventuelle tall`() {
        every { httpServletRequestMock.requestURI } returns "/en/identifisert/journalpost/1001"
        every { httpServletRequestMock.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        val correlationCaptor = slot<String>()
        verify { httpServletResponseMock.addHeader(CORRELATION_ID, capture(correlationCaptor)) }
        correlationCaptor.captured shouldContain "-journalpost/1001"
    }

    @Test
    fun `skal lage correlation id av siste del fra request uri som er ren tekst, samt legge på eventuelle prefiksede tall`() {
        every { httpServletRequestMock.requestURI } returns "/en/identifisert/journalpost/BID-1001"
        every { httpServletRequestMock.method } returns "GET"
        every { httpServletRequestMock.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock)
        val correlationCaptor = slot<String>()
        verify { httpServletResponseMock.addHeader(CORRELATION_ID, capture(correlationCaptor)) }
        correlationCaptor.captured shouldContain "-journalpost/BID-1001"
    }

    @Test
    fun `skal legge correlation id på ThreadLocal som kan leses for konfigurasjon`() {
        every { httpServletRequestMock.requestURI } returns "go somewhere"
        val aCorrelationIdThread =
            CorrelationIdThread { correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock) }
        val anotherCorrelationIdThread =
            CorrelationIdThread { correlationIdFilter.doFilter(httpServletRequestMock, httpServletResponseMock, filterChainMock) }
        aCorrelationIdThread.start()
        Thread.sleep(10) // to be sure the value is not from the same millis
        anotherCorrelationIdThread.start()
        aCorrelationIdThread.join()
        anotherCorrelationIdThread.join()
        aCorrelationIdThread.correlationId shouldNotBe null shouldNotBe anotherCorrelationIdThread.correlationId
        anotherCorrelationIdThread.correlationId shouldNotBe null shouldNotBe aCorrelationIdThread.correlationId
    }

    internal class CorrelationIdThread(private val filterExecutor: FilterExecutor) : Thread() {
        var correlationId: String? = null
        override fun run() {
            correlationId = try {
                filterExecutor.doFilter()
                fetchCorrelationIdForThread()
            } catch (e: IOException) {
                throw AssertionError(e)
            } catch (e: ServletException) {
                throw AssertionError(e)
            }
        }
    }

    internal fun interface FilterExecutor {

        fun doFilter()
    }

    companion object {
        private const val CORRELATION_ID = "X-Correlation-ID"
    }
}
