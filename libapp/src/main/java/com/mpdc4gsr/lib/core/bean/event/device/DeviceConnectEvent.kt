package com.mpdc4gsr.lib.core.bean.event.device

import android.hardware.usb.UsbDevice

data class DeviceConnectEvent(val isConnect: Boolean, val device: UsbDevice?)
