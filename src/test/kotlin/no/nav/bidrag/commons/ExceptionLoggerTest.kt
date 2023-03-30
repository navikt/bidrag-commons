package no.nav.bidrag.commons

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

internal class ExceptionLoggerTest {
    private var exceptionLogger = ExceptionLogger("bidrag-commons")
    private val logMeldinger: MutableList<ILoggingEvent> = ArrayList()

    private val appenderMock: Appender<ILoggingEvent> = mockk(relaxed = true)

    @BeforeEach
    fun mockLogAppender() {
        val logger = LoggerFactory.getLogger(ExceptionLogger::class.java) as Logger
//    every { appenderMock.doAppend(any()) } just runs
        every { appenderMock.name } returns "MOCK"
        every { appenderMock.isStarted } returns true
        logger.addAppender(appenderMock)
    }

    @Test
    fun `skal logge exception`() {
        Service().simulerServiceSomFeilerMedLoggingAvException()
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage }
            .shouldContain("IllegalStateException: test exception - caught in bidrag-commons within ExceptionLoggerTest")
            .shouldContain("|> no root cause")
    }

    @Test
    fun `skal logge root exception cause`() {
        exceptionLogger.logException(
            Exception("blew up", IllegalStateException("in common code", IllegalArgumentException("because of stupid arguments"))),
            "junit test"
        )
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage } shouldContain "blew up"
        logMeldinger.joinToString { it.formattedMessage } shouldContain
            "|> caused by java.lang.IllegalStateException, java.lang.IllegalArgumentException: because of stupid arguments"
    }

    @Test
    fun `skal logge exception uten exception cause`() {
        exceptionLogger.logException(Exception("the service blew up"), "junit test")
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage }
            .shouldContain("Exception: the service blew up - caught in bidrag-commons within junit test.")
            .shouldContain("Details:")
            .shouldContain("|> no root cause")
    }

    @Test
    fun `skal logge StackTraceElement fra no nav før exception`() {
        exceptionLogger.logException(
            Exception("blew up", IllegalStateException("in common code", IllegalArgumentException("because of stupid arguments"))),
            "junit test"
        )
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage }
            .shouldContain("|> kode i nav: no.nav.bidrag.commons.ExceptionLoggerTest.skal logge StackTraceElement fra no nav før exception(line:")
    }

    @Test
    fun `skal logge response body til et HttpStatusCodeException`() {
        exceptionLogger.logException(HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", "something is fishy".toByteArray(), null), "junit test")
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage } shouldContain "|> response body: something is fishy"
    }

    @Test
    fun `skal ikke logge response body til et HttpStatusCodeException når body er null`() {
        exceptionLogger.logException(HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", null, null), "junit test")
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage } shouldNotContain "Response body:"
    }

    @Test
    fun `skal kunne instansiere logger med klasser som ikke skal være del av logging`() {
        exceptionLogger = ExceptionLogger("bidrag-commons", OtherService::class.java)
        Service().simulerServiceSomFeilerMedLoggingAvException()
        verifiserLoggingSamtSamleLoggMeldinger()
        logMeldinger.joinToString { it.formattedMessage } shouldNotContain "OtherService"
    }

    @Test
    fun `skal returnere det som logges`() {
        val exceptionStreng = java.lang.String.join(
            "",
            exceptionLogger.logException(
                HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", "something is fishy".toByteArray(), null),
                "junit test"
            )
        )
        exceptionStreng shouldContain "|> response body: something is fishy"
    }

    private fun verifiserLoggingSamtSamleLoggMeldinger() {
        verify(atLeast = 1) { appenderMock.doAppend(capture(logMeldinger)) }
    }

    @Test
    fun `skal logge en error per exception`() {
        exceptionLogger.logException(
            HttpClientErrorException(HttpStatus.BAD_REQUEST, "oops", "i did it again".toByteArray(), null),
            "junit test"
        )
        fetchNumberOfLoglevelError() shouldBe 1
    }

    private fun fetchNumberOfLoglevelError(): Int {
        val eventCaptor = ArrayList<ILoggingEvent>()
        verify(atLeast = 1) { appenderMock.doAppend(capture(eventCaptor)) }
        return eventCaptor.map { it.level }.count { it == Level.ERROR }
    }

    private inner class Service {
        fun simulerServiceSomFeilerMedLoggingAvException() {
            OtherService(exceptionLogger).loggExceptionMedExceptionLogger()
        }
    }

    private class OtherService internal constructor(private val exceptionLogger: ExceptionLogger) {
        fun loggExceptionMedExceptionLogger() {
            exceptionLogger.logException(ILLEGAL_STATE_EXCEPTION, "ExceptionLoggerTest")
        }
    }

    companion object {
        private val ILLEGAL_STATE_EXCEPTION = IllegalStateException("test exception")
    }
}
