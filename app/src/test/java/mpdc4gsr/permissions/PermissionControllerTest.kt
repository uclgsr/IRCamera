package mpdc4gsr.permissions

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
        every { mockActivity.packageName } returns "com.mpdc4gsr.tc001"

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


        assertEquals(true, callbackResult?.first)
        assertEquals(mockDevice, callbackResult?.second)
    }

    @Test
    fun `test getCriticalPermissions excludes location permissions`() {
        val testPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val criticalPermissions = permissionController.getCriticalPermissions(testPermissions)
        
        assertTrue(criticalPermissions.contains(Manifest.permission.CAMERA))
        assertTrue(criticalPermissions.contains(Manifest.permission.BLUETOOTH_SCAN))
        assertFalse(criticalPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertFalse(criticalPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertFalse(criticalPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    @Test
    fun `test getLocationPermissions filters location permissions correctly`() {
        val testPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val locationPermissions = permissionController.getLocationPermissions(testPermissions)
        
        assertEquals(2, locationPermissions.size)
        assertTrue(locationPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(locationPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertFalse(locationPermissions.contains(Manifest.permission.CAMERA))
        assertFalse(locationPermissions.contains(Manifest.permission.BLUETOOTH_SCAN))
    }

    @Test
    fun `test canConnectToShimmerLimited returns true with bluetooth permissions only`() {
        mockkStatic(ContextCompat::class)

        // Mock Bluetooth permissions as granted, location as denied
        mockBluetoothPermissions(granted = true)
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        assertTrue(permissionController.canConnectToShimmerLimited())
        assertFalse(permissionController.canConnectToShimmer())
    }

    @Test
    fun `test getBluetoothConnectionStatus provides appropriate message`() {
        mockkStatic(ContextCompat::class)

        // Test with Bluetooth permissions but no location permission
        mockBluetoothPermissions(granted = true)
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        val status = permissionController.getBluetoothConnectionStatus()
        assertTrue(status.contains("Limited Bluetooth functionality"))
        assertTrue(status.contains("device scanning may not work"))
    }

    @Test
    fun `test isLocationPermissionPermanentlyDenied detects permanent denial correctly`() {
        mockkStatic(ContextCompat::class)

        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            mockActivity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        } returns false

        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            mockActivity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns false

        assertTrue(permissionController.isLocationPermissionPermanentlyDenied())
    }

    @Test
    fun `test permission request throttling mechanism`() {
        // Mock permissions as missing
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_DENIED

        var callbackCount = 0
        val callback = { granted: Boolean, denied: List<String> ->
            callbackCount++
        }

        // Simulate user declining permission rationale multiple times
        every { mockActivity.shouldShowRequestPermissionRationale(any()) } returns true
        
        // First few requests should go through until limit is reached
        permissionController.ensureAll(callback)
        Thread.sleep(100) // Small delay to avoid race conditions
        permissionController.ensureAll(callback)
        Thread.sleep(100)
        permissionController.ensureAll(callback)
        Thread.sleep(100)
        
        // After maximum attempts, should start throttling
        permissionController.ensureAll(callback)
        
        // Verify throttling is active
        assertTrue(permissionController.shouldSkipPermissionRequest())
    }

    @Test
    fun `test hasMinimumPermissions for basic functionality`() {
        // Mock basic permissions as granted
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_GRANTED

        assertTrue(permissionController.hasMinimumPermissions())
    }

    @Test
    fun `test resetPermissionState clears throttling`() {
        // Mock permissions as missing to trigger throttling
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(mockActivity, any())
        } returns PackageManager.PERMISSION_DENIED

        // Trigger throttling by making multiple requests
        val callback = { granted: Boolean, denied: List<String> -> }
        repeat(4) {
            permissionController.ensureAll(callback)
            Thread.sleep(50)
        }

        // Verify throttling is active
        assertTrue(permissionController.shouldSkipPermissionRequest())

        // Reset state
        permissionController.resetPermissionState()

        // Verify throttling is cleared
        assertFalse(permissionController.shouldSkipPermissionRequest())
    }

    private fun mockBluetoothPermissions(granted: Boolean) {
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_SCAN)
        } returns result
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_CONNECT)
        } returns result
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.BLUETOOTH_ADVERTISE)
        } returns result
    }
}
