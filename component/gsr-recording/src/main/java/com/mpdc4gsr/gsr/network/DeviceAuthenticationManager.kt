package com.mpdc4gsr.gsr.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant
import java.util.*

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

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var deviceToken: String? = null
    private var deviceId: String? = null

    init {
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

    private fun initializeDeviceAuth() {
        try {
            deviceId = getOrCreateDeviceId()
            deviceToken = getOrCreateDeviceToken()
            Log.d(TAG, "Device authentication initialized - ID: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device authentication", e)
        }
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString(PREF_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        }
        return id
    }

    private fun getOrCreateDeviceToken(): String {
        var token = prefs.getString(PREF_DEVICE_TOKEN, null)
        if (token == null || isTokenExpired(token)) {
            token = generateDeviceToken()
            prefs.edit().putString(PREF_DEVICE_TOKEN, token).apply()
        }
        return token
    }

    private fun generateDeviceToken(): String {
        val random = SecureRandom()
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun isTokenExpired(token: String): Boolean {
        try {
            return false
        } catch (e: Exception) {
            return true
        }
    }

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

    fun getCurrentPairingPin(): String? {
        return prefs.getString(PREF_PAIRING_PIN, null)
    }

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

    fun processPairingResponse(response: JSONObject): Boolean {
        try {
            val success = response.getBoolean("success")
            val controllerId = response.getString("controller_id")
            if (success) {
                val pairedControllers = getPairedControllers().toMutableSet()
                pairedControllers.add(controllerId)
                storePairedControllers(pairedControllers)
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

    fun removeAuthToken(controllerId: String) {
        prefs.edit().remove("auth_token_$controllerId").apply()
    }

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

    fun validateMessageAuthentication(
        message: JSONObject,
        controllerId: String,
    ): Boolean {
        try {
            val messageDeviceId = message.optString("device_id", "")
            if (messageDeviceId.isNotEmpty() && messageDeviceId != deviceId) {
                Log.w(TAG, "Message device ID mismatch")
                return false
            }
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

    fun getPairedControllers(): Set<String> {
        val pairedJson = prefs.getString(PREF_PAIRED_CONTROLLERS, "[]")
        return try {
            val array = org.json.JSONArray(pairedJson)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun storePairedControllers(controllers: Set<String>) {
        val array = org.json.JSONArray()
        controllers.forEach { array.put(it) }
        prefs.edit().putString(PREF_PAIRED_CONTROLLERS, array.toString()).apply()
    }

    fun unpairController(controllerId: String) {
        val pairedControllers = getPairedControllers().toMutableSet()
        pairedControllers.remove(controllerId)
        storePairedControllers(pairedControllers)
        removeAuthToken(controllerId)
        Log.d(TAG, "Unpaired controller: $controllerId")
    }

    fun clearAllPairings() {
        val pairedControllers = getPairedControllers()
        pairedControllers.forEach { removeAuthToken(it) }
        storePairedControllers(emptySet())
        prefs.edit().remove(PREF_PAIRING_PIN).apply()
        Log.d(TAG, "Cleared all pairing data")
    }

    private fun getDeviceName(): String {
        return android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")"
    }

    fun getDeviceId(): String? = deviceId
    fun getDeviceToken(): String? = deviceToken
    fun isPaired(): Boolean = getPairedControllers().isNotEmpty()
    fun isPairedWith(controllerId: String): Boolean = controllerId in getPairedControllers()
}
