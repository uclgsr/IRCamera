package mpdc4gsr.domain.model

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
            batteryLevel?.let { append(" • $it% battery") }
            if (isRecommended) append(" • Recommended")
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
