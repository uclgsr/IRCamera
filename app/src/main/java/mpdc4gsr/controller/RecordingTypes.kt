package mpdc4gsr.controller

/**
 * Public API aliases for recording types
 * This package provides a stable public API by aliasing internal implementation types
 */

// Enums
typealias RecordingState = mpdc4gsr.feature.network.data.RecordingState
typealias TriggerSource = mpdc4gsr.feature.network.data.TriggerSource
typealias SessionState = mpdc4gsr.feature.network.data.SessionState

// Data classes
typealias SessionManifest = mpdc4gsr.feature.network.data.SessionManifest
typealias SessionEvent = mpdc4gsr.feature.network.data.SessionEvent
typealias SensorActivityInfo = mpdc4gsr.feature.network.data.SensorActivityInfo
typealias SensorHealthInfo = mpdc4gsr.feature.network.data.SensorHealthInfo
typealias DropoutEvent = mpdc4gsr.feature.network.data.DropoutEvent
typealias ReconnectionEvent = mpdc4gsr.feature.network.data.ReconnectionEvent

// Recording controller types
typealias RecordingControllerSessionEvent = mpdc4gsr.feature.network.data.RecordingControllerSessionEvent
typealias RecordingControllerSensorActivityInfo = mpdc4gsr.feature.network.data.RecordingControllerSensorActivityInfo
typealias RecordingControllerSensorHealthInfo = mpdc4gsr.feature.network.data.RecordingControllerSensorHealthInfo
typealias RecordingControllerDropoutEvent = mpdc4gsr.feature.network.data.RecordingControllerDropoutEvent
typealias RecordingControllerReconnectionEvent = mpdc4gsr.feature.network.data.RecordingControllerReconnectionEvent
