package mpdc4gsr.tests

import android.util.Log
import io.mockk.*
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.core.utils.AppLogger
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggingFunctionalityTest {
    private lateinit var structuredLogger: StructuredLogger

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        structuredLogger = mockk(relaxed = true)
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = false,
            structuredLoggerInstance = structuredLogger,
        )
    }

    @After
    fun teardown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun testDebugLoggingRespectesMinLevel() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.INFO)
        AppLogger.d("TEST", "Debug message")
        verify(exactly = 0) { Log.d("TEST", "Debug message") }
    }

    @Test
    fun testInfoLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.i("TEST", "Info message")
        verify(exactly = 1) { Log.i("TEST", "Info message") }
    }

    @Test
    fun testWarningLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.w("TEST", "Warning message")
        verify(exactly = 1) { Log.w("TEST", "Warning message") }
    }

    @Test
    fun testErrorLoggingWorks() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        AppLogger.e("TEST", "Error message")
        verify(exactly = 1) { Log.e("TEST", "Error message") }
    }

    @Test
    fun testLoggingWithThrowable() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.DEBUG)
        val exception = RuntimeException("Test exception")
        AppLogger.e("TEST", "Error with exception", exception)
        verify(exactly = 1) { Log.e("TEST", "Error with exception", exception) }
    }

    @Test
    fun testStructuredLoggingWhenEnabled() {
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = true,
            structuredLoggerInstance = structuredLogger,
        )
        AppLogger.e("TEST", "Error message", component = "TestComponent")
        verify(exactly = 1) {
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "TestComponent",
                "log_message",
                any(),
            )
        }
    }

    @Test
    fun testStructuredLoggingNotCalledWhenDisabled() {
        AppLogger.initialize(
            minLevel = AppLogger.LogLevel.DEBUG,
            enableStructured = false,
            structuredLoggerInstance = structuredLogger,
        )
        AppLogger.e("TEST", "Error message", component = "TestComponent")
        verify(exactly = 0) {
            structuredLogger.log(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun testVerboseLoggingRespectesMinLevel() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.INFO)
        AppLogger.v("TEST", "Verbose message")
        verify(exactly = 0) { Log.v("TEST", "Verbose message") }
    }

    @Test
    fun testVerboseLoggingWhenEnabled() {
        AppLogger.setMinLogLevel(AppLogger.LogLevel.VERBOSE)
        AppLogger.v("TEST", "Verbose message")
        verify(exactly = 1) { Log.v("TEST", "Verbose message") }
    }
}
