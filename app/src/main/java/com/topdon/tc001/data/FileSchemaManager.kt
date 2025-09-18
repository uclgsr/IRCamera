package com.topdon.tc001.data

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * File Schema Manager
 * 
 * Enforces consistent file naming and schema conventions across all sensor modalities.
 * Addresses TODO: "Continue enforcing the standardized session directory structure"
 * and "Ensure each sensor's CSV has a clear header and units"
 */
class FileSchemaManager {
    companion object {
        private const val TAG = "FileSchemaManager"
        
        // Standard file naming pattern: {sensor}_{timestamp}_{session}.{extension}
        private const val FILE_NAME_PATTERN = "%s_%s_%s.%s"
        
        // Timestamp format for file names (ISO 8601 compatible)
        private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss_SSS"
        
        // Mandatory CSV columns
        private const val MANDATORY_TIMESTAMP_COLUMN = "timestamp_ns"
        
        // Session directory structure
        private val REQUIRED_DIRECTORIES = listOf(
            "thermal", "rgb", "gsr", "audio", "metadata"
        )
        
        // Schema validation patterns
        private val SENSOR_SCHEMAS = mapOf(
            "thermal" to ThermalSchema(),
            "rgb" to RgbSchema(), 
            "gsr" to GsrSchema(),
            "audio" to AudioSchema()
        )
    }
    
    /**
     * Base schema interface for all sensor types
     */
    interface SensorSchema {
        fun getRequiredColumns(): List<String>
        fun getOptionalColumns(): List<String>
        fun getFileExtensions(): List<String>
        fun validateData(data: Map<String, Any>): ValidationResult
        fun getUnits(): Map<String, String>
    }
    
    /**
     * Validation result for schema compliance
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    /**
     * File naming result
     */
    data class StandardFileName(
        val fileName: String,
        val fullPath: String,
        val isRenamed: Boolean = false,
        val originalName: String? = null
    )
    
    /**
     * Thermal sensor schema definition
     */
    class ThermalSchema : SensorSchema {
        override fun getRequiredColumns(): List<String> = listOf(
            MANDATORY_TIMESTAMP_COLUMN, "frame_index", "temp_matrix_serialized",
            "min_temp_celsius", "max_temp_celsius", "avg_temp_celsius",
            "emissivity", "ambient_temp_celsius"
        )
        
        override fun getOptionalColumns(): List<String> = listOf(
            "hotspot_x", "hotspot_y", "coldspot_x", "coldspot_y",
            "quality_score", "processing_time_ms", "device_serial",
            "firmware_version", "calibration_status"
        )
        
        override fun getFileExtensions(): List<String> = listOf("csv", "json")
        
        override fun getUnits(): Map<String, String> = mapOf(
            MANDATORY_TIMESTAMP_COLUMN to "nanoseconds",
            "frame_index" to "count",
            "min_temp_celsius" to "°C",
            "max_temp_celsius" to "°C", 
            "avg_temp_celsius" to "°C",
            "ambient_temp_celsius" to "°C",
            "emissivity" to "unitless (0.0-1.0)",
            "quality_score" to "unitless (0.0-1.0)",
            "processing_time_ms" to "milliseconds"
        )
        
        override fun validateData(data: Map<String, Any>): ValidationResult {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            
            // Validate temperature ranges
            val minTemp = data["min_temp_celsius"] as? Double
            val maxTemp = data["max_temp_celsius"] as? Double
            val avgTemp = data["avg_temp_celsius"] as? Double
            
            if (minTemp != null && maxTemp != null && minTemp > maxTemp) {
                errors.add("Min temperature ($minTemp) cannot be greater than max temperature ($maxTemp)")
            }
            
            if (avgTemp != null && minTemp != null && maxTemp != null) {
                if (avgTemp < minTemp || avgTemp > maxTemp) {
                    warnings.add("Average temperature ($avgTemp) is outside min-max range [$minTemp, $maxTemp]")
                }
            }
            
            // Validate emissivity range
            val emissivity = data["emissivity"] as? Double
            if (emissivity != null && (emissivity < 0.0 || emissivity > 1.0)) {
                errors.add("Emissivity ($emissivity) must be between 0.0 and 1.0")
            }
            
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }
    
