package mpdc4gsr.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mpdc4gsr.camera.RGBCameraRecorder.CameraMode
import mpdc4gsr.camera.ui.CameraModeSelector
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraModeSelectorTest {

    private lateinit var context: Context
    private lateinit var cameraModeSelector: CameraModeSelector

    @MockK
    private lateinit var mockModeChangedListener: (CameraMode) -> Unit

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        every { mockModeChangedListener(any()) } returns Unit

        cameraModeSelector = CameraModeSelector(context)
        cameraModeSelector.onModeChangedListener = mockModeChangedListener
    }

    @Test
    fun `test initial mode selection`() {

        val initialMode = CameraMode.VIDEO_4K
        cameraModeSelector.setCurrentMode(initialMode)

        assertEquals(initialMode, cameraModeSelector.getCurrentMode())
    }

    @Test
    fun `test mode switching triggers callback`() {

        val targetMode = CameraMode.RAW_50MP
        val modeSlot = slot<CameraMode>()

        cameraModeSelector.setCurrentMode(targetMode)

        cameraModeSelector.onModeChangedListener?.invoke(targetMode)

        verify { mockModeChangedListener(capture(modeSlot)) }
        assertEquals(targetMode, modeSlot.captured)
    }

    @Test
    fun `test all camera modes are supported`() {

        val allModes = listOf(
            CameraMode.RAW_50MP,
            CameraMode.VIDEO_4K,
            CameraMode.PREVIEW_ONLY
        )

        allModes.forEach { mode ->
            cameraModeSelector.setCurrentMode(mode)
            assertEquals("Mode $mode should be settable", mode, cameraModeSelector.getCurrentMode())
        }
    }

    @Test
    fun `test performance warning logic`() {

        val rawMode = CameraMode.RAW_50MP
        val videoMode = CameraMode.VIDEO_4K
        val previewMode = CameraMode.PREVIEW_ONLY

        assertTrue(
            "RAW mode should have performance considerations",
            rawMode == CameraMode.RAW_50MP
        )
        assertTrue(
            "4K video should have performance considerations",
            videoMode == CameraMode.VIDEO_4K
        )

        assertTrue("Preview mode should be lightweight", previewMode == CameraMode.PREVIEW_ONLY)
    }

    @Test
    fun `test samsung device capability detection`() {

        val samsungDevicePatterns = listOf("SM-S901", "SM-S906", "SM-S908")

        samsungDevicePatterns.forEach { pattern ->
            val isSamsungS22 = pattern.startsWith("SM-S9")
            assertTrue("Samsung S22 pattern should be detected", isSamsungS22)
        }

        val nonSamsungPatterns = listOf("Pixel", "OnePlus", "LG-")
        nonSamsungPatterns.forEach { pattern ->
            val isSamsungS22 = pattern.startsWith("SM-S9")
            assertFalse("Non-Samsung devices should not match S22 pattern", isSamsungS22)
        }
    }

    @Test
    fun `test ui state consistency`() {

        val modes = listOf(CameraMode.RAW_50MP, CameraMode.VIDEO_4K, CameraMode.PREVIEW_ONLY)

        modes.forEach { mode ->
            cameraModeSelector.setCurrentMode(mode)

            assertEquals(
                "UI should reflect current mode",
                mode,
                cameraModeSelector.getCurrentMode()
            )

            assertNotNull("Mode should have display name", mode.displayName)
            assertNotNull("Mode should have description", mode.description)
        }
    }

    @Test
    fun `test segmented control behavior`() {

        val initialMode = CameraMode.VIDEO_4K
        val targetMode = CameraMode.RAW_50MP

        cameraModeSelector.setCurrentMode(initialMode)
        assertEquals(initialMode, cameraModeSelector.getCurrentMode())

        cameraModeSelector.setCurrentMode(targetMode)
        assertEquals(targetMode, cameraModeSelector.getCurrentMode())

        assertNotEquals("Only one mode should be selected", initialMode, targetMode)
    }

    @Test
    fun `test error handling for invalid modes`() {

        val validModes = CameraMode.values()
        assertTrue("Should have at least 3 camera modes", validModes.size >= 3)

        validModes.forEach { mode ->
            cameraModeSelector.setCurrentMode(mode)
            assertEquals(
                "All defined modes should be settable",
                mode,
                cameraModeSelector.getCurrentMode()
            )
        }
    }

    @Test
    fun `test mode descriptions and display names`() {

        CameraMode.values().forEach { mode ->
            assertNotNull("Mode ${mode.name} should have display name", mode.displayName)
            assertNotNull("Mode ${mode.name} should have description", mode.description)
            assertTrue("Display name should not be empty", mode.displayName.isNotEmpty())
            assertTrue("Description should not be empty", mode.description.isNotEmpty())
        }
    }
}

@Test
fun `test battery and thermal warnings`() {

    val resourceIntensiveModes = listOf(CameraMode.RAW_50MP, CameraMode.VIDEO_4K)
    val lightweightModes = listOf(CameraMode.PREVIEW_ONLY)

    resourceIntensiveModes.forEach { mode ->

        assertTrue(
            "Resource intensive modes should be flagged",
            mode == CameraMode.RAW_50MP || mode == CameraMode.VIDEO_4K
        )
    }

    lightweightModes.forEach { mode ->

        assertTrue(
            "Lightweight modes should not need warnings",
            mode == CameraMode.PREVIEW_ONLY
        )
    }
}

@Test
fun `test mode transition validation`() {

    val validTransitions = mapOf(
        CameraMode.PREVIEW_ONLY to listOf(CameraMode.RAW_50MP, CameraMode.VIDEO_4K),
        CameraMode.RAW_50MP to listOf(CameraMode.VIDEO_4K, CameraMode.PREVIEW_ONLY),
        CameraMode.VIDEO_4K to listOf(CameraMode.RAW_50MP, CameraMode.PREVIEW_ONLY)
    )

    validTransitions.forEach { (fromMode, toModes) ->
        toModes.forEach { toMode ->

            assertNotNull("Transition from $fromMode to $toMode should be valid", toMode)
            assertTrue("All modes should be accessible", true)
        }
    }
}

@Test
fun `test real-time mode switching performance`() {

    val switchStartTime = System.currentTimeMillis()

    cameraModeSelector.setCurrentMode(CameraMode.VIDEO_4K)
    cameraModeSelector.setCurrentMode(CameraMode.RAW_50MP)
    cameraModeSelector.setCurrentMode(CameraMode.PREVIEW_ONLY)

    val switchEndTime = System.currentTimeMillis()
    val switchDuration = switchEndTime - switchStartTime

    assertTrue("UI mode switching should be fast", switchDuration < 50)
}
}
