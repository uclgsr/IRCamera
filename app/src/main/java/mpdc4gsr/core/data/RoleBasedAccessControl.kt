package mpdc4gsr.core.data

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.core.StructuredLogger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class RoleBasedAccessControl(
    private val context: Context,
    private val logger: StructuredLogger,
) {
    companion object {
        const val TAG = "RBAC"
        const val PERM_VIEW_STATUS = "view_status"
        const val PERM_VIEW_SESSIONS = "view_sessions"
        const val PERM_DOWNLOAD_DATA = "download_data"
        const val PERM_START_RECORDING = "start_recording"
        const val PERM_STOP_RECORDING = "stop_recording"
        const val PERM_MANAGE_SESSIONS = "manage_sessions"
        const val PERM_EXPORT_DATA = "export_data"
        const val PERM_ADMIN_CONFIG = "admin_config"
        const val PERM_USER_MANAGEMENT = "user_management"
        const val PERM_SECURITY_AUDIT = "security_audit"
        const val PERM_SYSTEM_CONTROL = "system_control"
        const val PERM_ALL = "*"
    }

    enum class Role(val level: Int, val displayName: String, val permissions: Set<String>) {
        GUEST(0, "Guest", setOf(PERM_VIEW_STATUS)),
        OBSERVER(
            1,
            "Observer",
            setOf(
                PERM_VIEW_STATUS,
                PERM_VIEW_SESSIONS,
                PERM_DOWNLOAD_DATA,
            ),
        ),
        OPERATOR(
            2,
            "Operator",
            setOf(
                PERM_VIEW_STATUS,
                PERM_VIEW_SESSIONS,
                PERM_DOWNLOAD_DATA,
                PERM_START_RECORDING,
                PERM_STOP_RECORDING,
            ),
        ),
        RESEARCHER(
            3,
            "Researcher",
            setOf(
                PERM_VIEW_STATUS,
                PERM_VIEW_SESSIONS,
                PERM_DOWNLOAD_DATA,
                PERM_START_RECORDING,
                PERM_STOP_RECORDING,
                PERM_MANAGE_SESSIONS,
                PERM_EXPORT_DATA,
            ),
        ),
        ADMINISTRATOR(4, "Administrator", setOf(PERM_ALL)),
        ;

        fun hasPermission(permission: String): Boolean {
            return permissions.contains(PERM_ALL) || permissions.contains(permission)
        }

        fun hasAllPermissions(requiredPermissions: Set<String>): Boolean {
            if (permissions.contains(PERM_ALL)) return true
            return requiredPermissions.all { permissions.contains(it) }
        }
    }

    enum class DeviceType(val roleMappings: Map<String, Role>) {
        PC_CONTROLLER(
            mapOf(
                "admin" to Role.ADMINISTRATOR,
                "researcher" to Role.RESEARCHER,
                "operator" to Role.OPERATOR,
                "observer" to Role.OBSERVER,
                "guest" to Role.GUEST,
            ),
        ),
        ANDROID_PHONE(
            mapOf(
                "owner" to Role.ADMINISTRATOR,
                "user" to Role.OPERATOR,
                "guest" to Role.OBSERVER,
            ),
        ),
        THERMAL_CAMERA(
            mapOf(
                "default" to Role.OBSERVER,
            ),
        ),
        SHIMMER_SENSOR(
            mapOf(
                "default" to Role.OBSERVER,
            ),
        ),
        UNKNOWN(
            mapOf(
                "default" to Role.GUEST,
            ),
        ),
    }

    private val deviceRoles = ConcurrentHashMap<String, Role>()
    private val sessionPermissions = ConcurrentHashMap<String, Set<String>>()
    private val temporaryPermissions =
        ConcurrentHashMap<String, Pair<Set<String>, Long>>()
    private val accessAttempts = mutableListOf<AccessAttempt>()

    data class AccessAttempt(
        val deviceId: String,
        val permission: String,
        val granted: Boolean,
        val timestamp: Long,
        val role: Role,
        val reason: String,
    )

    fun initialize(): Boolean {
        return try {
            AppLogger.i(TAG, "Initializing Role-Based Access Control")
            loadRoleAssignments()
            initializeDefaultMappings()
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "rbac_initialized",
                mapOf(
                    "roles_count" to Role.values().size,
                    "device_types_count" to DeviceType.values().size,
                    "assigned_roles_count" to deviceRoles.size,
                ),
            )
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize RBAC", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "rbac_init_failed",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
            false
        }
    }

    fun assignRole(
        deviceId: String,
        role: Role,
        reason: String = "explicit_assignment",
    ): Boolean {
        return try {
            val previousRole = deviceRoles[deviceId]
            deviceRoles[deviceId] = role
            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "role_assigned",
                mapOf(
                    "device_id" to deviceId,
                    "new_role" to role.name,
                    "previous_role" to (previousRole?.name ?: "none"),
                    "reason" to reason,
                ),
            )
            saveRoleAssignments()
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to assign role to device $deviceId", e)
            false
        }
    }

    fun determineRole(
        deviceId: String,
        deviceType: DeviceType,
        authContext: Map<String, Any>,
    ): Role {
        deviceRoles[deviceId]?.let { return it }
        val authLevel = authContext["auth_level"] as? Int ?: 0
        val userType = authContext["user_type"] as? String ?: "default"
        val mappedRole =
            deviceType.roleMappings[userType] ?: deviceType.roleMappings["default"] ?: Role.GUEST
        val adjustedRole =
            when (authLevel) {
                AdvancedAuthenticationManager.AUTH_LEVEL_NONE -> Role.GUEST
                AdvancedAuthenticationManager.AUTH_LEVEL_BASIC -> minOf(mappedRole, Role.OBSERVER)
                AdvancedAuthenticationManager.AUTH_LEVEL_CERTIFICATE -> minOf(
                    mappedRole,
                    Role.OPERATOR
                )

                AdvancedAuthenticationManager.AUTH_LEVEL_TOKEN -> minOf(mappedRole, Role.RESEARCHER)
                AdvancedAuthenticationManager.AUTH_LEVEL_BIOMETRIC -> mappedRole
                else -> Role.GUEST
            }
        assignRole(deviceId, adjustedRole, "auto_determined")
        return adjustedRole
    }

    fun hasPermission(
        deviceId: String,
        permission: String,
    ): Boolean {
        val role = deviceRoles[deviceId] ?: Role.GUEST
        val hasRolePermission = role.hasPermission(permission)
        val temporaryPerms = temporaryPermissions[deviceId]
        val hasTemporaryPermission =
            temporaryPerms?.let { (permissions, expiry) ->
                System.currentTimeMillis() < expiry && permissions.contains(permission)
            } ?: false
        val hasSessionPermission = sessionPermissions[deviceId]?.contains(permission) ?: false
        val granted = hasRolePermission || hasTemporaryPermission || hasSessionPermission
        logAccessAttempt(deviceId, permission, granted, role)
        return granted
    }

    fun hasAllPermissions(
        deviceId: String,
        requiredPermissions: Set<String>,
    ): Boolean {
        return requiredPermissions.all { hasPermission(deviceId, it) }
    }

    fun grantTemporaryPermissions(
        deviceId: String,
        permissions: Set<String>,
        durationMs: Long = 60 * 60 * 1000L,
    ) {
        val expiryTime = System.currentTimeMillis() + durationMs
        temporaryPermissions[deviceId] = Pair(permissions, expiryTime)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "temporary_permissions_granted",
            mapOf(
                "device_id" to deviceId,
                "permissions" to permissions.joinToString(","),
                "duration_minutes" to (durationMs / (60 * 1000L)),
            ),
        )
    }

    fun grantSessionPermissions(
        deviceId: String,
        permissions: Set<String>,
    ) {
        sessionPermissions[deviceId] = permissions
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "session_permissions_granted",
            mapOf(
                "device_id" to deviceId,
                "permissions" to permissions.joinToString(","),
            ),
        )
    }

    fun revokePermissions(deviceId: String) {
        temporaryPermissions.remove(deviceId)
        sessionPermissions.remove(deviceId)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            TAG,
            "permissions_revoked",
            mapOf(
                "device_id" to deviceId,
            ),
        )
    }

    fun getEffectivePermissions(deviceId: String): Set<String> {
        val role = deviceRoles[deviceId] ?: Role.GUEST
        val rolePermissions =
            if (role.permissions.contains(PERM_ALL)) {
                getAllPermissions()
            } else {
                role.permissions
            }
        val temporaryPerms =
            temporaryPermissions[deviceId]?.let { (permissions, expiry) ->
                if (System.currentTimeMillis() < expiry) permissions else emptySet()
            } ?: emptySet()
        val sessionPerms = sessionPermissions[deviceId] ?: emptySet()
        return rolePermissions + temporaryPerms + sessionPerms
    }

    private fun getAllPermissions(): Set<String> {
        return setOf(
            PERM_VIEW_STATUS,
            PERM_VIEW_SESSIONS,
            PERM_DOWNLOAD_DATA,
            PERM_START_RECORDING,
            PERM_STOP_RECORDING,
            PERM_MANAGE_SESSIONS,
            PERM_EXPORT_DATA,
            PERM_ADMIN_CONFIG,
            PERM_USER_MANAGEMENT,
            PERM_SECURITY_AUDIT,
            PERM_SYSTEM_CONTROL,
        )
    }

    private fun logAccessAttempt(
        deviceId: String,
        permission: String,
        granted: Boolean,
        role: Role,
    ) {
        val attempt =
            AccessAttempt(
                deviceId = deviceId,
                permission = permission,
                granted = granted,
                timestamp = System.currentTimeMillis(),
                role = role,
                reason = if (granted) "permission_granted" else "permission_denied",
            )
        synchronized(accessAttempts) {
            accessAttempts.add(attempt)
            if (accessAttempts.size > 1000) {
                accessAttempts.removeAt(0)
            }
        }
        if (!granted) {
            logger.log(
                StructuredLogger.LogLevel.WARNING,
                TAG,
                "access_denied",
                mapOf(
                    "device_id" to deviceId,
                    "permission" to permission,
                    "role" to role.name,
                ),
            )
        }
    }

    fun getAccessAuditTrail(
        deviceId: String? = null,
        limit: Int = 100,
    ): List<AccessAttempt> {
        synchronized(accessAttempts) {
            return accessAttempts
                .let { if (deviceId != null) it.filter { attempt -> attempt.deviceId == deviceId } else it }
                .takeLast(limit)
        }
    }

    fun getSecurityViolations(timeWindowMs: Long = 24 * 60 * 60 * 1000L): List<AccessAttempt> {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        synchronized(accessAttempts) {
            return accessAttempts
                .filter { !it.granted && it.timestamp > cutoffTime }
        }
    }

    private fun cleanupExpiredPermissions() {
        val currentTime = System.currentTimeMillis()
        val expiredDevices =
            temporaryPermissions.filterValues { (_, expiry) ->
                currentTime >= expiry
            }.keys
        expiredDevices.forEach { deviceId ->
            temporaryPermissions.remove(deviceId)
        }
        if (expiredDevices.isNotEmpty()) {
            logger.log(
                StructuredLogger.LogLevel.DEBUG,
                TAG,
                "expired_permissions_cleaned",
                mapOf(
                    "cleaned_devices_count" to expiredDevices.size,
                ),
            )
        }
    }

    private fun loadRoleAssignments() {
        AppLogger.i(TAG, "Role assignments loaded (placeholder implementation)")
    }

    private fun saveRoleAssignments() {
        AppLogger.d(TAG, "Role assignments saved (placeholder implementation)")
    }

    private fun initializeDefaultMappings() {
        AppLogger.i(TAG, "Default device type mappings initialized")
    }

    fun getRole(deviceId: String): Role {
        return deviceRoles[deviceId] ?: Role.GUEST
    }

    fun getAllDeviceRoles(): Map<String, Role> {
        return deviceRoles.toMap()
    }

    fun getDiagnostics(): JSONObject {
        cleanupExpiredPermissions()
        return JSONObject().apply {
            put("assigned_roles_count", deviceRoles.size)
            put("temporary_permissions_count", temporaryPermissions.size)
            put("session_permissions_count", sessionPermissions.size)
            put("total_access_attempts", accessAttempts.size)
            put("recent_violations", getSecurityViolations(60 * 60 * 1000L).size)
            put("available_roles", Role.values().map { it.name })
            put("available_permissions", getAllPermissions().sorted())
        }
    }

    inline fun <T> withPermission(
        deviceId: String,
        permission: String,
        action: () -> T,
    ): T? {
        return if (hasPermission(deviceId, permission)) {
            action()
        } else {
            AppLogger.w(TAG, "Permission denied for device $deviceId: $permission")
            null
        }
    }

    inline fun <T> withPermissions(
        deviceId: String,
        permissions: Set<String>,
        action: () -> T,
    ): T? {
        return if (hasAllPermissions(deviceId, permissions)) {
            action()
        } else {
            Log.w(
                TAG,
                "Insufficient permissions for device $deviceId: ${permissions.joinToString(",")}"
            )
            null
        }
    }
}
