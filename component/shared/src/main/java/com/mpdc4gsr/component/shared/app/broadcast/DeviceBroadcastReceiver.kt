package com.mpdc4gsr.component.shared.app.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mpdc4gsr.component.shared.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.component.shared.app.event.DeviceEventManager
import com.mpdc4gsr.component.shared.app.tools.DeviceTools

class DeviceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = this.javaClass.simpleName

    companion object {
        const val ACTION_USB_PERMISSION = "com.mpdc4gsr.topInfrared.USB_PERMISSION"
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> Unit
            UsbManager.ACTION_USB_DEVICE_DETACHED -> Unit
            ACTION_USB_PERMISSION -> Unit
            else -> Unit
        }
        if (intent.action == ACTION_USB_PERMISSION) {
            DeviceTools.isConnect(isSendConnectEvent = true, isAutoRequest = false)
        } else {
            handleUsbEvent(intent)
        }
    }

    private fun handleUsbEvent(intent: Intent) {
        val usbDevice: UsbDevice?
        try {
            @Suppress("DEPRECATION")
            usbDevice = intent.extras!!["device"] as UsbDevice?
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        if (usbDevice == null) {
            return
        }
        if (usbDevice.isTcTsDevice()) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                DeviceTools.isConnect(isSendConnectEvent = true, isAutoRequest = true)
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                DeviceEventManager.emitDeviceConnectionSync(false, null)
            }
        }
    }
}


