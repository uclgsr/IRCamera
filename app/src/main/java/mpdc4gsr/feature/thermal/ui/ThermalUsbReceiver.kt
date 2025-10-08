package mpdc4gsr.feature.thermal.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager

class ThermalUsbReceiver : BroadcastReceiver() {
    companion object {
        private const val USB_PERMISSION_ACTION = "mpdc4gsr.USB_PERMISSION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
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
                TAG,
                "USB device attached: ${device.productName} (VID=${device.vendorId.toString(16)}, PID=${
                    device.productId.toString(16)
                })"
            )
            if (device.isTcTsDevice()) {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val hasPermission = usbManager.hasPermission(device)
                if (hasPermission) {
                    DeviceEventManager.emitDeviceConnectionSync(true, device)
                } else {
                    DeviceEventManager.emitDevicePermissionRequestSync(device)
                }
            } else {
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
                TAG,
                "USB device detached: ${device.productName} (VID=${device.vendorId.toString(16)}, PID=${
                    device.productId.toString(16)
                })"
            )
            if (device.isTcTsDevice()) {
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
            if (device.isTcTsDevice()) {
                if (granted) {
                    DeviceEventManager.emitDeviceConnectionSync(true, device)
                } else {
                    DeviceEventManager.emitDevicePermissionRequestSync(device)
                }
            }
        }
    }
}
