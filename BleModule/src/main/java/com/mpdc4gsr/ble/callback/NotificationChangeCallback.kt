package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface NotificationChangeCallback : RequestFailedCallback {
    fun onNotificationChanged(request: Request?, isEnabled: Boolean)
}
