package mpdc4gsr.core.common

import io.mockk.mockkObject
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ErrorHandlerTest {

    @Before
    fun setup() {
        mockkObject(AppLogger)
    }

    @Test
    fun `runSafely returns success for successful block`() {
        val result =
            ErrorHandler.runSafely(tag = "Test", operation = "compute") {
                21 * 2
            }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runSafely captures exceptions and logs them`() {
        val result =
            ErrorHandler.runSafely(tag = "Test", operation = "explode") {
                error("boom")
            }

        assertTrue(result.isFailure)
        verify { AppLogger.e("Test", match { it.contains("explode") }, any(), component = null) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `runSafelySuspendWithDefault returns default on failure`() = runTest {
        val value =
            ErrorHandler.runSafelySuspendWithDefault(
                tag = "Async",
                operation = "load",
                defaultValue = 7,
            ) {
                throw IllegalStateException("bad")
            }

        assertEquals(7, value)
    }
}

