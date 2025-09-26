package mpdc4gsr.sensors.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DevicePermissionEvent
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import org.greenrobot.eventbus.EventBus


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
        } catch (e: Exception) {        }
    }

    private fun handleDeviceAttached(context: Context, intent: Intent) {
        val device =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
            }

        if (device != null) {}, PID=${
                    device.productId.toString(16)
                })"
            )

            if (device.isTcTsDevice()) {                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val hasPermission = usbManager.hasPermission(device)

                if (hasPermission) {                    EventBus.getDefault().post(DeviceConnectEvent(true, device))
                } else {                    EventBus.getDefault().post(DevicePermissionEvent(device))
                }
            } else {            }
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

        if (device != null) {}, PID=${
                    device.productId.toString(16)
                })"
            )

            if (device.isTcTsDevice()) {                EventBus.getDefault().post(DeviceConnectEvent(false, device))
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

        if (device != null) {            if (device.isTcTsDevice()) {
                if (granted) {                    EventBus.getDefault().post(DeviceConnectEvent(true, device))
                } else {                    EventBus.getDefault().post(DevicePermissionEvent(device))
                }
            }
        }
    }
}
