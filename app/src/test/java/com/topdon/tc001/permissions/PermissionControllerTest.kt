package com.topdon.tc001.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PermissionControllerTest {

    private lateinit var mockActivity: FragmentActivity
    private lateinit var mockUsbManager: UsbManager
    private lateinit var permissionController: PermissionController

    @Before
    fun setup() {
        mockActivity = mockk(relaxed = true)
        mockUsbManager = mockk(relaxed = true)

        every { mockActivity.getSystemService(Context.USB_SERVICE) } returns mockUsbManager
        every { mockActivity.packageName } returns "com.topdon.tc001"

        permissionController = PermissionController(mockActivity)
        permissionController.initialize()
    }

    @After
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `test hasCameraPermission returns true when camera permission granted`() {
        // Mock camera permission as granted
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(permissionController.hasCameraPermission())
    }

    @Test
    fun `test hasCameraPermission returns false when camera permission denied`() {
        // Mock camera permission as denied
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED

        assertFalse(permissionController.hasCameraPermission())
    }

    @Test
    fun `test hasLocationPermission returns true when fine location granted`() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } returns PackageManager.PERMISSION_DENIED

        assertTrue(permissionController.hasLocationPermission())
    }

    @Test
    fun `test hasLocationPermission returns true when coarse location granted`() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(permissionController.hasLocationPermission())
    }

    @Test
    fun `test canStartRecording returns true when camera and storage permissions granted`() {
        mockkStatic(ContextCompat::class)

        // Mock camera permission
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        // Mock storage permissions for Android 13+
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.READ_MEDIA_VIDEO)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.READ_MEDIA_IMAGES)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(permissionController.canStartRecording())
    }

    @Test
    fun `test canConnectToShimmer returns true when bluetooth and location permissions granted`() {
        mockkStatic(ContextCompat::class)

        // Mock Android 12+ Bluetooth permissions
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_ADVERTISE)
        } returns PackageManager.PERMISSION_GRANTED

        // Mock location permissions
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(
                mockActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(permissionController.canConnectToShimmer())
    }

    @Test
    fun `test getMissingPermissions returns empty list when all permissions granted`() {
        mockkStatic(ContextCompat::class)

        // Mock all permissions as granted
        every {
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val missingPermissions = permissionController.getMissingPermissions()
        assertTrue(missingPermissions.isEmpty())
    }

    @Test
    fun `test getMissingPermissions returns camera permission when denied`() {
        mockkStatic(ContextCompat::class)

        // Mock camera permission as denied, others as granted
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED andThen PackageManager.PERMISSION_GRANTED

        val missingPermissions = permissionController.getMissingPermissions()
        assertTrue(missingPermissions.contains(Manifest.permission.CAMERA))
        assertTrue(missingPermissions.contains(Manifest.permission.RECORD_AUDIO))
    }

    @Test
    fun `test getPermissionStatusMessage returns appropriate status`() {
        mockkStatic(ContextCompat::class)

        // Mock camera permission as denied, others as granted
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        val statusMessage = permissionController.getPermissionStatusMessage()
        assertTrue(statusMessage.contains("Camera permission required"))
    }

    @Test
    fun `test USB permission request with valid device`() {
        val mockDevice = mockk<UsbDevice>(relaxed = true)
        every { mockDevice.productName } returns "Test Thermal Camera"
        every { mockDevice.vendorId } returns 0x1234
        every { mockDevice.productId } returns 0x5678
        every { mockUsbManager.hasPermission(mockDevice) } returns false

        var callbackResult: Pair<Boolean, UsbDevice?>? = null

        permissionController.requestUsbPermission(mockDevice) { granted, device ->
            callbackResult = Pair(granted, device)
        }

        // Verify that permission request was attempted
        verify { mockUsbManager.requestPermission(mockDevice, any()) }
    }

    @Test
    fun `test USB permission already granted`() {
        val mockDevice = mockk<UsbDevice>(relaxed = true)
        every { mockDevice.productName } returns "Test Thermal Camera"
        every { mockUsbManager.hasPermission(mockDevice) } returns true

        var callbackResult: Pair<Boolean, UsbDevice?>? = null

        permissionController.requestUsbPermission(mockDevice) { granted, device ->
            callbackResult = Pair(granted, device)
        }

        // Should return immediately without requesting permission
        assertEquals(true, callbackResult?.first)
        assertEquals(mockDevice, callbackResult?.second)
    }
}