    /**
     * RGB camera schema definition
     */
    class RgbSchema : SensorSchema {
        override fun getRequiredColumns(): List<String> = listOf(
            MANDATORY_TIMESTAMP_COLUMN, "frame_number", "video_timestamp_us",
            "resolution_width", "resolution_height", "frame_rate_fps"
        )
        
        override fun getOptionalColumns(): List<String> = listOf(
            "exposure_time_ns", "iso_value", "focal_length_mm",
            "white_balance_mode", "quality_score", "motion_detected",
            "brightness_level", "contrast_level"
        )
        
        override fun getFileExtensions(): List<String> = listOf("csv", "mp4")
        
        override fun getUnits(): Map<String, String> = mapOf(
            MANDATORY_TIMESTAMP_COLUMN to "nanoseconds",
            "frame_number" to "count",
            "video_timestamp_us" to "microseconds",
            "resolution_width" to "pixels",
            "resolution_height" to "pixels",
            "frame_rate_fps" to "frames per second",
            "exposure_time_ns" to "nanoseconds",
            "focal_length_mm" to "millimeters"
        )
        
        override fun validateData(data: Map<String, Any>): ValidationResult {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            
            // Validate frame rate
            val frameRate = data["frame_rate_fps"] as? Double
            if (frameRate != null && frameRate < 1.0) {
                errors.add("Frame rate ($frameRate) must be at least 1 FPS")
            }
            
            // Validate resolution
            val width = data["resolution_width"] as? Int
            val height = data["resolution_height"] as? Int
            if (width != null && height != null) {
                if (width < 640 || height < 480) {
                    warnings.add("Resolution ${width}x${height} is below recommended minimum (640x480)")
                }
            }
            
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }
    
    /**
     * GSR sensor schema definition
     */
    class GsrSchema : SensorSchema {
        override fun getRequiredColumns(): List<String> = listOf(
            MANDATORY_TIMESTAMP_COLUMN, "gsr_microsiemens", "gsr_raw_12bit",
            "resistance_ohms", "sampling_rate_hz"
        )
        
        override fun getOptionalColumns(): List<String> = listOf(
            "device_id", "battery_level", "signal_quality",
            "gsr_range", "calibration_factor", "temperature_celsius"
        )
        
        override fun getFileExtensions(): List<String> = listOf("csv")
        
        override fun getUnits(): Map<String, String> = mapOf(
            MANDATORY_TIMESTAMP_COLUMN to "nanoseconds",
            "gsr_microsiemens" to "µS",
            "gsr_raw_12bit" to "ADC counts (0-4095)",
            "resistance_ohms" to "Ω",
            "sampling_rate_hz" to "Hz",
            "battery_level" to "percentage",
            "signal_quality" to "unitless (0.0-1.0)"
        )
        
        override fun validateData(data: Map<String, Any>): ValidationResult {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            
            // Validate 12-bit ADC range
            val rawValue = data["gsr_raw_12bit"] as? Int
            if (rawValue != null && (rawValue < 0 || rawValue > 4095)) {
                errors.add("GSR raw value ($rawValue) must be within 12-bit range (0-4095)")
            }
            
            // Validate GSR microsiemens range
            val gsrValue = data["gsr_microsiemens"] as? Double
            if (gsrValue != null && (gsrValue < 0.1 || gsrValue > 100.0)) {
                warnings.add("GSR value ($gsrValue µS) is outside typical range (0.1-100.0 µS)")
            }
            
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }
    
    /**
     * Audio sensor schema definition
     */
    class AudioSchema : SensorSchema {
        override fun getRequiredColumns(): List<String> = listOf(
            MANDATORY_TIMESTAMP_COLUMN, "sample_rate_hz", "bit_depth",
            "channels", "duration_ms"
        )
        
        override fun getOptionalColumns(): List<String> = listOf(
            "volume_db", "quality_score", "noise_floor_db",
            "peak_frequency_hz", "rms_level"
        )
        
        override fun getFileExtensions(): List<String> = listOf("csv", "wav")
        
