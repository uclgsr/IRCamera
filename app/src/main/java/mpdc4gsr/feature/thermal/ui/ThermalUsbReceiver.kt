package mpdc4gsr.feature.thermal.ui
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
class ThermalUsbReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ThermalUsbReceiver"
        private const val USB_PERMISSION_ACTION = "mpdc4gsr.USB_PERMISSION"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        try {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    handleDeviceAttached(context, intent)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    handleDeviceDetached(context, intent)
                }
                USB_PERMISSION_ACTION -> {
                    handleUsbPermissionResult(context, intent)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling USB broadcast", e)
        }
    }
    private fun handleDeviceAttached(context: Context, intent: Intent) {
        val device =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
            }
        if (device != null) {
            Log.i(
                TAG,
                "USB device attached: ${device.productName} (VID=${device.vendorId.toString(16)}, PID=${
                    device.productId.toString(16)
                })"
            )
            if (device.isTcTsDevice()) {
                AppLogger.i(TAG, "Topdon thermal camera detected: ${device.productName}")
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val hasPermission = usbManager.hasPermission(device)
                if (hasPermission) {
                    AppLogger.i(TAG, "Thermal camera attached with existing permission")
                    DeviceEventManager.emitDeviceConnectionSync(true, device)
                } else {
                    AppLogger.i(TAG, "Thermal camera attached, requesting USB permission")
                    DeviceEventManager.emitDevicePermissionRequestSync(device)
                }
            } else {
                AppLogger.d(TAG, "Non-thermal USB device attached, ignoring")
            }
        }
    }
    private fun handleDeviceDetached(context: Context, intent: Intent) {
        val device =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
            }
        if (device != null) {
            Log.i(
                TAG,
                "USB device detached: ${device.productName} (VID=${device.vendorId.toString(16)}, PID=${
                    device.productId.toString(16)
                })"
            )
            if (device.isTcTsDevice()) {
                AppLogger.w(TAG, "Topdon thermal camera disconnected: ${device.productName}")
                DeviceEventManager.emitDeviceConnectionSync(false, device)
            }
        }
    }
    private fun handleUsbPermissionResult(context: Context, intent: Intent) {
        val device =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
            }
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        if (device != null) {
            AppLogger.i(TAG, "USB permission result for ${device.productName}: granted=$granted")
            if (device.isTcTsDevice()) {
                if (granted) {
                    AppLogger.i(TAG, "USB permission granted for thermal camera")
                    DeviceEventManager.emitDeviceConnectionSync(true, device)
                } else {
                    AppLogger.w(TAG, "USB permission denied for thermal camera")
                    DeviceEventManager.emitDevicePermissionRequestSync(device)
                }
            }
        }
    }
}
