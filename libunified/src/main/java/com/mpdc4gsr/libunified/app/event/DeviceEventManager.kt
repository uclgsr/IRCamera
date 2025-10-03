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

    /**
     * Synchronously updates the device connection state.
     *
     * Use this function when you need to emit a device connection event from a non-coroutine context,
     * where calling suspend functions is not possible. Prefer [emitDeviceConnection] in coroutine contexts.
     */
    fun emitDeviceConnectionSync(isConnected: Boolean, device: UsbDevice?) {
        _deviceConnectionState.value = DeviceConnectionState(isConnected, device)
    }

    /**
     * Synchronously updates the socket connection state.
     *
     * Use this function when you need to emit a socket connection event from a non-coroutine context,
     * where calling suspend functions is not possible. Prefer [emitSocketConnection] in coroutine contexts.
     */
    fun emitSocketConnectionSync(isConnected: Boolean, isTS004: Boolean = false) {
        _socketConnectionState.value = SocketConnectionState(isConnected, isTS004)
    }
}
