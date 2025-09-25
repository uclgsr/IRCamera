package mpdc4gsr.controller

/**
 * Legacy compatibility exports - data classes moved to RecordingTypes.kt
 * This file maintains backward compatibility for existing imports
 */

// Re-export centralized types for backward compatibility
typealias SessionManifestCompat = SessionManifest
typealias SessionEventCompat = SessionEvent
typealias SensorActivityInfoCompat = SensorActivityInfo
typealias SensorHealthInfoCompat = SensorHealthInfo
typealias DropoutEventCompat = DropoutEvent
typealias ReconnectionEventCompat = ReconnectionEvent
