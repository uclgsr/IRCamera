package mpdc4gsr.core.ui.components.sensors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class SensorStatusUi {
    Disconnected,
    Connecting,
    Connected,
    Streaming,
    Error,
    Simulation,
}

data class SensorDashboardItem(
    val id: String,
    val name: String,
    val status: SensorStatusUi,
    val message: String,
    val icon: ImageVector,
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never",
    val signalStrength: Int = 0,
    val isAnimating: Boolean = false,
)

enum class SensorOption(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val isAvailableByDefault: Boolean = true,
    val requiresPermission: Boolean = false,
) {
    Thermal(
        displayName = "Thermal Camera",
        description = "TC001/TS004 thermal imaging sensor",
        icon = Icons.Default.Thermostat,
    ),
    Rgb(
        displayName = "RGB Camera",
        description = "High-resolution color camera",
        icon = Icons.Default.Camera,
        requiresPermission = true,
    ),
    Gsr(
        displayName = "GSR Sensor",
        description = "Galvanic skin response via Shimmer3",
        icon = Icons.Default.Sensors,
        requiresPermission = true,
    ),
    Audio(
        displayName = "Audio Recorder",
        description = "High-quality audio capture",
        icon = Icons.Default.Mic,
        requiresPermission = true,
    ),
    Accelerometer(
        displayName = "Accelerometer",
        description = "Motion and orientation sensor",
        icon = Icons.Default.Wifi,
    ),
    Gyroscope(
        displayName = "Gyroscope",
        description = "Angular velocity sensor",
        icon = Icons.AutoMirrored.Filled.RotateRight,
    ),
    Magnetometer(
        displayName = "Magnetometer",
        description = "Magnetic field sensor",
        icon = Icons.Default.Explore,
    ),
    HeartRate(
        displayName = "Heart Rate",
        description = "Optical heart rate monitor",
        icon = Icons.Default.Favorite,
        isAvailableByDefault = false,
    ),
    Temperature(
        displayName = "Temperature",
        description = "Ambient temperature sensor",
        icon = Icons.Default.DeviceThermostat,
    ),
    Humidity(
        displayName = "Humidity",
        description = "Environmental humidity sensor",
        icon = Icons.Default.Water,
        isAvailableByDefault = false,
    ),
    NetworkSync(
        displayName = "Network Sync",
        description = "Synchronization over network",
        icon = Icons.Default.NetworkCheck,
    ),
    Storage(
        displayName = "Storage System",
        description = "Local storage availability",
        icon = Icons.Default.Storage,
    ),
    BluetoothPeripheral(
        displayName = "Bluetooth Device",
        description = "Bluetooth LE peripherals",
        icon = Icons.Default.Bluetooth,
    ),
    ScheduledTask(
        displayName = "Scheduled Task",
        description = "Automation and scheduling",
        icon = Icons.Default.Schedule,
    ),
}

data class SensorAvailability(
    val option: SensorOption,
    val isAvailable: Boolean,
    val isSelected: Boolean,
    val availabilityReason: String = "",
    val batteryImpact: String = "Low",
    val dataRate: String = "Unknown",
)
