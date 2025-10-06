@file:OptIn(kotlin.ExperimentalStdlibApi::class)
package com.mpdc4gsr.libunified.app.tools
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isHik256
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcLiteDevice
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.utils.ByteUtils
object DeviceTools {
    fun isConnect(
        isSendConnectEvent: Boolean = false,
        isAutoRequest: Boolean = true,
    ): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcTsDevice()) {
                return if (usbManager.hasPermission(usbDevice)) {
                    XLog.i("[ph][ph][ph][ph][ph][ph][ph][ph][ph]")
                    if (isSendConnectEvent) {
                        DeviceEventManager.emitDeviceConnectionSync(true, usbDevice)
                    }
                    true
                } else {
                    XLog.w("[ph][ph][ph][ph][ph][ph][ph][ph][ph]")
                    if (isAutoRequest) {
                        DeviceEventManager.emitDevicePermissionRequestSync(usbDevice)
                    }
                    false
                }
            }
        }
        return false
    }
    fun findUsbDevice(): UsbDevice? {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcTsDevice()) {
                val productID = ByteUtils.toHexString(
                    ByteUtils.numberToBytes(
                        true,
                        usbDevice.productId.toLong(),
                        2
                    )
                )
                val vendorID = ByteUtils.toHexString(
                    ByteUtils.numberToBytes(
                        true,
                        usbDevice.vendorId.toLong(),
                        2
                    )
                )
                XLog.i("[ph][ph][ph][ph]usb[ph][ph] productId:$productID, vendorId:$vendorID, deviceName:${usbDevice.deviceName}")
                return usbDevice
            }
        }
        XLog.i("[ph][ph][ph]${deviceList.size}[ph][ph][ph], [ph][ph][ph][ph][ph][ph]usb[ph][ph]")
        return null
    }
    fun isTC001PlusConnect(): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        var usbCameraNumber = 0
        var isTcTsDev = false
        for (usbDevice in deviceList.values) {
            if ("USB Camera" == usbDevice.productName) {
                usbCameraNumber++
            }
            if (!isTcTsDev) {
                isTcTsDev = usbDevice.isTcTsDevice() && usbManager.hasPermission(usbDevice)
            }
        }
        return isTcTsDev && usbCameraNumber > 1
    }
    fun isTC001LiteConnect(): Boolean {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbDevice.isTcLiteDevice()) {
                return true
            }
        }
        return false
    }
    fun isHikConnect(): Boolean {
        val usbManager: UsbManager =
            ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        for (usbDevice in usbManager.deviceList.values) {
            if (usbDevice.isHik256()) {
                return true
            }
        }
        return false
    }
    fun requestUsb(
        activity: Activity,
        requestCode: Int,
        device: UsbDevice,
    ) {
        val usbManager = ContextProvider.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val intent = Intent(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(activity, requestCode, intent, flag)
        usbManager.requestPermission(device, pendingIntent)
        XLog.i("[ph][ph]usb[ph][ph]")
    }
}
