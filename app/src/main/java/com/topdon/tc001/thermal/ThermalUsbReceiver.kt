package com.topdon.tc001.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.topdon.lib.core.bean.event.device.DeviceConnectEvent
import com.topdon.lib.core.bean.event.device.DevicePermissionEvent
import com.topdon.lib.core.config.DeviceConfig.isTcTsDevice
import org.greenrobot.eventbus.EventBus

/**
 * USB broadcast receiver for handling Topdon TC001 thermal camera device attach/detach events
 * and USB permission responses.
 */
class ThermalUsbReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ThermalUsbReceiver"
        private const val USB_PERMISSION_ACTION = "com.topdon.tc001.USB_PERMISSION"
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
            Log.e(TAG, "Error handling USB broadcast", e)
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
                Log.i(TAG, "Topdon thermal camera detected: ${device.productName}")

                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val hasPermission = usbManager.hasPermission(device)

                if (hasPermission) {
                    // Device attached with permission - notify via EventBus
                    Log.i(TAG, "Thermal camera attached with existing permission")
                    EventBus.getDefault().post(DeviceConnectEvent(true, device))
                } else {
                    // Device attached without permission - request permission
                    Log.i(TAG, "Thermal camera attached, requesting USB permission")
                    EventBus.getDefault().post(DevicePermissionEvent(device))
                }
            } else {
                Log.d(TAG, "Non-thermal USB device attached, ignoring")
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
                Log.w(TAG, "Topdon thermal camera disconnected: ${device.productName}")

                // Notify via EventBus that thermal camera was disconnected
                EventBus.getDefault().post(DeviceConnectEvent(false, device))
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
            Log.i(TAG, "USB permission result for ${device.productName}: granted=$granted")

            if (device.isTcTsDevice()) {
                if (granted) {
                    Log.i(TAG, "USB permission granted for thermal camera")
                    // Notify that device is connected with permission
                    EventBus.getDefault().post(DeviceConnectEvent(true, device))
                } else {
                    Log.w(TAG, "USB permission denied for thermal camera")
                    // Notify permission event for handling in ThermalCameraRecorder
                    EventBus.getDefault().post(DevicePermissionEvent(device))
                }
            }
        }
    }
}
