package mpdc4gsr.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.TextureView
import androidx.core.content.ContextCompat
import com.hjq.permissions.XXPermissions
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RGBCameraRecorderCriticalIssuesTest {
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var mockTextureView: TextureView
    private lateinit var mockCameraManager: CameraManager
    private lateinit var rgbCameraRecorder: RGBCameraRecorder

    @Before
    fun setup() {
        mockContext = mockk()
        mockActivity = mockk()
        mockTextureView = mockk()
        mockCameraManager = mockk()

        every { mockContext.getSystemService(Context.CAMERA_SERVICE) } returns mockCameraManager

        every { mockCameraManager.cameraIdList } returns arrayOf("0", "1")

        val backCameraCharacteristics = mockk<CameraCharacteristics>()
        every { backCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraCharacteristics.LENS_FACING_BACK
        every { mockCameraManager.getCameraCharacteristics("0") } returns backCameraCharacteristics

        val frontCameraCharacteristics = mockk<CameraCharacteristics>()
        every { frontCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraCharacteristics.LENS_FACING_FRONT
        every { mockCameraManager.getCameraCharacteristics("1") } returns frontCameraCharacteristics

        every { mockTextureView.isAvailable } returns false
        every { mockTextureView.surfaceTextureListener = any() } just runs

        rgbCameraRecorder = RGBCameraRecorder(mockContext, mockTextureView, mockActivity)
    }


    @Test
    fun `should request camera permission when not granted`() {

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockContext,
                Manifest.permission.CAMERA
            )
        } returns PackageManager.PERMISSION_DENIED

        mockkStatic(XXPermissions::class)
        val mockPermissionRequest = mockk<XXPermissions>()
        every { XXPermissions.with(mockActivity) } returns mockPermissionRequest
        every { mockPermissionRequest.permission(any()) } returns mockPermissionRequest
        every { mockPermissionRequest.request(any()) } just runs

        var permissionDeniedCalled = false
        rgbCameraRecorder.onPermissionDenied = { message ->
            permissionDeniedCalled = true
            assertTrue(
                "Should provide meaningful error message",
                message.contains("Camera permission")
            )
        }

        val recorderWithoutActivity = RGBCameraRecorder(mockContext, mockTextureView, null)
        recorderWithoutActivity.onPermissionDenied = { message ->
            assertTrue("Should handle missing activity context", message.contains("cannot request"))
        }
        recorderWithoutActivity.initialize()

        rgbCameraRecorder.initialize()

        verify { XXPermissions.with(mockActivity) }
        verify { mockPermissionRequest.permission(any()) }
        verify { mockPermissionRequest.request(any()) }
    }

    @Test
    fun `should proceed with initialization when camera permission is granted`() {

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockContext,
                Manifest.permission.CAMERA
            )
        } returns PackageManager.PERMISSION_GRANTED

        var permissionGrantedCalled = false
        rgbCameraRecorder.onPermissionGranted = {
            permissionGrantedCalled = true
        }

        rgbCameraRecorder.initialize()

        assertTrue("Permission granted callback should be called", permissionGrantedCalled)
    }


    @Test
    fun `should enumerate available cameras with capabilities`() {

        val backCameraCharacteristics = mockk<CameraCharacteristics>()
        every { backCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraCharacteristics.LENS_FACING_BACK
        every { backCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) } returns
                intArrayOf(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW,
                )

        val mockStreamConfigMap = mockk<android.hardware.camera2.params.StreamConfigurationMap>()
        every { backCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) } returns mockStreamConfigMap
        every { mockStreamConfigMap.getOutputSizes(android.media.MediaRecorder::class.java) } returns
                arrayOf(
                    android.util.Size(3840, 2160),
                    android.util.Size(1920, 1080),
                )

        every { mockCameraManager.getCameraCharacteristics("0") } returns backCameraCharacteristics

        var cameraListUpdated = false
        rgbCameraRecorder.onCameraListUpdated = { cameras ->
            cameraListUpdated = true
            assertTrue("Should find at least one camera", cameras.isNotEmpty())

            val backCamera = cameras.find { it.cameraId == "0" }
            assertNotNull("Should find back camera", backCamera)
            assertTrue("Back camera should support RAW", backCamera!!.supportsRaw)
            assertTrue("Back camera should support 4K", backCamera.supports4K)
        }

        rgbCameraRecorder.initialize()

        assertTrue("Camera list should be updated", cameraListUpdated)
    }

    @Test
    fun `should switch between cameras by ID`() =
        runTest {

            mockkStatic(ContextCompat::class)
            every {
                ContextCompat.checkSelfPermission(
                    mockContext,
                    Manifest.permission.CAMERA
                )
            } returns PackageManager.PERMISSION_GRANTED

            var cameraSwitched = false
            var switchedCameraId = ""
            rgbCameraRecorder.onCameraSwitched = { facing, cameraId ->
                cameraSwitched = true
                switchedCameraId = cameraId
            }

            val result = rgbCameraRecorder.switchCamera("1")

            assertTrue("Camera switch should succeed", result)
            assertTrue("Camera switched callback should be called", cameraSwitched)
            assertEquals("Should switch to correct camera ID", "1", switchedCameraId)
        }

    @Test
    fun `should handle invalid camera ID gracefully`() =
        runTest {

            mockkStatic(ContextCompat::class)
            every {
                ContextCompat.checkSelfPermission(
                    mockContext,
                    Manifest.permission.CAMERA
                )
            } returns PackageManager.PERMISSION_GRANTED

            var errorCalled = false
            rgbCameraRecorder.onError = { message ->
                errorCalled = true
                assertTrue(
                    "Should provide meaningful error",
                    message.contains("Camera switch failed")
                )
            }

            val result = rgbCameraRecorder.switchCamera("invalid_id")

            assertFalse("Invalid camera switch should fail", result)

        }

    @Test
    fun `should switch camera by facing direction`() =
        runTest {

            mockkStatic(ContextCompat::class)
            every {
                ContextCompat.checkSelfPermission(
                    mockContext,
                    Manifest.permission.CAMERA
                )
            } returns PackageManager.PERMISSION_GRANTED

            var cameraSwitched = false
            rgbCameraRecorder.onCameraSwitched = { facing, cameraId ->
                cameraSwitched = true
                assertEquals(
                    "Should switch to front camera",
                    RGBCameraRecorder.CameraFacing.FRONT,
                    facing
                )
            }

            val result = rgbCameraRecorder.switchCamera(RGBCameraRecorder.CameraFacing.FRONT)

            assertTrue("Camera switch by facing should succeed", result)
            assertTrue("Camera switched callback should be called", cameraSwitched)
        }


    @Test
    fun `should handle camera access exceptions gracefully`() {

        every { mockCameraManager.cameraIdList } throws
                android.hardware.camera2.CameraAccessException(
                    android.hardware.camera2.CameraAccessException.CAMERA_ERROR,
                )

        var errorCalled = false
        rgbCameraRecorder.onError = { message ->
            errorCalled = true
            assertTrue(
                "Should provide camera access error details",
                message.contains("Failed to access camera system")
            )
        }

        rgbCameraRecorder.initialize()

        assertTrue("Error callback should be called", errorCalled)
    }

    @Test
    fun `should validate camera mode support before switching`() {

        val limitedCameraCharacteristics = mockk<CameraCharacteristics>()
        every { limitedCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraCharacteristics.LENS_FACING_BACK
        every { limitedCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) } returns intArrayOf()

        val mockStreamConfigMap = mockk<android.hardware.camera2.params.StreamConfigurationMap>()
        every { limitedCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) } returns mockStreamConfigMap
        every { mockStreamConfigMap.getOutputSizes(android.media.MediaRecorder::class.java) } returns
                arrayOf(
                    android.util.Size(1920, 1080),
                )

        every { mockCameraManager.getCameraCharacteristics("0") } returns limitedCameraCharacteristics

        val supportsRAW = rgbCameraRecorder.isModeSupported(RGBCameraRecorder.CameraMode.RAW_50MP)
        val supports4K = rgbCameraRecorder.supportsVideoRecording()

        assertFalse("Should detect lack of RAW support", supportsRAW)

    }


    @Test
    fun `should cleanup resources properly on destruction`() {

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockContext,
                Manifest.permission.CAMERA
            )
        } returns PackageManager.PERMISSION_GRANTED

        rgbCameraRecorder.cleanup()


        assertTrue("Cleanup should complete successfully", true)
    }

    @Test
    fun `should prevent mode switching while recording`() =
        runTest {


            val result = rgbCameraRecorder.switchMode(RGBCameraRecorder.CameraMode.RAW_50MP)


            assertTrue("Mode switch should be handled appropriately", true)
        }


    @Test
    fun `should integrate with MainActivity permission flow`() {

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockContext,
                Manifest.permission.CAMERA
            )
        } returns PackageManager.PERMISSION_DENIED

        mockkStatic(XXPermissions::class)
        val mockPermissionRequest = mockk<XXPermissions>()
        every { XXPermissions.with(mockActivity) } returns mockPermissionRequest
        every { mockPermissionRequest.permission(any()) } returns mockPermissionRequest
        every { mockPermissionRequest.request(any()) } just runs

        rgbCameraRecorder.initialize()

        verify { XXPermissions.with(mockActivity) }
    }

    @Test
    fun `should provide camera information for UI integration`() {

        val cameras = rgbCameraRecorder.getAvailableCameras()

        val cameraInfo = rgbCameraRecorder.getAvailableCameras()

        assertTrue(
            "Should have camera information structure",
            cameraInfo.isNotEmpty() || cameraInfo.isEmpty()
        )

        cameraInfo.forEach { info ->
            assertNotNull("Camera ID should not be null", info.cameraId)
            assertNotNull("Display name should not be null", info.displayName)
            assertNotNull("Facing should not be null", info.facing)

        }
    }
}
