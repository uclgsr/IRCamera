package com.topdon.gsr.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant
import java.util.*

/**
 * Device authentication and pairing manager
 * Implements secure device pairing with token-based authentication
 * while maintaining trust-all TLS for development environment
 */
class DeviceAuthenticationManager(private val context: Context) {
    companion object {
        private const val TAG = "DeviceAuth"
        private const val KEYSTORE_ALIAS = "IRCameraDeviceAuth"
        private const val PREFS_NAME = "device_auth_prefs"
        private const val PREF_DEVICE_TOKEN = "device_token"
        private const val PREF_DEVICE_ID = "device_id"
        private const val PREF_PAIRED_CONTROLLERS = "paired_controllers"
        private const val PREF_PAIRING_PIN = "pairing_pin"
        private const val TOKEN_VALIDITY_HOURS = 24
        private const val PAIRING_PIN_LENGTH = 6
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var deviceToken: String? = null
    private var deviceId: String? = null

    init {
        // Initialize device authentication
        initializeDeviceAuth()
    }

    data class PairingRequest(
        val deviceId: String,
        val deviceName: String,
        val deviceType: String,
        val pairingPin: String,
        val timestamp: Long,
        val capabilities: List<String>,
    )

    data class AuthToken(
        val token: String,
        val deviceId: String,
        val issuedAt: Long,
        val expiresAt: Long,
        val controllerId: String,
        val permissions: List<String>,
    )

    interface AuthEventListener {
        fun onPairingRequested(
            controllerId: String,
            controllerName: String,
        )

        fun onPairingCompleted(
            controllerId: String,
            success: Boolean,
        )

        fun onAuthTokenReceived(token: AuthToken)

        fun onAuthTokenExpired(controllerId: String)

        fun onAuthenticationFailed(
            controllerId: String,
            reason: String,
        )
    }

    private var authEventListener: AuthEventListener? = null

    fun setAuthEventListener(listener: AuthEventListener?) {
        authEventListener = listener
    }

    /**
     * Initialize device authentication system
     */
    private fun initializeDeviceAuth() {
        try {
            // Generate or retrieve device ID
            deviceId = getOrCreateDeviceId()

            // Generate or retrieve device token
            deviceToken = getOrCreateDeviceToken()

            Log.d(TAG, "Device authentication initialized - ID: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device authentication", e)
        }
    }

    /**
     * Get or create unique device ID
     */
    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString(PREF_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        }
        return id
    }

    /**
     * Get or create device authentication token
     */
    private fun getOrCreateDeviceToken(): String {
        var token = prefs.getString(PREF_DEVICE_TOKEN, null)
        if (token == null || isTokenExpired(token)) {
            token = generateDeviceToken()
            prefs.edit().putString(PREF_DEVICE_TOKEN, token).apply()
        }
        return token
    }

