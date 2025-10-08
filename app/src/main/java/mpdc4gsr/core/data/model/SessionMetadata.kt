package mpdc4gsr.core.data.model

import android.os.SystemClock
import com.google.gson.GsonBuilder
import mpdc4gsr.core.data.RecordingStats
import mpdc4gsr.core.data.TimestampManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SessionMetadata(
    val sessionId: String,
    val sessionStartTimestampMs: Long,
    val sessionEndTimestampMs: Long? = null,
    val sessionStartMonotonicNs: Long,
    val sessionEndMonotonicNs: Long? = null,
    val sessionStartIso: String,
    val sessionEndIso: String? = null,
    val deviceModel: String = android.os.Build.MODEL,
    val deviceManufacturer: String = android.os.Build.MANUFACTURER,
    val timingSource: String = "android_monotonic_realtime",
    val modalityFiles: MutableMap<String, String> = mutableMapOf(),
    val syncEvents: MutableList<SessionSyncEvent> = mutableListOf(),
    val sensorSummaries: MutableMap<String, SensorSummary> = mutableMapOf(),
    val stopResults: MutableMap<String, Boolean> = mutableMapOf(),
    val recordingDurationMs: Long? = null,
    // Enhanced metadata for TODO requirement: "Expand the metadata.json to include all relevant session info"
    val sessionName: String? = null,
    val studyName: String? = null,
    val participantId: String? = null,
    val userNotes: String? = null,
    val experimentalConditions: Map<String, Any> = emptyMap(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val environmentalConditions: EnvironmentalConditions = EnvironmentalConditions(),
    val networkSyncInfo: NetworkSyncInfo = NetworkSyncInfo(),
    val softwareVersions: Map<String, String> = emptyMap(),
    val calibrationInfo: Map<String, CalibrationData> = emptyMap(),
    val qualityMetrics: QualityMetrics = QualityMetrics(),
    val dataIntegrityChecks: Map<String, Boolean> = emptyMap()
) {
    data class DeviceInfo(
        val model: String = android.os.Build.MODEL,
        val manufacturer: String = android.os.Build.MANUFACTURER,
        val androidVersion: String = android.os.Build.VERSION.RELEASE,
        val apiLevel: Int = android.os.Build.VERSION.SDK_INT,
        val serialNumber: String = getDeviceSerial(),
        val hardwareCapabilities: Map<String, Boolean> = emptyMap(),
        val cpuInfo: String = getCPUInfo(),
        val memoryInfo: String = getMemoryInfo()
    ) {
        companion object {
            private fun getDeviceSerial(): String {
                return (
                    @Suppress("DEPRECATION")
                    android.os.Build.SERIAL.takeIf { it != "unknown" } ?: getPersistentDeviceId()
                    "SN-UNAVAILABLE"
                }
            }

            private fun getPersistentDeviceId(): String {
                return (
                    "DEVICE-${android.os.Build.FINGERPRINT.hashCode().toString(16).uppercase()}"
                    "DEVICE-UNKNOWN"
                }
            }

            private fun getCPUInfo(): String {
                return (
                    "${android.os.Build.HARDWARE} - ${android.os.Build.SUPPORTED_ABIS.joinToString(",")}"
                    "CPU-INFO-UNAVAILABLE"
                }
            }

            private fun getMemoryInfo(): String {
                return (
                    val runtime = Runtime.getRuntime()
                    val maxMemory = runtime.maxMemory() / (1024 * 1024)
                    "Max: ${maxMemory}MB"
                    "MEMORY-INFO-UNAVAILABLE"
                }
            }
        }
    }

    data class EnvironmentalConditions(
        val ambientTemperatureC: Double? = null,
        val humidityPercent: Double? = null,
        val lightingConditions: String? = null,
        val noiseLevel: String? = null,
        val locationDescription: String? = null,
        val weatherConditions: String? = null,
        val roomConditions: Map<String, Any> = emptyMap()
    )

    data class NetworkSyncInfo(
        val pcControllerAddress: String? = null,
        val clockOffsetMs: Long = 0,
        val networkLatencyMs: Long = 0,
        val syncQuality: Double = 0.0,
        val syncAttempts: Int = 0,
        val lastSyncTime: Long = 0,
        val driftMeasurements: List<Long> = emptyList()
    )

    data class CalibrationData(
        val sensorType: String,
        val calibrationTimestamp: Long,
        val calibrationParameters: Map<String, Double>,
        val accuracyMetrics: Map<String, Double>,
        val validationStatus: String,
        val calibrationNotes: String? = null
    )

    data class QualityMetrics(
        val overallQualityScore: Double = 0.0,
        val sensorQualityScores: Map<String, Double> = emptyMap(),
        val syncAccuracyMs: Double = 0.0,
        val dataCompletenessPercent: Double = 0.0,
        val errorCount: Int = 0,
        val warningCount: Int = 0,
        val validationsPassed: Int = 0,
        val validationsFailed: Int = 0
    )

    companion object {
        fun createSessionStart(sessionId: String): SessionMetadata {
            val wallClockStartMs = System.currentTimeMillis()
            val monotonicStartNs = SystemClock.elapsedRealtimeNanos()
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
            return SessionMetadata(
                sessionId = sessionId,
                sessionStartTimestampMs = wallClockStartMs,
                sessionStartMonotonicNs = monotonicStartNs,
                sessionStartIso = isoFormatter.format(Date(wallClockStartMs))
            )
        }
    }

    fun markSessionEnd(): SessionMetadata {
        val wallClockEndMs = System.currentTimeMillis()
        val monotonicEndNs = SystemClock.elapsedRealtimeNanos()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
        val durationMs = (monotonicEndNs - sessionStartMonotonicNs) / 1_000_000L
        return this.copy(
            sessionEndTimestampMs = wallClockEndMs,
            sessionEndMonotonicNs = monotonicEndNs,
            sessionEndIso = isoFormatter.format(Date(wallClockEndMs)),
            recordingDurationMs = durationMs
        )
    }

    fun addModalityFile(modalityType: String, fileName: String, startOffsetMs: Long = 0) {
        modalityFiles[modalityType] = fileName
        syncEvents.add(
            SessionSyncEvent(
                eventType = "${modalityType}_START",
                timestampMs = sessionStartTimestampMs + startOffsetMs,
                monotonicOffsetNs = startOffsetMs * 1_000_000L,
                metadata = mapOf(
                    "modality" to modalityType,
                    "file" to fileName,
                    "offset_ms" to startOffsetMs.toString()
                )
            )
        )
    }

    fun addSyncEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        val currentWallMs = System.currentTimeMillis()
        val currentMonotonicNs = SystemClock.elapsedRealtimeNanos()
        val offsetFromStartNs = currentMonotonicNs - sessionStartMonotonicNs
        syncEvents.add(
            SessionSyncEvent(
                eventType = eventType,
                timestampMs = currentWallMs,
                monotonicOffsetNs = offsetFromStartNs,
                metadata = metadata
            )
        )
    }

    private fun relativeMillis(monotonicNs: Long): Long {
        return (monotonicNs - sessionStartMonotonicNs) / 1_000_000L
    }

    fun markSensorStart(
        sensorName: String,
        sensorId: String,
        sensorType: String,
        startMonotonicNs: Long,
        metadata: Map<String, String> = emptyMap()
    ) {
        val summary = SensorSummary(
            sensorId = sensorId,
            sensorType = sensorType,
            startTimestampNs = startMonotonicNs,
            startTimestampMs = monotonicToWallClock(startMonotonicNs),
            relativeStartMs = relativeMillis(startMonotonicNs)
        )
        summary.metadata.putAll(metadata)
        sensorSummaries[sensorName] = summary
    }

    fun markSensorStop(
        sensorName: String,
        stopMonotonicNs: Long,
        success: Boolean,
        stats: RecordingStats? = null,
        metadata: Map<String, String> = emptyMap(),
        errorMessage: String? = null,
        sensorId: String? = null,
        sensorType: String? = null
    ) {
        val summary = sensorSummaries[sensorName] ?: SensorSummary(
            sensorId = sensorId ?: sensorName,
            sensorType = sensorType ?: "unknown",
            startTimestampNs = sessionStartMonotonicNs,
            startTimestampMs = sessionStartTimestampMs,
            relativeStartMs = 0L
        )
        summary.stopTimestampNs = stopMonotonicNs
        summary.stopTimestampMs = monotonicToWallClock(stopMonotonicNs)
        summary.relativeStopMs = relativeMillis(stopMonotonicNs)
        summary.status = if (success) "COMPLETED" else "FAILED"
        summary.metadata.putAll(metadata)
        stats?.let {
            summary.samplesRecorded = it.totalSamplesRecorded
            summary.averageDataRate = it.averageDataRate
            summary.droppedSamples = it.droppedSamples
            summary.syncMarkers = it.syncMarkersCount
            summary.storageUsedMb = it.storageUsedMB
        }
        if (!success && errorMessage != null) {
            summary.errors.add(errorMessage)
        }
        sensorSummaries[sensorName] = summary
    }

    fun recordStopResults(results: Map<String, Boolean>) {
        stopResults.clear()
        stopResults.putAll(results)
    }

    fun getRelativeTimestamp(): Long {
        val currentMonotonicNs = SystemClock.elapsedRealtimeNanos()
        return (currentMonotonicNs - sessionStartMonotonicNs) / 1_000_000L
    }

    fun monotonicToWallClock(monotonicNs: Long): Long {
        return (
            TimestampManager.convertMonotonicToWallClock(monotonicNs)
            val offsetFromStartNs = monotonicNs - sessionStartMonotonicNs
            sessionStartTimestampMs + (offsetFromStartNs / 1_000_000L)
        }
    }

    fun saveToFile(sessionDirectory: File): File {
        val metadataFile = File(sessionDirectory, "session_metadata.json")
        val gson = GsonBuilder().setPrettyPrinting().create()
            metadataFile.writeText(gson.toJson(this))
            android.util.Log.i(TAG, "Session metadata saved: ${metadataFile.absolutePath}")
            android.util.Log.e(TAG, "Failed to save session metadata", e)
        }
        return metadataFile
    }

    fun exportToUnifiedMetadataFile(sessionDirectory: File): Boolean {
        return (
            val metadataFile = File(sessionDirectory, "session_metadata_complete.json")
            val gson = GsonBuilder().setPrettyPrinting().create()
            val comprehensiveMetadata = buildComprehensiveMetadata()
            val jsonContent = gson.toJson(comprehensiveMetadata)
            metadataFile.writeText(jsonContent)
            android.util.Log.i(
                TAG,
                "Comprehensive session metadata exported to: ${metadataFile.absolutePath}"
            )
            true
            android.util.Log.e(TAG, "Failed to export comprehensive session metadata", e)
            false
        }
    }

    private fun buildComprehensiveMetadata(): Map<String, Any> {
        return mapOf(
            "session_header" to mapOf(
                "session_id" to sessionId,
                "session_name" to (sessionName ?: "Unnamed Session"),
                "study_name" to (studyName ?: ""),
                "participant_id" to (participantId ?: ""),
                "user_notes" to (userNotes ?: ""),
                "experimental_conditions" to experimentalConditions
            ),
            "timing_information" to mapOf(
                "session_start_utc_ms" to sessionStartTimestampMs,
                "session_start_iso" to sessionStartIso,
                "session_start_monotonic_ns" to sessionStartMonotonicNs,
                "session_end_utc_ms" to sessionEndTimestampMs,
                "session_end_iso" to sessionEndIso,
                "session_end_monotonic_ns" to sessionEndMonotonicNs,
                "recording_duration_ms" to recordingDurationMs,
                "timing_source" to timingSource
            ),
            "device_information" to mapOf(
                "primary_device" to deviceInfo,
                "software_versions" to softwareVersions,
                "environmental_conditions" to environmentalConditions
            ),
            "network_synchronization" to mapOf(
                "pc_controller_sync" to networkSyncInfo,
                "sync_events" to syncEvents.map { syncEvent ->
                    mapOf(
                        "event_type" to syncEvent.eventType,
                        "timestamp_ms" to syncEvent.timestampMs,
                        "monotonic_offset_ns" to syncEvent.monotonicOffsetNs,
                        "metadata" to syncEvent.metadata
                    )
                }
            ),
            "sensor_summaries" to sensorSummaries.mapValues { (sensorId, summary) ->
                mapOf(
                    "sensor_id" to summary.sensorId,
                    "sensor_type" to summary.sensorType,
                    "timing" to mapOf(
                        "start_timestamp_ns" to summary.startTimestampNs,
                        "start_timestamp_ms" to summary.startTimestampMs,
                        "relative_start_ms" to summary.relativeStartMs,
                        "stop_timestamp_ns" to summary.stopTimestampNs,
                        "stop_timestamp_ms" to summary.stopTimestampMs,
                        "relative_stop_ms" to summary.relativeStopMs
                    ),
                    "performance" to mapOf(
                        "samples_recorded" to summary.samplesRecorded,
                        "average_data_rate" to summary.averageDataRate,
                        "dropped_samples" to summary.droppedSamples,
                        "sync_markers" to summary.syncMarkers,
                        "storage_used_mb" to summary.storageUsedMb
                    ),
                    "status" to summary.status,
                    "errors" to summary.errors,
                    "metadata" to summary.metadata
                )
            },
            "calibration_data" to calibrationInfo.mapValues { (sensorType, calibration) ->
                mapOf(
                    "sensor_type" to calibration.sensorType,
                    "calibration_timestamp" to calibration.calibrationTimestamp,
                    "parameters" to calibration.calibrationParameters,
                    "accuracy_metrics" to calibration.accuracyMetrics,
                    "validation_status" to calibration.validationStatus,
                    "notes" to calibration.calibrationNotes
                )
            },
            "data_files" to mapOf(
                "modality_files" to modalityFiles,
                "file_schema" to mapOf(
                    "thermal_data_csv" to mpdc4gsr.core.data.FileSchemaManager()
                        .generateCsvHeader("thermal", includeUnits = false),
                    "rgb_data_csv" to mpdc4gsr.core.data.FileSchemaManager()
                        .generateCsvHeader("rgb", includeUnits = false),
                    "gsr_data_csv" to mpdc4gsr.core.data.FileSchemaManager()
                        .generateCsvHeader("gsr", includeUnits = false),
                    "audio_data_csv" to mpdc4gsr.core.data.FileSchemaManager()
                        .generateCsvHeader("audio", includeUnits = false)
                )
            ),
            "quality_assurance" to mapOf(
                "quality_metrics" to qualityMetrics,
                "data_integrity_checks" to dataIntegrityChecks,
                "stop_results" to stopResults,
                "validation_summary" to mapOf(
                    "overall_quality_score" to qualityMetrics.overallQualityScore,
                    "sync_accuracy_ms" to qualityMetrics.syncAccuracyMs,
                    "data_completeness_percent" to qualityMetrics.dataCompletenessPercent,
                    "total_errors" to qualityMetrics.errorCount,
                    "total_warnings" to qualityMetrics.warningCount
                )
            ),
            "export_information" to mapOf(
                "export_timestamp" to System.currentTimeMillis(),
                "export_version" to "1.2.0",
                "format_specification" to "IRCamera Enhanced Metadata v1.2",
                "data_completeness" to "Full session metadata with all sensor contributions"
            )
        )
    }

    fun updateQualityMetrics(
        sensorQualityScores: Map<String, Double>,
        syncAccuracy: Double,
        dataCompleteness: Double,
        errorCount: Int,
        warningCount: Int
    ): SessionMetadata {
        val overallScore = sensorQualityScores.values.average()
        val updatedQualityMetrics = qualityMetrics.copy(
            overallQualityScore = overallScore,
            sensorQualityScores = sensorQualityScores,
            syncAccuracyMs = syncAccuracy,
            dataCompletenessPercent = dataCompleteness,
            errorCount = errorCount,
            warningCount = warningCount,
            validationsPassed = sensorQualityScores.values.count { it >= 0.7 },
            validationsFailed = sensorQualityScores.values.count { it < 0.7 }
        )
        return this.copy(qualityMetrics = updatedQualityMetrics)
    }

    fun addCalibrationInfo(sensorType: String, calibrationData: CalibrationData): SessionMetadata {
        val updatedCalibrationInfo = calibrationInfo.toMutableMap()
        updatedCalibrationInfo[sensorType] = calibrationData
        return this.copy(calibrationInfo = updatedCalibrationInfo)
    }

    fun updateNetworkSyncInfo(
        pcAddress: String?,
        clockOffset: Long,
        latency: Long,
        quality: Double
    ): SessionMetadata {
        val updatedNetworkSyncInfo = networkSyncInfo.copy(
            pcControllerAddress = pcAddress,
            clockOffsetMs = clockOffset,
            networkLatencyMs = latency,
            syncQuality = quality,
            syncAttempts = networkSyncInfo.syncAttempts + 1,
            lastSyncTime = System.currentTimeMillis()
        )
        return this.copy(networkSyncInfo = updatedNetworkSyncInfo)
    }

    fun generateSessionSummaryText(): String {
        return buildString {
            appendLine("=== IRCamera Session Summary ===")
            appendLine("Session ID: $sessionId")
            appendLine("Session Name: ${sessionName ?: "Unnamed"}")
            appendLine("Study: ${studyName ?: "No study specified"}")
            appendLine("Participant: ${participantId ?: "No participant ID"}")
            appendLine()
            appendLine("Timing Information:")
            appendLine("  Start: $sessionStartIso")
            appendLine("  End: ${sessionEndIso ?: "In progress"}")
            appendLine("  Duration: ${recordingDurationMs?.let { "${it / 1000.0}s" } ?: "Unknown"}")
            appendLine()
            appendLine("Device Information:")
            appendLine("  Device: ${deviceInfo.manufacturer} ${deviceInfo.model}")
            appendLine("  Android: ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
            appendLine("  Serial: ${deviceInfo.serialNumber}")
            appendLine()
            appendLine("Sensor Summary:")
            sensorSummaries.forEach { (id, summary) ->
                appendLine("  $id (${summary.sensorType}):")
                appendLine("    Status: ${summary.status}")
                appendLine("    Samples: ${summary.samplesRecorded ?: "Unknown"}")
                appendLine("    Errors: ${summary.errors.size}")
            }
            appendLine()
            appendLine("Quality Metrics:")
            appendLine(
                "  Overall Score: ${
                    String.format(
                        "%.2f",
                        qualityMetrics.overallQualityScore
                    )
                }"
            )
            appendLine("  Sync Accuracy: ${qualityMetrics.syncAccuracyMs}ms")
            appendLine(
                "  Data Completeness: ${
                    String.format(
                        "%.1f%%",
                        qualityMetrics.dataCompletenessPercent
                    )
                }"
            )
            appendLine("  Errors: ${qualityMetrics.errorCount}")
            appendLine("  Warnings: ${qualityMetrics.warningCount}")
            appendLine()
            appendLine("Data Files:")
            modalityFiles.forEach { (modality, filename) ->
                appendLine("  $modality: $filename")
            }
            appendLine("=== End Summary ===")
        }
    }

    fun createTimingHeader(): String {
        return buildString {
            appendLine("# Multi-Modal Recording Session Timing Information")
            appendLine("# Session ID: $sessionId")
            appendLine("# Session Start: $sessionStartIso (${sessionStartTimestampMs}ms UTC)")
            appendLine("# Monotonic Start: ${sessionStartMonotonicNs}ns")
            appendLine("# Timing Source: $timingSource")
            appendLine("# Device: $deviceManufacturer $deviceModel")
            appendLine("#")
            appendLine("# Timestamps in this file are:")
            appendLine("#   - Wall clock: UTC milliseconds since epoch")
            appendLine("#   - Relative: milliseconds since session start (monotonic)")
            appendLine("#   - Monotonic: nanoseconds since boot (for interval calculation)")
            appendLine("#")
        }
    }
}

data class SessionSyncEvent(
    val eventType: String,
    val timestampMs: Long,
    val monotonicOffsetNs: Long,
    val metadata: Map<String, String> = emptyMap()
)

data class SensorSummary(
    val sensorId: String,
    val sensorType: String,
    val startTimestampNs: Long,
    val startTimestampMs: Long,
    val relativeStartMs: Long,
    var stopTimestampNs: Long? = null,
    var stopTimestampMs: Long? = null,
    var relativeStopMs: Long? = null,
    var status: String = "ACTIVE",
    val errors: MutableList<String> = mutableListOf(),
    var samplesRecorded: Long? = null,
    var averageDataRate: Double? = null,
    var droppedSamples: Long? = null,
    var syncMarkers: Int? = null,
    var storageUsedMb: Double? = null,
    val metadata: MutableMap<String, String> = mutableMapOf()
)
