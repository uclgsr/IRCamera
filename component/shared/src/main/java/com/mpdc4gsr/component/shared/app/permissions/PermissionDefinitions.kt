package com.mpdc4gsr.component.shared.app.permissions

import android.Manifest
import android.os.Build

/**
 * Declarative description of a runtime permission requirement.
 *
 * @property permission Android manifest permission string.
 * @property minSdk First API level where the permission is required.
 * @property maxSdk Optional final API level where the permission applies. Null means no upper bound.
 */
data class PermissionRequirement(
    val permission: String,
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val maxSdk: Int? = null,
) {
    fun isRequiredFor(apiLevel: Int = Build.VERSION.SDK_INT): Boolean {
        if (apiLevel < minSdk) return false
        val upperBound = maxSdk
        return upperBound == null || apiLevel <= upperBound
    }
}

/**
 * Logical group of permissions that should be granted together.
 *
 * @property id Stable identifier used for analytics/UI.
 * @property title Human readable label (e.g. "Bluetooth sensors").
 * @property rationale Short explanation shown to the user.
 * @property requirements Set of individual runtime permissions guarded by SDK constraints.
 */
data class PermissionGroup(
    val id: String,
    val title: String,
    val rationale: String,
    val requirements: List<PermissionRequirement>,
) {
    fun permissionsForDevice(apiLevel: Int = Build.VERSION.SDK_INT): List<String> =
        requirements
            .filter { it.isRequiredFor(apiLevel) }
            .map { it.permission }
}

/**
 * High-level functional areas in the application and the permissions they require.
 * These are consumed both by the welcome flow (Compose) and legacy Activity flows.
 */
enum class FeaturePermissionArea(
    val title: String,
    val description: String,
    val groups: List<PermissionGroup>,
) {
    /**
     * Streaming GSR sensors over Bluetooth Low Energy.
     */
    GSR_SENSORS(
        title = "Bluetooth sensors",
        description = "Required to discover and connect to Shimmer GSR hardware over Bluetooth.",
        groups =
            listOf(
                PermissionCatalog.bluetoothLe,
            ),
    ),

    /**
     * Capturing RGB video streams alongside GSR telemetry.
     */
    RGB_VIDEO(
        title = "RGB video capture",
        description = "Required to capture synchronized RGB video and audio during sessions.",
        groups =
            listOf(
                PermissionCatalog.camera,
                PermissionCatalog.microphone,
                PermissionCatalog.mediaRead,
            ),
    ),

    /**
     * Reviewing or exporting previously captured media without capturing new footage.
     */
    MEDIA_REVIEW(
        title = "Session media access",
        description = "Allows reading captured thermal and RGB media for review or export.",
        groups =
            listOf(
                PermissionCatalog.mediaRead,
            ),
    ),

    /**
     * Capturing thermal/IR imagery alongside GSR telemetry.
     */
    THERMAL_IR(
        title = "Thermal / IR capture",
        description = "Required to control the IR camera, record thermal video, and access captured media.",
        groups =
            listOf(
                PermissionCatalog.camera,
                PermissionCatalog.microphone,
                PermissionCatalog.mediaRead,
                PermissionCatalog.nearbyWifi,
            ),
    ),

    /**
     * Optional push notifications for remote session control or alerts.
     */
    NOTIFICATIONS(
        title = "Session notifications",
        description = "Enables alerts for session faults and remote control messages.",
        groups =
            listOf(
                PermissionCatalog.notifications,
            ),
    ),
    ;

    fun allPermissions(apiLevel: Int = Build.VERSION.SDK_INT): List<String> =
        groups.flatMap { it.permissionsForDevice(apiLevel) }.distinct()
}

/**
 * Canonical library of permission groups referenced throughout the app.
 */
object PermissionCatalog {
    val camera =
        PermissionGroup(
            id = "camera",
            title = "Camera access",
            rationale = "Allows the app to capture RGB and thermal imagery.",
            requirements =
                listOf(
                    PermissionRequirement(Manifest.permission.CAMERA),
                ),
        )

    val microphone =
        PermissionGroup(
            id = "microphone",
            title = "Microphone access",
            rationale = "Needed to record synchronized audio streams.",
            requirements =
                listOf(
                    PermissionRequirement(Manifest.permission.RECORD_AUDIO),
                ),
        )

    val bluetoothLe =
        PermissionGroup(
            id = "bluetooth",
            title = "Bluetooth LE",
            rationale = "Required to scan and connect to nearby GSR sensors.",
            requirements =
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.BLUETOOTH_SCAN,
                        minSdk = Build.VERSION_CODES.S,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.BLUETOOTH_CONNECT,
                        minSdk = Build.VERSION_CODES.S,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.ACCESS_FINE_LOCATION,
                        maxSdk = Build.VERSION_CODES.R,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
                        maxSdk = Build.VERSION_CODES.R,
                    ),
                ),
        )

    val mediaRead =
        PermissionGroup(
            id = "media_read",
            title = "Media library",
            rationale = "Allows the app to read captured photos and videos for review.",
            requirements =
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.READ_MEDIA_IMAGES,
                        minSdk = Build.VERSION_CODES.TIRAMISU,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.READ_MEDIA_VIDEO,
                        minSdk = Build.VERSION_CODES.TIRAMISU,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                        maxSdk = Build.VERSION_CODES.S_V2,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        maxSdk = Build.VERSION_CODES.S_V2,
                    ),
                ),
        )

    val notifications =
        PermissionGroup(
            id = "notifications",
            title = "Notifications",
            rationale = "Allows the app to post session alerts on Android 13+.",
            requirements =
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        minSdk = Build.VERSION_CODES.TIRAMISU,
                    ),
                ),
        )

    val nearbyWifi =
        PermissionGroup(
            id = "nearby_wifi",
            title = "Nearby Wi‑Fi devices",
            rationale = "Required to discover Wi‑Fi based thermal cameras on Android 13+.",
            requirements =
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.NEARBY_WIFI_DEVICES,
                        minSdk = Build.VERSION_CODES.TIRAMISU,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.ACCESS_FINE_LOCATION,
                        maxSdk = Build.VERSION_CODES.S_V2,
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
                        maxSdk = Build.VERSION_CODES.S_V2,
                    ),
                ),
        )
}
