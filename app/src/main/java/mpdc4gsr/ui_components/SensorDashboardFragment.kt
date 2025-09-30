package mpdc4gsr.ui_components

/**
 * Fragment-related constants and types for sensor dashboard
 * This provides compatibility for activities that reference Fragment types
 */
object SensorDashboardFragment {
    enum class SensorStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        WARNING,
        ERROR
    }
}
