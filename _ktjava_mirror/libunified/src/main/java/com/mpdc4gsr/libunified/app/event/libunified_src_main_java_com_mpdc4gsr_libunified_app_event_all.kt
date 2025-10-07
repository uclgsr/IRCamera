// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\event' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\event\DeviceEventManager.kt =====

package com.mpdc4gsr.libunified.app.event

import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object DeviceEventManager {
    data class DeviceConnectionState(
        val isConnected: Boolean,
        val device: UsbDevice?
    )

    data class SocketConnectionState(
        val isConnected: Boolean,
        val isTS004: Boolean = false
    )

    private val _deviceConnectionState = MutableStateFlow<DeviceConnectionState?>(null)
    val deviceConnectionState: StateFlow<DeviceConnectionState?> = _deviceConnectionState.asStateFlow()
    private val _socketConnectionState = MutableStateFlow<SocketConnectionState?>(null)
    val socketConnectionState: StateFlow<SocketConnectionState?> = _socketConnectionState.asStateFlow()
    private val _devicePermissionRequested = MutableSharedFlow<UsbDevice>()
    val devicePermissionRequested: SharedFlow<UsbDevice> = _devicePermissionRequested.asSharedFlow()
    suspend fun emitDeviceConnection(isConnected: Boolean, device: UsbDevice?) {
        _deviceConnectionState.emit(DeviceConnectionState(isConnected, device))
    }

    suspend fun emitSocketConnection(isConnected: Boolean, isTS004: Boolean = false) {
        _socketConnectionState.emit(SocketConnectionState(isConnected, isTS004))
    }

    suspend fun emitDevicePermissionRequest(device: UsbDevice) {
        _devicePermissionRequested.emit(device)
    }

    fun emitDeviceConnectionSync(isConnected: Boolean, device: UsbDevice?) {
        _deviceConnectionState.value = DeviceConnectionState(isConnected, device)
    }

    fun emitSocketConnectionSync(isConnected: Boolean, isTS004: Boolean = false) {
        _socketConnectionState.value = SocketConnectionState(isConnected, isTS004)
    }

    fun emitDevicePermissionRequestSync(device: UsbDevice): Boolean {
        return _devicePermissionRequested.tryEmit(device)
    }
}


