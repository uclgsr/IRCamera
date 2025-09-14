package com.topdon.tc001.sensors.shimmer.model

data class ShimmerDeviceInfo(
    val macAddress: String,
    val name: String,
    val rssi: Int,
    val isPaired: Boolean,
    val priority: Int,
    val connectionState: String,
    val deviceType: String = detectDeviceType(name),
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null
) {

    companion object {

        private val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72", "00:80:98")

        fun detectDeviceType(deviceName: String): String = when {
            deviceName.contains("Shimmer3-GSR", ignoreCase = true) -> "Shimmer3 GSR+"
            deviceName.contains("GSR", ignoreCase = true) -> "Shimmer GSR"
            deviceName.contains("Shimmer3", ignoreCase = true) -> "Shimmer3"
            deviceName.contains("Shimmer", ignoreCase = true) -> "Shimmer (Generic)"
            else -> "Unknown"
        }

        fun isValidShimmerMAC(macAddress: String): Boolean {
            return SHIMMER_MAC_PREFIXES.any { prefix ->
                macAddress.startsWith(prefix, ignoreCase = true)
            }
        }

        fun calculatePriority(
            macAddress: String,
            deviceName: String,
            rssi: Int,
            isPaired: Boolean
        ): Int {
            var priority = 0

            when {
                macAddress.startsWith("00:06:66", true) -> priority += 100  // Primary
                macAddress.startsWith("d0:39:72", true) -> priority += 90   // Secondary
                macAddress.startsWith("00:80:98", true) -> priority += 80   // Alternative
            }

            when {
                deviceName.contains("Shimmer3-GSR", true) -> priority += 50
                deviceName.contains("GSR", true) -> priority += 30
                deviceName.contains("Shimmer3", true) -> priority += 25
                deviceName.contains("Shimmer", true) -> priority += 20
            }

            if (isPaired) priority += 25

            priority += maxOf(0, (rssi + 100) / 2)

            return priority
        }
    }

    fun isValidGSRDevice(): Boolean {
        return isValidShimmerMAC(macAddress) &&
                (name.contains("GSR", ignoreCase = true) ||
                        name.contains("Shimmer", ignoreCase = true))
    }

    fun getSignalQuality(): String = when {
        rssi >= -50 -> "Excellent"
        rssi >= -60 -> "Good"
        rssi >= -70 -> "Fair"
        rssi >= -80 -> "Poor"
        else -> "Weak"
    }

    fun getDetailedStatus(): String {
        val quality = getSignalQuality()
        return "$connectionState ($quality, ${rssi}dBm)"
    }

    fun isReadyForConnection(): Boolean {
        return connectionState in listOf("Available", "Discovered", "Ready") &&
                rssi >= -85  // Minimum signal strength threshold
    }

    fun getDisplayName(): String {
        return "$name\n${macAddress} • ${getSignalQuality()}"
    }

    fun withConnectionState(newState: String): ShimmerDeviceInfo {
        return copy(connectionState = newState)
    }

    fun withRSSI(newRssi: Int): ShimmerDeviceInfo {
        return copy(
            rssi = newRssi,
            priority = calculatePriority(macAddress, name, newRssi, isPaired)
        )
    }

    override fun toString(): String {
        return "ShimmerDevice(name='$name', mac='$macAddress', rssi=${rssi}dBm, " +
                "type='$deviceType', priority=$priority, paired=$isPaired, state='$connectionState')"
    }
}