        override fun getUnits(): Map<String, String> = mapOf(
            MANDATORY_TIMESTAMP_COLUMN to "nanoseconds",
            "sample_rate_hz" to "Hz",
            "bit_depth" to "bits",
            "channels" to "count",
            "duration_ms" to "milliseconds",
            "volume_db" to "dB",
            "noise_floor_db" to "dB"
        )
        
        override fun validateData(data: Map<String, Any>): ValidationResult {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            
            // Validate sample rate
            val sampleRate = data["sample_rate_hz"] as? Int
            if (sampleRate != null && sampleRate < 8000) {
                warnings.add("Sample rate ($sampleRate Hz) is below recommended minimum (8000 Hz)")
            }
            
            // Validate bit depth
            val bitDepth = data["bit_depth"] as? Int
            if (bitDepth != null && bitDepth !in listOf(16, 24, 32)) {
                warnings.add("Bit depth ($bitDepth) is not a standard value (16, 24, or 32)")
            }
            
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }
    
    /**
     * Generate standardized file name
     */
    fun generateStandardFileName(
        sensorType: String,
        sessionId: String,
        extension: String,
        customTimestamp: Long? = null
    ): StandardFileName {
        val timestamp = customTimestamp ?: System.currentTimeMillis()
        val dateFormat = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault())
        val formattedTimestamp = dateFormat.format(Date(timestamp))
        
        val fileName = String.format(
            FILE_NAME_PATTERN,
            sensorType.lowercase(),
            formattedTimestamp,
            sessionId,
            extension
        )
        
        return StandardFileName(
            fileName = fileName,
            fullPath = fileName // Will be updated with full path when needed
        )
    }
    
    /**
     * Validate and potentially rename existing file to standard format
     */
    fun validateAndStandardizeFileName(filePath: String, sensorType: String, sessionId: String): StandardFileName? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "File does not exist: $filePath")
            return null
        }
        
        val fileName = file.name
        val extension = file.extension
        
        // Check if already follows standard format
        if (isStandardFormat(fileName, sensorType, sessionId)) {
            return StandardFileName(
                fileName = fileName,
                fullPath = filePath,
                isRenamed = false
            )
        }
        
        // Generate standard name
        val standardName = generateStandardFileName(sensorType, sessionId, extension)
        val newPath = File(file.parent, standardName.fileName).absolutePath
        
        return StandardFileName(
            fileName = standardName.fileName,
            fullPath = newPath,
            isRenamed = true,
            originalName = fileName
        )
    }
    
    /**
     * Check if file name follows standard format
     */
    private fun isStandardFormat(fileName: String, sensorType: String, sessionId: String): Boolean {
        val pattern = "${sensorType.lowercase()}_\\d{8}_\\d{6}_\\d{3}_${sessionId}\\.\\w+"
        return fileName.matches(Regex(pattern))
    }
    
    /**
     * Validate CSV schema for specific sensor type
     */
    fun validateCsvSchema(filePath: String, sensorType: String): ValidationResult {
        val schema = SENSOR_SCHEMAS[sensorType.lowercase()]
        if (schema == null) {
            return ValidationResult(false, listOf("Unknown sensor type: $sensorType"))
        }
        
        val file = File(filePath)
        if (!file.exists()) {
            return ValidationResult(false, listOf("File does not exist: $filePath"))
        }
        
        return try {
            val firstLine = file.bufferedReader().use { it.readLine() }
            if (firstLine == null) {
                return ValidationResult(false, listOf("CSV file is empty"))
            }
            
            // Validate header
            val header = firstLine.split(",").map { it.trim() }
            validateCsvHeader(header, schema)
            
        } catch (e: Exception) {
            ValidationResult(false, listOf("Error reading CSV file: ${e.message}"))
        }
    }
    
    /**
     * Validate CSV header against schema
     */
    private fun validateCsvHeader(header: List<String>, schema: SensorSchema): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        val requiredColumns = schema.getRequiredColumns()
        val optionalColumns = schema.getOptionalColumns()
        val allValidColumns = (requiredColumns + optionalColumns).toSet()
        
