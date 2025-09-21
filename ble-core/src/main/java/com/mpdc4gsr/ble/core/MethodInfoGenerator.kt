package com.mpdc4gsr.ble.core

import java.lang.reflect.Method

/**
 * Utility class for generating method information for BLE operations
 */
object MethodInfoGenerator {

    fun generateInfo(method: Method, vararg params: Any?): String {
        val paramTypes = method.parameterTypes.joinToString(", ") { it.simpleName }
        val paramValues = params.joinToString(", ") { it?.toString() ?: "null" }
        return "${method.declaringClass.simpleName}.${method.name}($paramTypes) with values: [$paramValues]"
    }

    fun generateInfo(className: String, methodName: String, vararg params: Any?): String {
        val paramValues = params.joinToString(", ") { it?.toString() ?: "null" }
        return "$className.$methodName() with values: [$paramValues]"
    }

    fun generateRequestInfo(requestType: String, params: Map<String, Any?>): String {
        val paramStr = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        return "BLE Request: $requestType [$paramStr]"
    }

    /**
     * Generate method info for connect failed events
     */
    fun onConnectFailed(device: Device, reason: Int): com.mpdc4gsr.commons.poster.MethodInfo {
        return com.mpdc4gsr.commons.poster.MethodInfo(
            "onConnectFailed",
            com.mpdc4gsr.commons.poster.MethodInfo.Parameter(Device::class.java, device),
            com.mpdc4gsr.commons.poster.MethodInfo.Parameter(Int::class.java, reason)
        )
    }

    /**
     * Generate method info for bluetooth adapter state changed events
     */
    fun onBluetoothAdapterStateChanged(state: Int): com.mpdc4gsr.commons.poster.MethodInfo {
        return com.mpdc4gsr.commons.poster.MethodInfo(
            "onBluetoothAdapterStateChanged",
            com.mpdc4gsr.commons.poster.MethodInfo.Parameter(Int::class.java, state)
        )
    }

    /**
     * Generate method info for bluetooth off events
     */
    fun onBluetoothOff(): com.mpdc4gsr.commons.poster.MethodInfo {
        return com.mpdc4gsr.commons.poster.MethodInfo("onBluetoothOff")
    }
}