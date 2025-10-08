package mpdc4gsr.core.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileSchemaManager {
    companion object {
        private const val FILE_NAME_PATTERN = "%s_%s_%s.%s"
        private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss_SSS"
        private const val MANDATORY_TIMESTAMP_COLUMN = "timestamp_ns"
        private val REQUIRED_DIRECTORIES = listOf(
            "thermal", "rgb", "gsr", "audio", "metadata"
        )
        private val SENSOR_SCHEMAS = mapOf(
            "thermal" to ThermalSchema(),
            "rgb" to RgbSchema(),
            "gsr" to GsrSchema(),
            "audio" to AudioSchema()
        )
    }

    interface SensorSchema {
        fun getRequiredColumns(): List<String>
        fun getOptionalColumns(): List<String>
        fun getFileExtensions(): List<String>
        fun validateData(data: Map<String, Any>): ValidationResult
        fun getUnits(): Map<String, String>
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    data class StandardFileName(
        val fileName: String,
        val fullPath: String,
        val isRenamed: Boolean = false,
        val originalName: String? = null
    )

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
            val emissivity = data["emissivity"] as? Double
            if (emissivity != null && (emissivity < 0.0 || emissivity > 1.0)) {
                errors.add("Emissivity ($emissivity) must be between 0.0 and 1.0")
            }
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }

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
            val frameRate = data["frame_rate_fps"] as? Double
            if (frameRate != null && frameRate < 1.0) {
                errors.add("Frame rate ($frameRate) must be at least 1 FPS")
            }
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
            val rawValue = data["gsr_raw_12bit"] as? Int
            if (rawValue != null && (rawValue < 0 || rawValue > 4095)) {
                errors.add("GSR raw value ($rawValue) must be within 12-bit range (0-4095)")
            }
            val gsrValue = data["gsr_microsiemens"] as? Double
            if (gsrValue != null && (gsrValue < 0.1 || gsrValue > 100.0)) {
                warnings.add("GSR value ($gsrValue µS) is outside typical range (0.1-100.0 µS)")
            }
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }

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
            val sampleRate = data["sample_rate_hz"] as? Int
            if (sampleRate != null && sampleRate < 8000) {
                warnings.add("Sample rate ($sampleRate Hz) is below recommended minimum (8000 Hz)")
            }
            val bitDepth = data["bit_depth"] as? Int
            if (bitDepth != null && bitDepth !in listOf(16, 24, 32)) {
                warnings.add("Bit depth ($bitDepth) is not a standard value (16, 24, or 32)")
            }
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }
    }

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
            fullPath = fileName
        )
    }

    fun validateAndStandardizeFileName(
        filePath: String,
        sensorType: String,
        sessionId: String
    ): StandardFileName? {
        val file = File(filePath)
        if (!file.exists()) {
            return null
        }
        val fileName = file.name
        val extension = file.extension
        if (isStandardFormat(fileName, sensorType, sessionId)) {
            return StandardFileName(
                fileName = fileName,
                fullPath = filePath,
                isRenamed = false
            )
        }
        val standardName = generateStandardFileName(sensorType, sessionId, extension)
        val newPath = File(file.parent, standardName.fileName).absolutePath
        return StandardFileName(
            fileName = standardName.fileName,
            fullPath = newPath,
            isRenamed = true,
            originalName = fileName
        )
    }

    private fun isStandardFormat(fileName: String, sensorType: String, sessionId: String): Boolean {
        val pattern = "${sensorType.lowercase()}_\\d{8}_\\d{6}_\\d{3}_${sessionId}\\.\\w+"
        return fileName.matches(Regex(pattern))
    }

    fun validateCsvSchema(filePath: String, sensorType: String): ValidationResult {
        val schema = SENSOR_SCHEMAS[sensorType.lowercase()]
        if (schema == null) {
            return ValidationResult(false, listOf("Unknown sensor type: $sensorType"))
        }
        val file = File(filePath)
        if (!file.exists()) {
            return ValidationResult(false, listOf("File does not exist: $filePath"))
        }
        return (
            val firstLine = file.bufferedReader().use { it.readLine() }
            if (firstLine == null) {
                return ValidationResult(false, listOf("CSV file is empty"))
            }
            val header = firstLine.split(",").map { it.trim() }
            validateCsvHeader(header, schema)
        }
    }

    private fun validateCsvHeader(header: List<String>, schema: SensorSchema): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val requiredColumns = schema.getRequiredColumns()
        val optionalColumns = schema.getOptionalColumns()
        val allValidColumns = (requiredColumns + optionalColumns).toSet()
        if (!header.contains(MANDATORY_TIMESTAMP_COLUMN)) {
            errors.add("Missing mandatory timestamp column: $MANDATORY_TIMESTAMP_COLUMN")
        }
        for (requiredColumn in requiredColumns) {
            if (!header.contains(requiredColumn)) {
                errors.add("Missing required column: $requiredColumn")
            }
        }
        for (column in header) {
            if (!allValidColumns.contains(column)) {
                warnings.add("Unknown column: $column")
            }
        }
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    fun createSessionDirectoryStructure(baseDir: File, sessionId: String): File {
        val sessionDir = File(baseDir, sessionId)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        for (sensorDir in REQUIRED_DIRECTORIES) {
            val subDir = File(sessionDir, sensorDir)
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
        }
        return sessionDir
    }

    fun validateSessionDirectoryStructure(sessionDir: File): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (!sessionDir.exists()) {
            return ValidationResult(
                false,
                listOf("Session directory does not exist: ${sessionDir.absolutePath}")
            )
        }
        if (!sessionDir.isDirectory) {
            return ValidationResult(
                false,
                listOf("Path is not a directory: ${sessionDir.absolutePath}")
            )
        }
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