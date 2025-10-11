package mpdc4gsr.core.common

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import mpdc4gsr.core.common.logging.StructuredLogger
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppLoggerTest {

    private val structuredLogger = mockk<StructuredLogger>(relaxed = true)

    @Before
    fun setUp() {
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = true,
            structuredLoggerInstance = structuredLogger,
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `info level logs surface to structured logger`() {
        AppLogger.i(tag = "Test", message = "Processing complete", component = "capture")

        verify {
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "capture",
                "log_message",
                withArg { details ->
                    require(details["message"] == "Processing complete")
                },
            )
        }
    }

    @Test
    fun `sub-debug logs are filtered when min level raised`() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.ERROR)
        AppLogger.w(tag = "Test", message = "Should be filtered", component = "pipeline")

        verify(exactly = 0) { structuredLogger.log(any(), any(), any(), any()) }
    }
}

