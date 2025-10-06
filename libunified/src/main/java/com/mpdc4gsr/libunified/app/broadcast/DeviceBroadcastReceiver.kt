package com.mpdc4gsr.libunified.app.broadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.tools.DeviceTools
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
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> XLog.v("$TAG ACTION_USB_DEVICE_ATTACHED")
            UsbManager.ACTION_USB_DEVICE_DETACHED -> XLog.v("$TAG ACTION_USB_DEVICE_DETACHED")
            ACTION_USB_PERMISSION -> XLog.v("$TAG ACTION_USB_PERMISSION")
            else -> XLog.v("$TAG ${intent.action}")
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
            XLog.e("$TAG Get UsbDevice error: ${e.message}")
            return
        }
        if (usbDevice == null) {
            XLog.w("$TAG usbDevice == null")
            return
        }
        XLog.v("$TAG usbDevice PRODUCT_ID = ${usbDevice.productId}, VENDOR_ID = ${usbDevice.vendorId}")
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
