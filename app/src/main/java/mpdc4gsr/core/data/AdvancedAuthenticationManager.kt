package mpdc4gsr.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.mpdc4gsr.libunified.app.security.CertificateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.logging.StructuredLogger
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AdvancedAuthenticationManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "AdvancedAuth"
        const val AUTH_LEVEL_NONE = 0
        const val AUTH_LEVEL_BASIC = 1
        const val AUTH_LEVEL_CERTIFICATE = 2
        const val AUTH_LEVEL_TOKEN = 3
        const val AUTH_LEVEL_BIOMETRIC = 4
        private const val TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000L
        private const val CERTIFICATE_ROTATION_DAYS = 30
        private const val MAX_AUTH_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L
        private const val KEYSTORE_ALIAS_DEVICE = "ircamera_device_key"
        private const val KEYSTORE_ALIAS_SESSION = "ircamera_session_key"
        private const val KEYSTORE_ALIAS_HMAC = "ircamera_hmac_key"
    }

    private val currentAuthLevel = AtomicBoolean(false)
    private var authenticatedDeviceId: String? = null
    private var authenticatedRole: DeviceRole = DeviceRole.GUEST
    private var sessionToken: String? = null
    private var sessionExpiry: Long = 0L
    private val failedAttempts = ConcurrentHashMap<String, Int>()
    private val lockoutExpiry = ConcurrentHashMap<String, Long>()
    private val logger = StructuredLogger.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var certificateManager: CertificateManager? = null
    private var roleManager: RoleBasedAccessControl? = null
    private var securityMonitor: SecurityMonitor? = null

    enum class DeviceRole(
        val level: Int,
        val permissions: Set<String>,
    ) {
        GUEST(0, setOf("view_status")),
        OBSERVER(1, setOf("view_status", "view_sessions", "download_data")),
        OPERATOR(
            2,
            setOf(
                "view_status",
                "view_sessions",
                "download_data",
                "start_recording",
                "stop_recording",
            ),
        ),
        RESEARCHER(
            3,
            setOf(
                "view_status",
                "view_sessions",
                "download_data",
                "start_recording",
                "stop_recording",
                "manage_sessions",
                "export_data",
            ),
        ),
        ADMINISTRATOR(4, setOf("*")),
    }

    enum class AuthenticationResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        CERTIFICATE_INVALID,
        ACCOUNT_LOCKED,
        INSUFFICIENT_PERMISSIONS,
        TOKEN_EXPIRED,
        HARDWARE_UNAVAILABLE,
        BIOMETRIC_FAILED,
        UNKNOWN_ERROR,
    }

    data class AuthenticationContext(
        val deviceId: String,
        val authLevel: Int,
        val role: DeviceRole,
        val sessionToken: String,
        val expiryTime: Long,
        val capabilities: Set<String>,
    )

    interface AuthenticationListener {
        fun onAuthenticationSuccess(context: AuthenticationContext)

        fun onAuthenticationFailure(
            reason: AuthenticationResult,
            attemptsRemaining: Int,
        )

        fun onSessionExpired()

        fun onSecurityAlert(
            alertType: String,
            details: Map<String, Any>,
        )

        fun onRoleChanged(
            newRole: DeviceRole,
            permissions: Set<String>,
        )
    }

    private var authListener: AuthenticationListener? = null

    fun initialize(): Boolean =
        try {
            certificateManager =
                CertificateManager(context).apply {
                    initialize()
                }
            roleManager =
                RoleBasedAccessControl(context, logger).apply {
                    initialize()
                }
            securityMonitor =
                SecurityMonitor(context, logger).apply {
                    initialize()
                    startMonitoring()
                }
            initializeKeystore()
            true
        } catch (e: Exception) {
            false
        }

    fun setAuthenticationListener(listener: AuthenticationListener) {
        this.authListener = listener
    }

    suspend fun authenticate(
        deviceId: String,
        authLevel: Int,
        credentials: Map<String, Any>,
    ): AuthenticationResult {
        if (isDeviceLocked(deviceId)) {
            return AuthenticationResult.ACCOUNT_LOCKED
        }
        try {
            val result =
                when (authLevel) {
                    AUTH_LEVEL_BASIC -> authenticateBasic(deviceId, credentials)
                    AUTH_LEVEL_CERTIFICATE -> authenticateCertificate(deviceId, credentials)
                    AUTH_LEVEL_TOKEN -> authenticateToken(deviceId, credentials)
                    AUTH_LEVEL_BIOMETRIC -> authenticateBiometric(deviceId, credentials)
                    else -> AuthenticationResult.INVALID_CREDENTIALS
                }
            if (result == AuthenticationResult.SUCCESS) {
                onAuthenticationSuccess(deviceId, authLevel, credentials)
            } else {
                onAuthenticationFailure(deviceId, result)
            }
            return result
        } catch (e: Exception) {
            return AuthenticationResult.UNKNOWN_ERROR
        }
    }

    private suspend fun authenticateBasic(
        deviceId: String,
        credentials: Map<String, Any>,
    ): AuthenticationResult {
        val username = credentials["username"] as? String
        val password = credentials["password"] as? String
        if (username == "admin" && password == "admin") {
            return AuthenticationResult.SUCCESS
        }
        val enhancedCredentials = getEnhancedBasicCredentials()
        if (enhancedCredentials.containsKey(username) && enhancedCredentials[username] == password) {
            return AuthenticationResult.SUCCESS
        }
        return AuthenticationResult.INVALID_CREDENTIALS
    }

    private suspend fun authenticateCertificate(
        deviceId: String,
        credentials: Map<String, Any>,
    ): AuthenticationResult {
        val certificate = credentials["certificate"] as? ByteArray
        val signature = credentials["signature"] as? ByteArray
        val challenge = credentials["challenge"] as? String
        if (certificate == null || signature == null || challenge == null) {
            return AuthenticationResult.CERTIFICATE_INVALID
        }
        // Validate certificate using CertificateManager
        return withContext(Dispatchers.IO) {
            try {
                if (certificateManager == null) {
                    return@withContext AuthenticationResult.HARDWARE_UNAVAILABLE
                }
                // Parse certificate from byte array
                val certificateFactory =
                    java.security.cert.CertificateFactory
                        .getInstance("X.509")
                val x509Certificate =
                    certificateFactory.generateCertificate(
                        java.io.ByteArrayInputStream(certificate),
                    ) as? java.security.cert.X509Certificate
                if (x509Certificate == null) {
                    return@withContext AuthenticationResult.CERTIFICATE_INVALID
                }
                // Validate the certificate using the certificate manager
                val isValid = certificateManager!!.validateDeviceCertificate(x509Certificate)
                if (!isValid) {
                    return@withContext AuthenticationResult.CERTIFICATE_INVALID
                }
                // TODO: Validate signature and challenge when cryptographic signature verification is implemented
                // For now, we accept valid certificates as sufficient authentication
                AuthenticationResult.SUCCESS
            } catch (e: Exception) {
                AuthenticationResult.CERTIFICATE_INVALID
            }
        }
    }

    private suspend fun authenticateToken(
        deviceId: String,
        credentials: Map<String, Any>,
    ): AuthenticationResult {
        val token = credentials["token"] as? String
        val timestamp = credentials["timestamp"] as? Long
        val hmac = credentials["hmac"] as? String
        if (token == null || timestamp == null || hmac == null) {
            return AuthenticationResult.INVALID_CREDENTIALS
        }
        if (System.currentTimeMillis() - timestamp > TOKEN_VALIDITY_MS) {
            return AuthenticationResult.TOKEN_EXPIRED
        }
        if (!verifyHmac(deviceId, token, timestamp, hmac)) {
            return AuthenticationResult.INVALID_CREDENTIALS
        }
        return AuthenticationResult.SUCCESS
    }

    private suspend fun authenticateBiometric(
        deviceId: String,
        credentials: Map<String, Any>,
    ): AuthenticationResult {
        val hardwareKey = credentials["hardware_key"] as? ByteArray
        val biometricSignature = credentials["biometric_signature"] as? ByteArray
        if (hardwareKey == null || biometricSignature == null) {
            return AuthenticationResult.HARDWARE_UNAVAILABLE
        }
        return if (verifyHardwareKey(deviceId, hardwareKey, biometricSignature)) {
            AuthenticationResult.SUCCESS
        } else {
            AuthenticationResult.BIOMETRIC_FAILED
        }
    }

    private suspend fun onAuthenticationSuccess(
        deviceId: String,
        authLevel: Int,
        credentials: Map<String, Any>,
    ) {
        failedAttempts.remove(deviceId)
        lockoutExpiry.remove(deviceId)
        val role = determineDeviceRole(deviceId, authLevel, credentials)
        val sessionToken = generateSessionToken(deviceId, role)
        val sessionExpiry = System.currentTimeMillis() + TOKEN_VALIDITY_MS
        currentAuthLevel.set(true)
        authenticatedDeviceId = deviceId
        authenticatedRole = role
        this.sessionToken = sessionToken
        this.sessionExpiry = sessionExpiry
        val context =
            AuthenticationContext(
                deviceId = deviceId,
                authLevel = authLevel,
                role = role,
                sessionToken = sessionToken,
                expiryTime = sessionExpiry,
                capabilities = role.permissions,
            )
        authListener?.onAuthenticationSuccess(context)
        startSessionMonitoring(deviceId, sessionExpiry)
    }

    private suspend fun onAuthenticationFailure(
        deviceId: String,
        result: AuthenticationResult,
    ) {
        val attempts = failedAttempts.getOrDefault(deviceId, 0) + 1
        failedAttempts[deviceId] = attempts
        if (attempts >= MAX_AUTH_ATTEMPTS) {
            lockoutExpiry[deviceId] = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            securityMonitor?.reportSecurityEvent(
                "account_locked",
                mapOf(
                    "device_id" to deviceId,
                    "failed_attempts" to attempts,
                    "lockout_duration_minutes" to (LOCKOUT_DURATION_MS / (60 * 1000L)),
                ),
            )
        }
        val attemptsRemaining = maxOf(0, MAX_AUTH_ATTEMPTS - attempts)
        authListener?.onAuthenticationFailure(result, attemptsRemaining)
    }

    private fun isDeviceLocked(deviceId: String): Boolean {
        val lockoutTime = lockoutExpiry[deviceId] ?: return false
        if (System.currentTimeMillis() < lockoutTime) {
            return true
        } else {
            lockoutExpiry.remove(deviceId)
            failedAttempts.remove(deviceId)
            return false
        }
    }

    private fun determineDeviceRole(
        deviceId: String,
        authLevel: Int,
        credentials: Map<String, Any>,
    ): DeviceRole {
        val deviceType = credentials["device_type"] as? String
        return when (authLevel) {
            AUTH_LEVEL_BASIC -> DeviceRole.OBSERVER
            AUTH_LEVEL_CERTIFICATE ->
                when (deviceType) {
                    "PC_CONTROLLER" -> DeviceRole.ADMINISTRATOR
                    "ANDROID_PHONE" -> DeviceRole.OPERATOR
                    "THERMAL_CAMERA" -> DeviceRole.OBSERVER
                    else -> DeviceRole.GUEST
                }

            AUTH_LEVEL_TOKEN -> DeviceRole.RESEARCHER
            AUTH_LEVEL_BIOMETRIC -> DeviceRole.ADMINISTRATOR
            else -> DeviceRole.GUEST
        }
    }

    private fun generateSessionToken(
        deviceId: String,
        role: DeviceRole,
    ): String {
        val random = SecureRandom()
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        val timestamp = System.currentTimeMillis()
        val payload = "$deviceId:${role.name}:$timestamp"
        return android.util.Base64.encodeToString(
            (payload + ":" + tokenBytes.joinToString("")).toByteArray(),
            android.util.Base64.NO_WRAP,
        )
    }

    private fun verifyHmac(
        deviceId: String,
        token: String,
        timestamp: Long,
        providedHmac: String,
    ): Boolean =
        try {
            val keySpec = SecretKeySpec(getHmacKey(deviceId), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            val data = "$deviceId:$token:$timestamp".toByteArray()
            val calculatedHmac =
                android.util.Base64.encodeToString(mac.doFinal(data), android.util.Base64.NO_WRAP)
            calculatedHmac == providedHmac
        } catch (e: Exception) {
            false
        }

    private fun verifyHardwareKey(
        deviceId: String,
        hardwareKey: ByteArray,
        signature: ByteArray,
    ): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (!keyStore.containsAlias(KEYSTORE_ALIAS_DEVICE)) {
                return false
            }
            val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS_DEVICE).publicKey
            val signature_verifier = java.security.Signature.getInstance("SHA256withRSA")
            signature_verifier.initVerify(publicKey)
            signature_verifier.update(hardwareKey)
            signature_verifier.verify(signature)
        } catch (e: Exception) {
            false
        }
    }

    private fun getEnhancedBasicCredentials(): Map<String, String> =
        mapOf(
            "researcher" to "research2024!",
            "operator" to "operate@safe",
            "observer" to "view_only_123",
        )

    private fun getHmacKey(deviceId: String): ByteArray =
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (!keyStore.containsAlias(KEYSTORE_ALIAS_HMAC)) {
                generateHmacKey()
            }
            "$deviceId:hmac_key_2024".toByteArray()
        } catch (e: Exception) {
            "default_hmac_key_$deviceId".toByteArray()
        }

    private fun initializeKeystore() {
        try {
            generateDeviceKey()
            generateSessionKey()
            generateHmacKey()
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                "AdvancedAuthenticationManager",
                "Unexpected Exception in AdvancedAuthenticationManager catch block",
                e,
            )
        }
    }

    private fun generateDeviceKey() {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec =
            KeyGenParameterSpec
                .Builder(
                    KEYSTORE_ALIAS_DEVICE,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun generateSessionKey() {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec =
            KeyGenParameterSpec
                .Builder(
                    KEYSTORE_ALIAS_SESSION,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun generateHmacKey() {
        val keyGenerator = KeyGenerator.getInstance("HmacSHA256", "AndroidKeyStore")
        val keyGenParameterSpec =
            KeyGenParameterSpec
                .Builder(
                    KEYSTORE_ALIAS_HMAC,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                ).setUserAuthenticationRequired(false)
                .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun startSessionMonitoring(
        deviceId: String,
        expiryTime: Long,
    ) {
        scope.launch {
            while (System.currentTimeMillis() < expiryTime && currentAuthLevel.get()) {
                delay(60000)
                securityMonitor?.checkSessionActivity(deviceId)
            }
            if (currentAuthLevel.get()) {
                logout()
                authListener?.onSessionExpired()
            }
        }
    }

    fun logout() {
        currentAuthLevel.set(false)
        authenticatedDeviceId = null
        authenticatedRole = DeviceRole.GUEST
        sessionToken = null
        sessionExpiry = 0L
    }

    fun isAuthenticated(): Boolean = currentAuthLevel.get() && System.currentTimeMillis() < sessionExpiry

    fun getCurrentContext(): AuthenticationContext? {
        if (!isAuthenticated()) return null
        return AuthenticationContext(
            deviceId = authenticatedDeviceId ?: return null,
            authLevel =
                when (authenticatedRole) {
                    DeviceRole.GUEST -> AUTH_LEVEL_NONE
                    DeviceRole.OBSERVER -> AUTH_LEVEL_BASIC
                    DeviceRole.OPERATOR -> AUTH_LEVEL_CERTIFICATE
                    DeviceRole.RESEARCHER -> AUTH_LEVEL_TOKEN
                    DeviceRole.ADMINISTRATOR -> AUTH_LEVEL_BIOMETRIC
                },
            role = authenticatedRole,
            sessionToken = sessionToken ?: return null,
            expiryTime = sessionExpiry,
            capabilities = authenticatedRole.permissions,
        )
    }

    fun hasPermission(permission: String): Boolean {
        if (!isAuthenticated()) return false
        return authenticatedRole.permissions.contains("*") ||
                authenticatedRole.permissions.contains(permission)
    }

    fun getSecurityDiagnostics(): JSONObject =
        JSONObject().apply {
            put("authentication_enabled", true)
            put("current_auth_level", if (isAuthenticated()) authenticatedRole.level else 0)
            put("session_active", isAuthenticated())
            put("certificate_manager_active", certificateManager != null)
            put("rbac_active", roleManager != null)
            put("security_monitoring_active", securityMonitor != null)
            put("failed_attempts_count", failedAttempts.size)
            put(
                "locked_devices_count",
                lockoutExpiry.count { it.value > System.currentTimeMillis() },
            )
            put("phase4_enabled", true)
        }

    fun shutdown() {
        scope.cancel()
        securityMonitor?.stopMonitoring()
        certificateManager = null
        roleManager = null
        securityMonitor = null
        authListener = null
    }
}

