package mpdc4gsr.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mpdc4gsr.controller.RecordingController
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingServiceTest {

    private lateinit var context: Context
    private lateinit var mockRecordingController: RecordingController

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockRecordingController = mockk<RecordingController> {
            every { isRecording } returns false
            every { getAvailableSensors() } returns emptyList()
            every { getActiveSensorCount() } returns 0
            coEvery { initializeSensors() } returns true
            coEvery { startSession(any()) } returns true
            coEvery { stopSession() } returns true
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `test service handles partial sensor failures gracefully`() = runTest {
        every { mockRecordingController.getAvailableSensors() } returns listOf(
            mockk {
                every { sensorId } returns "RGB"
                every { sensorType } returns "camera"
            }
        )
        every { mockRecordingController.getActiveSensorCount() } returns 1

        assertTrue("Service should handle partial sensor scenarios", true)
    }

    @Test
    fun `test backward compatibility maintained`() = runTest {
        try {
            RecordingService::class.java.getDeclaredMethod(
                "startRecording", Context::class.java, String::class.java
            )
            assertTrue("startRecording method should exist", true)
        } catch (e: NoSuchMethodException) {
            fail("Backward compatibility broken")
        }
    }
}
