// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\core\data\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\core\data\model\app_src_main_java_mpdc4gsr_core_data_model_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\core\data\model' subtree
// Files: 5; Generated 2025-10-07 23:07:38


// ===== app\src\main\java\mpdc4gsr\core\data\model\DeviceInfo.kt =====

package mpdc4gsr.core.data.model

data class DeviceInfo(
    val address: String,
    val name: String,
    val deviceType: String,
    val rssi: Int,
    val isGSRCapable: Boolean,
    val priority: Int = 2,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null
) {
    val isGSRPlusDevice: Boolean
        get() = name.contains("GSR", ignoreCase = true) ||
                deviceType.contains("GSR", ignoreCase = true)
    val hasStrongSignal: Boolean
        get() = rssi >= -60
    val hasWeakSignal: Boolean
        get() = rssi <= -80
    val signalStrength: SignalStrength
        get() = when {
            rssi >= -50 -> SignalStrength.EXCELLENT
            rssi >= -60 -> SignalStrength.GOOD
            rssi >= -70 -> SignalStrength.FAIR
            rssi >= -80 -> SignalStrength.POOR
            else -> SignalStrength.VERY_POOR
        }
    val isRecommended: Boolean
        get() = isGSRCapable &&
                hasStrongSignal &&
                (batteryLevel == null || batteryLevel > 20)
    val displayName: String
        get() = when {
            isGSRPlusDevice -> "$name (GSR+)"
            isGSRCapable -> "$name (GSR)"
            else -> name
        }
    val statusSummary: String
        get() = buildString {
            append(signalStrength.displayName)
            batteryLevel?.let { append(" â€¢ $it% battery") }
            if (isRecommended) append(" â€¢ Recommended")
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "address" to address,
            "name" to name,
            "device_type" to deviceType,
            "rssi" to rssi,
            "is_gsr_capable" to isGSRCapable,
            "is_gsr_plus" to isGSRPlusDevice,
            "priority" to priority,
            "battery_level" to batteryLevel,
            "firmware_version" to firmwareVersion,
            "signal_strength" to signalStrength.name,
            "is_recommended" to isRecommended,
            "display_name" to displayName,
            "status_summary" to statusSummary
        )
    }

    enum class SignalStrength(val displayName: String) {
        EXCELLENT("Excellent"),
        GOOD("Good"),
        FAIR("Fair"),
        POOR("Poor"),
        VERY_POOR("Very Poor")
    }

    companion object {
        val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72")
        fun isShimmerDevice(address: String): Boolean {
            return SHIMMER_MAC_PREFIXES.any {
                address.startsWith(it, ignoreCase = true)
            }
        }

        fun fromBluetoothDevice(
            address: String,
            name: String?,
            rssi: Int
        ): DeviceInfo? {
            if (!isShimmerDevice(address)) {
                return null
            }
            val deviceName = name ?: "Shimmer Device"
            val isGSRDevice = deviceName.contains("GSR", ignoreCase = true)
            val deviceType = when {
                isGSRDevice -> "GSR+"
                deviceName.contains("Shimmer3", ignoreCase = true) -> "Shimmer3"
                deviceName.contains("Shimmer", ignoreCase = true) -> "Shimmer"
                else -> "Unknown"
            }
            val priority = when {
                isGSRDevice -> 1
                deviceType.startsWith("Shimmer3") -> 2
                else -> 3
            }
            return DeviceInfo(
                address = address,
                name = deviceName,
                deviceType = deviceType,
                rssi = rssi,
                isGSRCapable = true,
                priority = priority
            )
        }

        fun sortByPriority(devices: List<DeviceInfo>): List<DeviceInfo> {
            return devices.sortedWith(
                compareBy<DeviceInfo> { it.priority }
                    .thenByDescending { it.rssi }
                    .thenBy { it.name }
            )
        }

        fun getRecommendedDevices(devices: List<DeviceInfo>): List<DeviceInfo> {
            return devices
                .filter { it.isRecommended }
                .let { sortByPriority(it) }
        }

        fun createMockDevice(
            deviceId: String = "test_device",
            isGSR: Boolean = true,
            signalStrength: SignalStrength = SignalStrength.GOOD
        ): DeviceInfo {
            val rssi = when (signalStrength) {
                SignalStrength.EXCELLENT -> -45
                SignalStrength.GOOD -> -55
                SignalStrength.FAIR -> -65
                SignalStrength.POOR -> -75
                SignalStrength.VERY_POOR -> -85
            }
            return DeviceInfo(
                address = "00:06:66:${
                    String.format(
                        "%02X:%02X:%02X",
                        deviceId.hashCode() and 0xFF,
                        (deviceId.hashCode() shr 8) and 0xFF,
                        (deviceId.hashCode() shr 16) and 0xFF
                    )
                }",
                name = if (isGSR) "Shimmer3-GSR+ $deviceId" else "Shimmer3 $deviceId",
                deviceType = if (isGSR) "GSR+" else "Shimmer3",
                rssi = rssi,
                isGSRCapable = true,
                priority = if (isGSR) 1 else 2,
                batteryLevel = (50..90).random(),
                firmwareVersion = "BtStream 0.7.0"
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\data\model\GSRSample.kt =====

package mpdc4gsr.core.data .model

data class GSRSample(
    val timestamp: Long,
    val timestampIso: String,
    val gsrMicrosiemens: Double,
    val gsrRaw: Int,
    val ppgRaw: Int = 0,
    val qualityScore: Double,
    val connectionRssi: Int
) {
    val isValid: Boolean
        get() = gsrRaw in 0..4095 &&
                gsrMicrosiemens > 0.0 &&
                qualityScore >= 0.5
    val resistanceOhms: Double
        get() = if (gsrMicrosiemens > 0) 1_000_000.0 / gsrMicrosiemens else Double.MAX_VALUE
    val qualityLevel: QualityLevel
        get() = when {
            qualityScore >= 0.9 -> QualityLevel.EXCELLENT
            qualityScore >= 0.7 -> QualityLevel.GOOD
            qualityScore >= 0.5 -> QualityLevel.FAIR
            else -> QualityLevel.POOR
        }

    fun toCsvRow(): String {
        return "$timestamp,$timestampIso,$gsrMicrosiemens,$gsrRaw,$ppgRaw,$qualityScore,$connectionRssi"
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "timestamp" to timestamp,
            "timestamp_iso" to timestampIso,
            "gsr_microsiemens" to gsrMicrosiemens,
            "gsr_raw" to gsrRaw,
            "ppg_raw" to ppgRaw,
            "quality_score" to qualityScore,
            "connection_rssi" to connectionRssi,
            "resistance_ohms" to resistanceOhms,
            "is_valid" to isValid,
            "quality_level" to qualityLevel.name
        )
    }

    enum class QualityLevel {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }

    companion object {
        const val CSV_HEADER =
            "timestamp_ns,timestamp_iso,gsr_microsiemens,gsr_raw,ppg_raw,quality_score,connection_rssi"

        fun fromRawData(
            timestamp: Long,
            timestampIso: String,
            gsrCalibratedValue: Double,
            gsrRawValue: Int,
            ppgRawValue: Int = 0,
            connectionRssi: Int = -50
        ): GSRSample {
            val qualityScore = when {
                gsrRawValue < 0 || gsrRawValue > 4095 -> 0.0
                gsrCalibratedValue <= 0 -> 0.3
                gsrRawValue < 50 || gsrRawValue > 4000 -> 0.6
                else -> 0.9
            }
            return GSRSample(
                timestamp = timestamp,
                timestampIso = timestampIso,
                gsrMicrosiemens = gsrCalibratedValue,
                gsrRaw = gsrRawValue,
                ppgRaw = ppgRawValue,
                qualityScore = qualityScore,
                connectionRssi = connectionRssi
            )
        }

        fun fromCsvRow(csvRow: String): GSRSample? {
            return try {
                val parts = csvRow.split(",")
                if (parts.size >= 7) {
                    GSRSample(
                        timestamp = parts[0].toLong(),
                        timestampIso = parts[1],
                        gsrMicrosiemens = parts[2].toDouble(),
                        gsrRaw = parts[3].toInt(),
                        ppgRaw = parts[4].toInt(),
                        qualityScore = parts[5].toDouble(),
                        connectionRssi = parts[6].toInt()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\data\model\NetworkStatus.kt =====

package mpdc4gsr.core.data .model

enum class NetworkStatus(
    val displayName: String,
    val isConnected: Boolean,
    val canDiscover: Boolean
) {
    DISCONNECTED("Disconnected", false, false),
    NO_WIFI("No Wi-Fi", false, false),
    CONNECTED_TO_WIFI("Connected to Wi-Fi", true, true),
    PERMISSION_DENIED("Permission Denied", false, false),
    DISCOVERING("Discovering Controllers", true, true),
    READY("Ready", true, false),
    NO_CONTROLLERS_FOUND("No Controllers Found", true, false),
    CONNECTING("Connecting", true, false),
    CONNECTED("Connected to PC", true, false),
    CONNECTION_FAILED("Connection Failed", true, true),
    NETWORK_LOST("Network Lost", false, false),
    ERROR("Network Error", false, false);

    val isNetworkAvailable: Boolean
        get() = this != DISCONNECTED && this != NO_WIFI && this != NETWORK_LOST && this != PERMISSION_DENIED
    val isError: Boolean
        get() = this == ERROR || this == CONNECTION_FAILED || this == PERMISSION_DENIED
    val isConnecting: Boolean
        get() = this == DISCOVERING || this == CONNECTING
    val canConnect: Boolean
        get() = this == READY || this == NO_CONTROLLERS_FOUND || this == CONNECTION_FAILED
    val statusColor: StatusColor
        get() = when (this) {
            CONNECTED -> StatusColor.GREEN
            CONNECTED_TO_WIFI, READY -> StatusColor.BLUE
            DISCOVERING, CONNECTING -> StatusColor.YELLOW
            CONNECTION_FAILED, NO_CONTROLLERS_FOUND -> StatusColor.ORANGE
            DISCONNECTED, NO_WIFI, NETWORK_LOST, ERROR, PERMISSION_DENIED -> StatusColor.RED
        }
    val description: String
        get() = when (this) {
            DISCONNECTED -> "No network connection available"
            NO_WIFI -> "Wi-Fi connection required for PC communication"
            CONNECTED_TO_WIFI -> "Connected to Wi-Fi network"
            PERMISSION_DENIED -> "Network permissions required"
            DISCOVERING -> "Scanning for PC controllers on local network"
            READY -> "Ready to connect to PC controllers"
            NO_CONTROLLERS_FOUND -> "No PC controllers found on network"
            CONNECTING -> "Establishing connection to PC controller"
            CONNECTED -> "Connected and communicating with PC controller"
            CONNECTION_FAILED -> "Unable to connect to PC controller"
            NETWORK_LOST -> "Wi-Fi connection lost"
            ERROR -> "Network error occurred"
        }
    val recommendedAction: String?
        get() = when (this) {
            DISCONNECTED, NO_WIFI -> "Connect to Wi-Fi network"
            PERMISSION_DENIED -> "Grant network permissions in settings"
            NO_CONTROLLERS_FOUND -> "Ensure PC controller is running and on same network"
            CONNECTION_FAILED -> "Check PC controller address and try again"
            NETWORK_LOST -> "Reconnect to Wi-Fi network"
            ERROR -> "Check network settings and try again"
            else -> null
        }

    enum class StatusColor {
        GREEN,
        BLUE,
        YELLOW,
        ORANGE,
        RED
    }

    companion object {
        fun getConnectedStates(): List<NetworkStatus> {
            return values().filter { it.isConnected }
        }

        fun getErrorStates(): List<NetworkStatus> {
            return values().filter { it.isError }
        }

        fun getDiscoveryStates(): List<NetworkStatus> {
            return values().filter { it.canDiscover }
        }

        fun fromConnectionState(
            hasWifi: Boolean,
            hasInternet: Boolean,
            isDiscovering: Boolean,
            connectedControllers: Int
        ): NetworkStatus {
            return when {
                !hasWifi -> NO_WIFI
                !hasInternet -> CONNECTED_TO_WIFI
                connectedControllers > 0 -> CONNECTED
                isDiscovering -> DISCOVERING
                else -> READY
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\data\model\PCControllerInfo.kt =====

package mpdc4gsr.core.data .model

data class PCControllerInfo(
    val name: String,
    val host: String,
    val port: Int,
    val type: String,
    val properties: Map<String, String> = emptyMap(),
    val capabilities: List<String> = emptyList(),
    val protocolVersion: String = "1.0",
    val lastSeen: Long = System.currentTimeMillis()
) {
    val address: String
        get() = "$host:$port"
    val supportsGSR: Boolean
        get() = capabilities.contains("gsr") ||
                properties["supports_gsr"] == "true" ||
                properties.containsKey("shimmer_support")
    val supportsThermal: Boolean
        get() = capabilities.contains("thermal") ||
                properties["supports_thermal"] == "true"
    val supportsRGB: Boolean
        get() = capabilities.contains("rgb") ||
                properties["supports_rgb"] == "true"
    val supportsSecure: Boolean
        get() = capabilities.contains("tls") ||
                properties["secure"] == "true" ||
                properties["tls"] == "true"
    val isRecentlyActive: Boolean
        get() = System.currentTimeMillis() - lastSeen < 60000
    val softwareVersion: String?
        get() = properties["version"] ?: properties["software_version"]
    val platform: String?
        get() = properties["platform"] ?: properties["os"]
    val displayName: String
        get() = properties["display_name"] ?: name
    val statusSummary: String
        get() = buildString {
            append(address)
            softwareVersion?.let { append(" â€¢ v$it") }
            platform?.let { append(" â€¢ $it") }
            val supportedFeatures = mutableListOf<String>()
            if (supportsGSR) supportedFeatures.add("GSR")
            if (supportsThermal) supportedFeatures.add("Thermal")
            if (supportsRGB) supportedFeatures.add("RGB")
            if (supportedFeatures.isNotEmpty()) {
                append(" â€¢ ${supportedFeatures.joinToString("/")}")
            }
        }
    val connectionPriority: Int
        get() {
            var priority = 0
            if (isRecentlyActive) priority += 100
            if (supportsGSR) priority += 20
            if (supportsThermal) priority += 15
            if (supportsRGB) priority += 10
            if (supportsSecure) priority += 5
            return priority
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "host" to host,
            "port" to port,
            "type" to type,
            "address" to address,
            "capabilities" to capabilities,
            "properties" to properties,
            "protocol_version" to protocolVersion,
            "last_seen" to lastSeen,
            "supports_gsr" to supportsGSR,
            "supports_thermal" to supportsThermal,
            "supports_rgb" to supportsRGB,
            "supports_secure" to supportsSecure,
            "is_recently_active" to isRecentlyActive,
            "software_version" to softwareVersion,
            "platform" to platform,
            "display_name" to displayName,
            "status_summary" to statusSummary,
            "connection_priority" to connectionPriority
        )
    }

    fun getWebSocketUrl(secure: Boolean = false): String {
        val protocol = if (secure && supportsSecure) "wss" else "ws"
        return "$protocol://$host:$port"
    }

    fun isCompatibleWith(requiredFeatures: List<String>): Boolean {
        return requiredFeatures.all { feature ->
            when (feature.lowercase()) {
                "gsr" -> supportsGSR
                "thermal" -> supportsThermal
                "rgb" -> supportsRGB
                "secure", "tls" -> supportsSecure
                else -> capabilities.contains(feature) || properties.containsKey(feature)
            }
        }
    }

    companion object {
        fun fromServiceInfo(
            serviceName: String,
            hostAddress: String,
            port: Int,
            serviceType: String,
            txtRecord: Map<String, String>
        ): PCControllerInfo {
            val capabilities =
                txtRecord["capabilities"]?.split(",")?.map { it.trim() } ?: emptyList()
            val protocolVersion = txtRecord["protocol_version"] ?: txtRecord["version"] ?: "1.0"
            return PCControllerInfo(
                name = serviceName,
                host = hostAddress,
                port = port,
                type = serviceType,
                properties = txtRecord,
                capabilities = capabilities,
                protocolVersion = protocolVersion
            )
        }

        fun createMockController(
            controllerId: String = "test_pc",
            includeGSR: Boolean = true,
            includeThermal: Boolean = true,
            includeRGB: Boolean = true
        ): PCControllerInfo {
            val capabilities = mutableListOf<String>()
            if (includeGSR) capabilities.add("gsr")
            if (includeThermal) capabilities.add("thermal")
            if (includeRGB) capabilities.add("rgb")
            capabilities.add("tls")
            val properties = mapOf(
                "version" to "2.1.0",
                "platform" to "Windows 11",
                "display_name" to "IRCamera PC Controller $controllerId",
                "supports_gsr" to includeGSR.toString(),
                "supports_thermal" to includeThermal.toString(),
                "supports_rgb" to includeRGB.toString(),
                "secure" to "true",
                "shimmer_support" to "true"
            )
            return PCControllerInfo(
                name = "ircamera-pc-$controllerId",
                host = "192.168.1.${100 + controllerId.hashCode() % 50}",
                port = 8888,
                type = "_ircamera._tcp.local.",
                properties = properties,
                capabilities = capabilities,
                protocolVersion = "2.0"
            )
        }

        fun sortByPriority(controllers: List<PCControllerInfo>): List<PCControllerInfo> {
            return controllers.sortedByDescending { it.connectionPriority }
        }

        fun filterByFeatures(
            controllers: List<PCControllerInfo>,
            requiredFeatures: List<String>
        ): List<PCControllerInfo> {
            return controllers.filter { it.isCompatibleWith(requiredFeatures) }
        }

        fun getGSRCapableControllers(controllers: List<PCControllerInfo>): List<PCControllerInfo> {
            return controllers.filter { it.supportsGSR }
        }

        fun getActiveControllers(controllers: List<PCControllerInfo>): List<PCControllerInfo> {
            return controllers.filter { it.isRecentlyActive }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\core\data\model\SessionModels.kt =====

package mpdc4gsr.core.data .model

import org.json.JSONObject

data class SessionConfig(
    val sessionName: String,
    val studyName: String,
    val participantId: String,
    val enabledSensors: List<String>,
    val sessionType: SessionType = SessionType.LOCAL,
    val maxDuration: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun fromJson(json: JSONObject): SessionConfig {
            val enabledSensors = mutableListOf<String>()
            val sensorsArray = json.optJSONArray("enabled_sensors")
            if (sensorsArray != null) {
                for (i in 0 until sensorsArray.length()) {
                    enabledSensors.add(sensorsArray.getString(i))
                }
            }
            val metadata = mutableMapOf<String, Any>()
            val metadataObj = json.optJSONObject("metadata")
            metadataObj?.keys()?.forEach { key ->
                metadata[key] = metadataObj.get(key)
            }
            return SessionConfig(
                sessionName = json.getString("session_name"),
                studyName = json.optString("study_name", ""),
                participantId = json.getString("participant_id"),
                enabledSensors = enabledSensors,
                sessionType = SessionType.valueOf(json.optString("session_type", "LOCAL")),
                maxDuration = if (json.has("max_duration")) json.getLong("max_duration") else null,
                metadata = metadata
            )
        }
    }
}

data class SessionInfo(
    val sessionId: String,
    val sessionName: String,
    val studyName: String,
    val participantId: String,
    val sessionDirectory: String,
    val enabledSensors: List<String>,
    val sessionType: SessionType,
    val createdAt: Long,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    val duration: Long
        get() = when {
            completedAt != null && startedAt != null -> completedAt - startedAt
            startedAt != null -> System.currentTimeMillis() - startedAt
            else -> 0L
        }
    val isActive: Boolean
        get() = startedAt != null && completedAt == null

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("session_id", sessionId)
            put("session_name", sessionName)
            put("study_name", studyName)
            put("participant_id", participantId)
            put("session_directory", sessionDirectory)
            put("enabled_sensors", enabledSensors.joinToString(","))
            put("session_type", sessionType.name)
            put("created_at", createdAt)
            put("started_at", startedAt)
            put("completed_at", completedAt)
            put("duration", duration)
            put("is_active", isActive)
            put("metadata", JSONObject(metadata))
        }
    }
}

enum class SessionType {
    LOCAL,
    REMOTE,
    HYBRID,
    RESEARCH
}

enum class SessionStatus(val displayName: String) {
    IDLE("Idle"),
    CREATED("Created"),
    STARTING("Starting"),
    RECORDING("Recording"),
    PAUSED("Paused"),
    STOPPING("Stopping"),
    COMPLETED("Completed"),
    ERROR("Error");

    val isActive: Boolean
        get() = this == RECORDING || this == PAUSED
    val isTransitioning: Boolean
        get() = this == STARTING || this == STOPPING
    val isCompleted: Boolean
        get() = this == COMPLETED || this == ERROR
}

data class SessionQuality(
    val overallQuality: Double = 0.0,
    val networkQuality: Double = 0.0,
    val gsrQuality: Double = 0.0,
    val thermalQuality: Double = 0.0,
    val rgbQuality: Double = 0.0,
    val gsrSampleCount: Long = 0L,
    val thermalFrameCount: Long = 0L,
    val rgbFrameCount: Long = 0L,
    val syncMarkerCount: Long = 0L,
    val errorCount: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val qualityLevel: QualityLevel
        get() = when {
            overallQuality >= 0.9 -> QualityLevel.EXCELLENT
            overallQuality >= 0.7 -> QualityLevel.GOOD
            overallQuality >= 0.5 -> QualityLevel.FAIR
            overallQuality >= 0.3 -> QualityLevel.POOR
            else -> QualityLevel.CRITICAL
        }
    val totalSamples: Long
        get() = gsrSampleCount + thermalFrameCount + rgbFrameCount
    val isAcceptableQuality: Boolean
        get() = overallQuality >= 0.6 && errorCount < 10

    fun toMap(): Map<String, Any> {
        return mapOf(
            "overall_quality" to overallQuality,
            "network_quality" to networkQuality,
            "gsr_quality" to gsrQuality,
            "thermal_quality" to thermalQuality,
            "rgb_quality" to rgbQuality,
            "gsr_sample_count" to gsrSampleCount,
            "thermal_frame_count" to thermalFrameCount,
            "rgb_frame_count" to rgbFrameCount,
            "sync_marker_count" to syncMarkerCount,
            "error_count" to errorCount,
            "total_samples" to totalSamples,
            "quality_level" to qualityLevel.name,
            "is_acceptable_quality" to isAcceptableQuality,
            "last_updated" to lastUpdated
        )
    }

    enum class QualityLevel {
        CRITICAL,
        POOR,
        FAIR,
        GOOD,
        EXCELLENT
    }
}

data class SessionStatistics(
    val sessionId: String?,
    val isActive: Boolean,
    val duration: Long,
    val status: SessionStatus,
    val enabledSensors: List<String>,
    val dataQuality: Double,
    val networkQuality: Double,
    val gsrSamples: Long,
    val thermalFrames: Long,
    val rgbFrames: Long,
    val syncMarkers: Long,
    val errors: Long
) {
    val totalDataPoints: Long
        get() = gsrSamples + thermalFrames + rgbFrames + syncMarkers
    val averageSamplingRate: Double
        get() = if (duration > 0) {
            (totalDataPoints * 1000.0) / duration
        } else 0.0
    val qualityStatus: String
        get() = when {
            dataQuality >= 0.9 -> "Excellent"
            dataQuality >= 0.7 -> "Good"
            dataQuality >= 0.5 -> "Fair"
            dataQuality >= 0.3 -> "Poor"
            else -> "Critical"
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "session_id" to sessionId,
            "is_active" to isActive,
            "duration" to duration,
            "duration_formatted" to formatDuration(duration),
            "status" to status.displayName,
            "enabled_sensors" to enabledSensors,
            "data_quality" to dataQuality,
            "network_quality" to networkQuality,
            "gsr_samples" to gsrSamples,
            "thermal_frames" to thermalFrames,
            "rgb_frames" to rgbFrames,
            "sync_markers" to syncMarkers,
            "errors" to errors,
            "total_data_points" to totalDataPoints,
            "average_sampling_rate" to averageSamplingRate,
            "quality_status" to qualityStatus
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}

data class SessionSummary(
    val sessionId: String,
    val duration: Long,
    val totalSamples: Long,
    val averageQuality: Double,
    val completedSuccessfully: Boolean,
    val errorCount: Long,
    val dataSize: Long,
    val metadata: Map<String, Any> = emptyMap()
) {
    val dataSizeFormatted: String
        get() = formatBytes(dataSize)
    val successRate: Double
        get() = if (totalSamples > 0) {
            ((totalSamples - errorCount).toDouble() / totalSamples.toDouble()) * 100.0
        } else 0.0

    fun toMap(): Map<String, Any> {
        return mapOf(
            "session_id" to sessionId,
            "duration" to duration,
            "total_samples" to totalSamples,
            "average_quality" to averageQuality,
            "completed_successfully" to completedSuccessfully,
            "error_count" to errorCount,
            "data_size" to dataSize,
            "data_size_formatted" to dataSizeFormatted,
            "success_rate" to successRate,
            "metadata" to metadata
        )
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}

data class SensorConfig(
    val sensorType: String,
    val enabled: Boolean,
    val samplingRate: Double? = null,
    val configuration: Map<String, Any> = emptyMap()
) {
    val isGSR: Boolean
        get() = sensorType.equals("gsr", ignoreCase = true)
    val isThermal: Boolean
        get() = sensorType.equals("thermal", ignoreCase = true)
    val isRGB: Boolean
        get() = sensorType.equals("rgb", ignoreCase = true)
}