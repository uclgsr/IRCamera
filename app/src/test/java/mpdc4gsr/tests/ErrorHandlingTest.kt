package mpdc4gsr.tests

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorHandlingTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        mockkObject(AppLogger)
        every { AppLogger.e(any(), any(), any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun testRunSafelySuccessCase() {
        val result =
            ErrorHandler.runSafely("TEST", "test operation") {
                42
            }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyFailureCase() {
        val exception = RuntimeException("Test error")
        val result =
            ErrorHandler.runSafely("TEST", "test operation") {
                throw exception
            }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(exactly = 1) {
            AppLogger.e("TEST", "Failed to test operation: Test error", exception)
        }
    }

    @Test
    fun testRunSafelyWithDefaultSuccess() {
        val result =
            ErrorHandler.runSafelyWithDefault(
                "TEST",
                "test operation",
                0,
            ) {
                42
            }
        assertEquals(42, result)
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyWithDefaultFailure() {
        val exception = RuntimeException("Test error")
        val result =
            ErrorHandler.runSafelyWithDefault(
                "TEST",
                "test operation",
                0,
            ) {
                throw exception
            }
        assertEquals(0, result)
        verify(exactly = 1) {
            AppLogger.e(
                "TEST",
                "Failed to test operation: Test error, using default",
                exception,
            )
        }
    }

    @Test
    fun testRunSafelyIgnoreResultSuccess() {
        var executed = false
        ErrorHandler.runSafelyIgnoreResult("TEST", "test operation") {
            executed = true
        }
        assertTrue(executed)
        verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
    }

    @Test
    fun testRunSafelyIgnoreResultFailure() {
        val exception = RuntimeException("Test error")
        ErrorHandler.runSafelyIgnoreResult("TEST", "test operation") {
            throw exception
        }
        verify(exactly = 1) {
            AppLogger.e("TEST", "Failed to test operation: Test error", exception)
        }
    }

    @Test
    fun testRunSafelySuspendSuccess() =
        runBlocking {
            val result =
                ErrorHandler.runSafelySuspend("TEST", "test operation") {
                    42
                }
            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrNull())
            verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
        }

    @Test
    fun testRunSafelySuspendFailure() =
        runBlocking {
            val exception = RuntimeException("Test error")
            val result =
                ErrorHandler.runSafelySuspend("TEST", "test operation") {
                    throw exception
                }
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            verify(exactly = 1) {
                AppLogger.e("TEST", "Failed to test operation: Test error", exception)
            }
        }

    @Test
    fun testRunSafelySuspendWithDefaultSuccess() =
        runBlocking {
            val result =
                ErrorHandler.runSafelySuspendWithDefault(
                    "TEST",
                    "test operation",
                    0,
                ) {
                    42
                }
            assertEquals(42, result)
            verify(exactly = 0) { AppLogger.e(any(), any(), any(), any()) }
        }

    @Test
    fun testRunSafelySuspendWithDefaultFailure() =
        runBlocking {
            val exception = RuntimeException("Test error")
            val result =
                ErrorHandler.runSafelySuspendWithDefault(
                    "TEST",
                    "test operation",
                    0,
                ) {
                    throw exception
                }
            assertEquals(0, result)
            verify(exactly = 1) {
                AppLogger.e(
                    "TEST",
                    "Failed to test operation: Test error, using default",
                    exception,
                )
            }
        }
}
