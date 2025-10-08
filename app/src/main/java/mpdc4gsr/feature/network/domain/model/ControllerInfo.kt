package mpdc4gsr.feature.network.domain.model

data class ControllerInfo(
    val ipAddress: String,
    val port: Int,
    val deviceName: String,
    val capabilities: List<String>,
    val lastSeen: Long = System.currentTimeMillis()
)