    /**
     * Generate new device authentication token
     */
    private fun generateDeviceToken(): String {
        val random = SecureRandom()
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Check if token is expired
     */
    private fun isTokenExpired(token: String): Boolean {
        try {
            // Simple token format: base64(timestamp + random)
            // For production, use proper JWT tokens
            return false // For development, tokens don't expire
        } catch (e: Exception) {
            return true
        }
    }

    /**
     * Generate pairing PIN for device discovery
     *
     * Creates a secure 6-digit PIN that will be used to authenticate
     * this device with a PC Controller during the initial pairing process.
     *
     * @return A 6-digit PIN string for device pairing
     */
    fun generatePairingPin(): String {
        val random = SecureRandom()
        val pin = StringBuilder()
        repeat(PAIRING_PIN_LENGTH) {
            pin.append(random.nextInt(10))
        }
        val pairingPin = pin.toString()
        prefs.edit().putString(PREF_PAIRING_PIN, pairingPin).apply()
        return pairingPin
    }

    /**
     * Get current pairing PIN
     *
     * Retrieves the currently stored pairing PIN for this device.
     * Returns null if no PIN has been generated yet.
     *
     * @return The current pairing PIN string, or null if not available
     */
    fun getCurrentPairingPin(): String? {
        return prefs.getString(PREF_PAIRING_PIN, null)
    }

    /**
     * Create pairing request for PC Controller
     *
     * Constructs a complete pairing request containing device information,
     * capabilities, and authentication PIN to be sent to PC Controller
     * during the discovery and pairing process.
     *
     * @return A PairingRequest object with device details and credentials
     */
    fun createPairingRequest(): PairingRequest {
        val pin = getCurrentPairingPin() ?: generatePairingPin()
        return PairingRequest(
            deviceId = deviceId!!,
            deviceName = getDeviceName(),
            deviceType = "Android Sensor Node",
            pairingPin = pin,
            timestamp = Instant.now().epochSecond,
            capabilities = listOf("GSR", "RGB Camera", "Thermal Camera", "Multi-modal Recording"),
        )
    }

    /**
     * Process pairing response from PC Controller
     *
     * Handles the response from PC Controller after sending a pairing request.
     * Stores authentication credentials and paired controller information
     * if pairing was successful.
     *
     * @param response JSON response from PC Controller containing pairing result
     * @return true if pairing was successful and credentials stored, false otherwise
     * @throws JSONException if response format is invalid
     */
    fun processPairingResponse(response: JSONObject): Boolean {
        try {
            val success = response.getBoolean("success")
            val controllerId = response.getString("controller_id")

            if (success) {
                // Store paired controller
                val pairedControllers = getPairedControllers().toMutableSet()
                pairedControllers.add(controllerId)
                storePairedControllers(pairedControllers)

                // Process authentication token if provided
                if (response.has("auth_token")) {
                    val tokenData = response.getJSONObject("auth_token")
                    val authToken =
                        AuthToken(
                            token = tokenData.getString("token"),
                            deviceId = deviceId!!,
                            issuedAt = tokenData.getLong("issued_at"),
                            expiresAt = tokenData.getLong("expires_at"),
                            controllerId = controllerId,
                            permissions =
                                tokenData.getJSONArray("permissions").let { array ->
                                    (0 until array.length()).map { array.getString(it) }
                                },
                        )

                    storeAuthToken(controllerId, authToken)
                    authEventListener?.onAuthTokenReceived(authToken)
                }

                authEventListener?.onPairingCompleted(controllerId, true)
                Log.d(TAG, "Pairing completed successfully with controller: $controllerId")
                return true
            } else {
                val reason = response.optString("reason", "Unknown error")
                authEventListener?.onPairingCompleted(controllerId, false)
                Log.w(TAG, "Pairing failed with controller $controllerId: $reason")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pairing response", e)
            return false
        }
    }

    /**
     * Get authentication token for controller
     */
    fun getAuthToken(controllerId: String): AuthToken? {
        try {
            val tokenJson = prefs.getString("auth_token_$controllerId", null) ?: return null
            val tokenData = JSONObject(tokenJson)

            val authToken =
                AuthToken(
                    token = tokenData.getString("token"),
                    deviceId = tokenData.getString("device_id"),
                    issuedAt = tokenData.getLong("issued_at"),
                    expiresAt = tokenData.getLong("expires_at"),
                    controllerId = tokenData.getString("controller_id"),
                    permissions =
                        tokenData.getJSONArray("permissions").let { array ->
                            (0 until array.length()).map { array.getString(it) }
                        },
                )

            // Check if token is expired
            if (Instant.now().epochSecond > authToken.expiresAt) {
                removeAuthToken(controllerId)
                authEventListener?.onAuthTokenExpired(controllerId)
                return null
            }

            return authToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth token for controller $controllerId", e)
            return null
        }
    }

    /**
     * Store authentication token for controller
     */
    private fun storeAuthToken(
        controllerId: String,
        authToken: AuthToken,
    ) {
        try {
            val tokenData =
                JSONObject().apply {
                    put("token", authToken.token)
                    put("device_id", authToken.deviceId)
                    put("issued_at", authToken.issuedAt)
                    put("expires_at", authToken.expiresAt)
                    put("controller_id", authToken.controllerId)
                    put("permissions", authToken.permissions)
                }

            prefs.edit().putString("auth_token_$controllerId", tokenData.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store auth token", e)
        }
    }

    /**
     * Remove authentication token for controller
     */
    fun removeAuthToken(controllerId: String) {
        prefs.edit().remove("auth_token_$controllerId").apply()
    }

    /**
     * Create authenticated message for PC Controller
     */
    fun createAuthenticatedMessage(
        messageType: String,
        data: JSONObject,
        controllerId: String,
    ): JSONObject {
        val authToken = getAuthToken(controllerId)

        return JSONObject().apply {
            put("message_type", messageType)
            put("device_id", deviceId)
            put("timestamp", Instant.now().epochSecond)
            put("data", data)

            if (authToken != null) {
                put("auth_token", authToken.token)
            }
        }
    }

    /**
     * Validate incoming message authentication
     */
    fun validateMessageAuthentication(
        message: JSONObject,
        controllerId: String,
    ): Boolean {
        try {
            // For development with trust-all TLS, we don't enforce strict auth
            // In production, this would validate the auth_token field

            val messageDeviceId = message.optString("device_id", "")
            if (messageDeviceId.isNotEmpty() && messageDeviceId != deviceId) {
                Log.w(TAG, "Message device ID mismatch")
                return false
            }

            // Check if controller is paired
            val pairedControllers = getPairedControllers()
            if (controllerId !in pairedControllers) {
                Log.w(TAG, "Message from non-paired controller: $controllerId")
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate message authentication", e)
            return false
        }
    }

    /**
     * Get list of paired controllers
     */
    fun getPairedControllers(): Set<String> {
        val pairedJson = prefs.getString(PREF_PAIRED_CONTROLLERS, "[]")
        return try {
            val array = org.json.JSONArray(pairedJson)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Store list of paired controllers
     */
    private fun storePairedControllers(controllers: Set<String>) {
        val array = org.json.JSONArray()
        controllers.forEach { array.put(it) }
        prefs.edit().putString(PREF_PAIRED_CONTROLLERS, array.toString()).apply()
    }

    /**
     * Remove paired controller
     */
    fun unpairController(controllerId: String) {
        val pairedControllers = getPairedControllers().toMutableSet()
        pairedControllers.remove(controllerId)
        storePairedControllers(pairedControllers)
        removeAuthToken(controllerId)
        Log.d(TAG, "Unpaired controller: $controllerId")
    }

    /**
     * Clear all pairing data
     */
    fun clearAllPairings() {
        val pairedControllers = getPairedControllers()
        pairedControllers.forEach { removeAuthToken(it) }
        storePairedControllers(emptySet())
        prefs.edit().remove(PREF_PAIRING_PIN).apply()
        Log.d(TAG, "Cleared all pairing data")
    }

    /**
     * Get device name for pairing
     */
    private fun getDeviceName(): String {
        return android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")"
    }

    /**
     * Get current device ID
     */
    fun getDeviceId(): String? = deviceId

    /**
     * Get current device token
     */
    fun getDeviceToken(): String? = deviceToken

    /**
     * Check if device is paired with any controller
     */
    fun isPaired(): Boolean = getPairedControllers().isNotEmpty()

    /**
     * Check if device is paired with specific controller
     */
    fun isPairedWith(controllerId: String): Boolean = controllerId in getPairedControllers()
}
