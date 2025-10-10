package mpdc4gsr.core.data.model

data class PCControllerInfo(
    val name: String,
    val host: String,
    val port: Int,
    val type: String,
    val properties: Map<String, String> = emptyMap(),
    val capabilities: List<String> = emptyList(),
    val protocolVersion: String = "1.0",
    val lastSeen: Long = System.currentTimeMillis(),
) {
    val address: String
        get() = "$host:$port"
    val supportsGSR: Boolean
        get() =
            capabilities.contains("gsr") ||
                    properties["supports_gsr"] == "true" ||
                    properties.containsKey("shimmer_support")
    val supportsThermal: Boolean
        get() =
            capabilities.contains("thermal") ||
                    properties["supports_thermal"] == "true"
    val supportsRGB: Boolean
        get() =
            capabilities.contains("rgb") ||
                    properties["supports_rgb"] == "true"
    val supportsSecure: Boolean
        get() =
            capabilities.contains("tls") ||
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
        get() =
            buildString {
                append(address)
                softwareVersion?.let { append(" • v$it") }
                platform?.let { append(" • $it") }
                val supportedFeatures = mutableListOf<String>()
                if (supportsGSR) supportedFeatures.add("GSR")
                if (supportsThermal) supportedFeatures.add("Thermal")
                if (supportsRGB) supportedFeatures.add("RGB")
                if (supportedFeatures.isNotEmpty()) {
                    append(" • ${supportedFeatures.joinToString("/")}")
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

    fun toMap(): Map<String, Any?> =
        mapOf(
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
            "connection_priority" to connectionPriority,
        )

    fun getWebSocketUrl(secure: Boolean = false): String {
        val protocol = if (secure && supportsSecure) "wss" else "ws"
        return "$protocol://$host:$port"
    }

    fun isCompatibleWith(requiredFeatures: List<String>): Boolean =
        requiredFeatures.all { feature ->
            when (feature.lowercase()) {
                "gsr" -> supportsGSR
                "thermal" -> supportsThermal
                "rgb" -> supportsRGB
                "secure", "tls" -> supportsSecure
                else -> capabilities.contains(feature) || properties.containsKey(feature)
            }
        }

    companion object {
        fun fromServiceInfo(
            serviceName: String,
            hostAddress: String,
            port: Int,
            serviceType: String,
            txtRecord: Map<String, String>,
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
                protocolVersion = protocolVersion,
            )
        }

        fun createMockController(
            controllerId: String = "test_pc",
            includeGSR: Boolean = true,
            includeThermal: Boolean = true,
            includeRGB: Boolean = true,
        ): PCControllerInfo {
            val capabilities = mutableListOf<String>()
            if (includeGSR) capabilities.add("gsr")
            if (includeThermal) capabilities.add("thermal")
            if (includeRGB) capabilities.add("rgb")
            capabilities.add("tls")
            val properties =
                mapOf(
                    "version" to "2.1.0",
                    "platform" to "Windows 11",
                    "display_name" to "IRCamera PC Controller $controllerId",
                    "supports_gsr" to includeGSR.toString(),
                    "supports_thermal" to includeThermal.toString(),
                    "supports_rgb" to includeRGB.toString(),
                    "secure" to "true",
                    "shimmer_support" to "true",
                )
            return PCControllerInfo(
                name = "ircamera-pc-$controllerId",
                host = "192.168.1.${100 + controllerId.hashCode() % 50}",
                port = 8888,
                type = "_ircamera._tcp.local.",
                properties = properties,
                capabilities = capabilities,
                protocolVersion = "2.0",
            )
        }

        fun sortByPriority(controllers: List<PCControllerInfo>): List<PCControllerInfo> =
            controllers.sortedByDescending { it.connectionPriority }

        fun filterByFeatures(
            controllers: List<PCControllerInfo>,
            requiredFeatures: List<String>,
        ): List<PCControllerInfo> = controllers.filter { it.isCompatibleWith(requiredFeatures) }

        fun getGSRCapableControllers(controllers: List<PCControllerInfo>): List<PCControllerInfo> =
            controllers.filter { it.supportsGSR }

        fun getActiveControllers(controllers: List<PCControllerInfo>): List<PCControllerInfo> =
            controllers.filter { it.isRecentlyActive }
    }
}

