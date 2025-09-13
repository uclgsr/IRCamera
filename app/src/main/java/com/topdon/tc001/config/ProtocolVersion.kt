package com.topdon.tc001.config

import android.util.Log
import org.json.JSONObject

/**
 * Protocol versioning system for PC-to-phone communication
 * Phase 0 implementation - Freeze minimal protocol version v1
 */
object ProtocolVersion {
    private const val TAG = "ProtocolVersion"

    // Protocol version constants
    const val CURRENT_VERSION = "v1"
    const val MIN_SUPPORTED_VERSION = "v1"

    // Protocol capabilities for v1
    private val V1_CAPABILITIES =
        setOf(
            "session_start",
            "session_stop",
            "sync_flash",
            "status_request",
            "heartbeat",
            "time_sync",
            "device_discovery",
            "basic_auth",
        )

    /**
     * Check if a protocol version is supported
     */
    fun isVersionSupported(version: String): Boolean {
        return when (version) {
            "v1" -> true
            else -> false
        }
    }

    /**
     * Get capabilities for a specific protocol version
     */
    fun getCapabilities(version: String): Set<String> {
        return when (version) {
            "v1" -> V1_CAPABILITIES
            else -> emptySet()
        }
    }

    /**
     * Create protocol handshake message
     */
    fun createHandshakeMessage(deviceId: String): JSONObject {
        return JSONObject().apply {
            put("message_type", "protocol_handshake")
            put("protocol_version", CURRENT_VERSION)
            put("min_supported_version", MIN_SUPPORTED_VERSION)
            put("device_id", deviceId)
            put("device_type", "android_sensor_node")
            put("capabilities", V1_CAPABILITIES.joinToString(","))
            put("timestamp", System.currentTimeMillis())
        }
    }

    /**
     * Validate incoming handshake response
     */
    fun validateHandshakeResponse(response: JSONObject): HandshakeResult {
        try {
            val remoteVersion = response.optString("protocol_version")
            val remoteMinVersion = response.optString("min_supported_version", remoteVersion)
            val remoteCapabilities = response.optString("capabilities", "").split(",").filter { it.isNotEmpty() }.toSet()

            if (!isVersionSupported(remoteVersion)) {
                return HandshakeResult(
                    success = false,
                    error = "Unsupported protocol version: $remoteVersion",
                )
            }

            // Check version compatibility
            val isCompatible =
                when {
                    remoteVersion == CURRENT_VERSION -> true
                    remoteMinVersion <= CURRENT_VERSION && remoteVersion >= MIN_SUPPORTED_VERSION -> true
                    else -> false
                }

            if (!isCompatible) {
                return HandshakeResult(
                    success = false,
                    error = "Protocol version incompatible. Remote: $remoteVersion, Local: $CURRENT_VERSION",
                )
            }

            // Find common capabilities
            val localCapabilities = getCapabilities(CURRENT_VERSION)
            val commonCapabilities = localCapabilities.intersect(remoteCapabilities)

            Log.i(TAG, "Protocol handshake successful: version=$remoteVersion, capabilities=${commonCapabilities.size}")

            return HandshakeResult(
                success = true,
                negotiatedVersion = remoteVersion,
                commonCapabilities = commonCapabilities,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error validating handshake response", e)
            return HandshakeResult(
                success = false,
                error = "Invalid handshake response: ${e.message}",
            )
        }
    }

    /**
     * Create protocol message with version header
     */
    fun createProtocolMessage(
        messageType: String,
        content: JSONObject = JSONObject(),
    ): JSONObject {
        return JSONObject().apply {
            put("protocol_version", CURRENT_VERSION)
            put("message_type", messageType)
            put("timestamp", System.currentTimeMillis())

            // Merge content
            content.keys().forEach { key ->
                put(key, content.get(key))
            }
        }
    }

    /**
     * Validate incoming message protocol version
     */
    fun validateMessageVersion(message: JSONObject): Boolean {
        val version = message.optString("protocol_version", CURRENT_VERSION)
        val isValid = isVersionSupported(version)

        if (!isValid) {
            Log.w(TAG, "Received message with unsupported protocol version: $version")
        }

        return isValid
    }

    /**
     * Get protocol information for debugging
     */
    fun getProtocolInfo(): Map<String, Any> {
        return mapOf(
            "current_version" to CURRENT_VERSION,
            "min_supported_version" to MIN_SUPPORTED_VERSION,
            "capabilities" to V1_CAPABILITIES.toList(),
            "capabilities_count" to V1_CAPABILITIES.size,
        )
    }

    /**
     * Result of protocol handshake negotiation
     */
    data class HandshakeResult(
        val success: Boolean,
        val negotiatedVersion: String? = null,
        val commonCapabilities: Set<String> = emptySet(),
        val error: String? = null,
    )
}