        // Check for mandatory timestamp column
        if (!header.contains(MANDATORY_TIMESTAMP_COLUMN)) {
            errors.add("Missing mandatory timestamp column: $MANDATORY_TIMESTAMP_COLUMN")
        }
        
        // Check for required columns
        for (requiredColumn in requiredColumns) {
            if (!header.contains(requiredColumn)) {
                errors.add("Missing required column: $requiredColumn")
            }
        }
        
        // Check for unknown columns
        for (column in header) {
            if (!allValidColumns.contains(column)) {
                warnings.add("Unknown column: $column")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    /**
     * Create session directory structure
     */
    fun createSessionDirectoryStructure(baseDir: File, sessionId: String): File {
        val sessionDir = File(baseDir, sessionId)
        
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
            Log.i(TAG, "Created session directory: ${sessionDir.absolutePath}")
        }
        
        // Create sensor subdirectories
        for (sensorDir in REQUIRED_DIRECTORIES) {
            val subDir = File(sessionDir, sensorDir)
            if (!subDir.exists()) {
                subDir.mkdirs()
                Log.d(TAG, "Created sensor subdirectory: ${subDir.absolutePath}")
            }
        }
        
        return sessionDir
    }
    
    /**
     * Validate session directory structure
     */
    fun validateSessionDirectoryStructure(sessionDir: File): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (!sessionDir.exists()) {
            return ValidationResult(false, listOf("Session directory does not exist: ${sessionDir.absolutePath}"))
        }
        
        if (!sessionDir.isDirectory) {
            return ValidationResult(false, listOf("Path is not a directory: ${sessionDir.absolutePath}"))
        }
        
        // Check for required subdirectories
        for (requiredDir in REQUIRED_DIRECTORIES) {
            val subDir = File(sessionDir, requiredDir)
            if (!subDir.exists()) {
                warnings.add("Missing subdirectory: $requiredDir")
            } else if (!subDir.isDirectory) {
                errors.add("Path is not a directory: ${subDir.absolutePath}")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    /**
     * Generate CSV header for sensor type
     */
    fun generateCsvHeader(sensorType: String, includeUnits: Boolean = true): String? {
        val schema = SENSOR_SCHEMAS[sensorType.lowercase()] ?: return null
        
        val columns = schema.getRequiredColumns() + schema.getOptionalColumns()
        
        return if (includeUnits) {
            val units = schema.getUnits()
            val headerWithUnits = columns.map { column ->
                val unit = units[column]
                if (unit != null) "$column ($unit)" else column
            }
            headerWithUnits.joinToString(",")
        } else {
            columns.joinToString(",")
        }
    }
    
    /**
     * Get schema documentation for sensor type
     */
    fun getSchemaDocumentation(sensorType: String): Map<String, Any>? {
        val schema = SENSOR_SCHEMAS[sensorType.lowercase()] ?: return null
        
        return mapOf(
            "sensor_type" to sensorType,
            "required_columns" to schema.getRequiredColumns(),
            "optional_columns" to schema.getOptionalColumns(),
            "file_extensions" to schema.getFileExtensions(),
            "units" to schema.getUnits(),
            "mandatory_timestamp_column" to MANDATORY_TIMESTAMP_COLUMN,
            "file_naming_pattern" to FILE_NAME_PATTERN
        )
    }
    
    /**
     * Get complete schema documentation for all sensors
     */
    fun getAllSchemaDocumentation(): Map<String, Any> {
        val allSchemas = mutableMapOf<String, Any>()
        
        for (sensorType in SENSOR_SCHEMAS.keys) {
            getSchemaDocumentation(sensorType)?.let { schema ->
                allSchemas[sensorType] = schema
            }
        }
        
        return mapOf(
            "schemas" to allSchemas,
            "global_requirements" to mapOf(
                "mandatory_timestamp_column" to MANDATORY_TIMESTAMP_COLUMN,
                "timestamp_format" to TIMESTAMP_FORMAT,
                "file_naming_pattern" to FILE_NAME_PATTERN,
                "required_directories" to REQUIRED_DIRECTORIES
            )
        )
    }
}