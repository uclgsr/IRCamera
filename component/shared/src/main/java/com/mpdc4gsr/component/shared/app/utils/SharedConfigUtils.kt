package com.mpdc4gsr.component.shared.app.utils

import android.content.Context
import java.io.File

object SharedConfigUtils {
    data class ConfigSection(
        val name: String,
        val properties: MutableMap<String, String> = mutableMapOf(),
    ) {
        fun getString(
            key: String,
            defaultValue: String = "",
        ): String = properties[key] ?: defaultValue

        fun getInt(
            key: String,
            defaultValue: Int = 0,
        ): Int = properties[key]?.toIntOrNull() ?: defaultValue

        fun getBoolean(
            key: String,
            defaultValue: Boolean = false,
        ): Boolean = properties[key]?.toBooleanStrictOrNull() ?: defaultValue

        fun getFloat(
            key: String,
            defaultValue: Float = 0f,
        ): Float = properties[key]?.toFloatOrNull() ?: defaultValue
    }

    fun parseIniContent(content: String): Map<String, ConfigSection> {
        val sections = mutableMapOf<String, ConfigSection>()
        var currentSection: ConfigSection? = null
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith(";") -> {
                    // Skip empty lines and comments
                }

                trimmedLine.startsWith("[") && trimmedLine.endsWith("]") -> {
                    // Section header
                    val sectionName = trimmedLine.substring(1, trimmedLine.length - 1)
                    currentSection = ConfigSection(sectionName)
                    sections[sectionName] = currentSection
                }

                trimmedLine.contains("=") -> {
                    // Key-value pair
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        currentSection?.properties?.put(key, value)
                    }
                }
            }
        }
        return sections
    }

    fun readIniFromAssets(
        context: Context,
        fileName: String,
    ): Map<String, ConfigSection> =
        try {
            val inputStream = context.assets.open(fileName)
            val content = inputStream.bufferedReader().use { it.readText() }
            parseIniContent(content)
        } catch (e: Exception) {
            emptyMap()
        }

    fun readIniFromFile(file: File): Map<String, ConfigSection> =
        try {
            val content = file.readText()
            parseIniContent(content)
        } catch (e: Exception) {
            emptyMap()
        }

    fun writeIniToFile(
        file: File,
        sections: Map<String, ConfigSection>,
    ): Boolean =
        try {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                sections.values.forEach { section ->
                    writer.write("[${section.name}]\n")
                    section.properties.forEach { (key, value) ->
                        writer.write("$key=$value\n")
                    }
                    writer.write("\n")
                }
            }
            true
        } catch (e: Exception) {
            false
        }

    fun mergeSections(
        base: Map<String, ConfigSection>,
        overlay: Map<String, ConfigSection>,
    ): Map<String, ConfigSection> {
        val result = base.toMutableMap()
        overlay.forEach { (sectionName, overlaySection) ->
            val existingSection = result[sectionName]
            if (existingSection != null) {
                // Merge properties
                existingSection.properties.putAll(overlaySection.properties)
            } else {
                // Add new section
                result[sectionName] = overlaySection.copy()
            }
        }
        return result
    }

    data class ConfigValidationRule(
        val section: String,
        val key: String,
        val required: Boolean = false,
        val validator: (String) -> Boolean = { true },
    )

    fun validateConfiguration(
        config: Map<String, ConfigSection>,
        rules: List<ConfigValidationRule>,
    ): List<String> {
        val errors = mutableListOf<String>()
        rules.forEach { rule ->
            val section = config[rule.section]
            val value = section?.properties?.get(rule.key)
            when {
                rule.required && value == null -> {
                    errors.add("Required configuration missing: [${rule.section}] ${rule.key}")
                }

                value != null && !rule.validator(value) -> {
                    errors.add("Invalid configuration value: [${rule.section}] ${rule.key} = $value")
                }
            }
        }
        return errors
    }

    fun createDefaultAppConfig(): Map<String, ConfigSection> =
        mapOf(
            "app" to
                ConfigSection(
                    "app",
                    mutableMapOf(
                        "version" to "1.0.0",
                        "debug" to "false",
                        "log_level" to "INFO",
                    ),
                ),
            "camera" to
                ConfigSection(
                    "camera",
                    mutableMapOf(
                        "width" to "1920",
                        "height" to "1080",
                        "fps" to "30",
                        "format" to "JPEG",
                    ),
                ),
            "thermal" to
                ConfigSection(
                    "thermal",
                    mutableMapOf(
                        "emissivity" to "0.95",
                        "temperature_unit" to "CELSIUS",
                        "color_palette" to "RAINBOW",
                    ),
                ),
            "gsr" to
                ConfigSection(
                    "gsr",
                    mutableMapOf(
                        "sampling_rate" to "128",
                        "gain" to "1",
                        "range" to "GSR_RANGE_AUTO",
                    ),
                ),
            "network" to
                ConfigSection(
                    "network",
                    mutableMapOf(
                        "server_port" to "8080",
                        "timeout" to "5000",
                        "retry_count" to "3",
                    ),
                ),
        )

    fun getSystemConfig(context: Context): Map<String, String> =
        mapOf(
            "android_version" to android.os.Build.VERSION.RELEASE,
            "api_level" to
                android.os.Build.VERSION.SDK_INT
                    .toString(),
            "device_model" to android.os.Build.MODEL,
            "device_manufacturer" to android.os.Build.MANUFACTURER,
            "app_version" to SharedPackageUtils.getVersionName(context),
            "app_version_code" to SharedPackageUtils.getVersionCode(context).toString(),
            "package_name" to context.packageName,
            "is_debuggable" to SharedPackageUtils.isDebuggable(context).toString(),
        )

    enum class Environment {
        DEVELOPMENT,
        TESTING,
        PRODUCTION,
    }

    fun loadEnvironmentConfig(
        context: Context,
        environment: Environment = Environment.PRODUCTION,
    ): Map<String, ConfigSection> {
        val baseConfig = createDefaultAppConfig()
        val envConfigFile =
            when (environment) {
                Environment.DEVELOPMENT -> "config-dev.ini"
                Environment.TESTING -> "config-test.ini"
                Environment.PRODUCTION -> "config-prod.ini"
            }
        val envConfig = readIniFromAssets(context, envConfigFile)
        return mergeSections(baseConfig, envConfig)
    }

    fun backupConfiguration(
        context: Context,
        config: Map<String, ConfigSection>,
    ): Boolean {
        val backupFile = File(context.filesDir, "config_backup_${System.currentTimeMillis()}.ini")
        return writeIniToFile(backupFile, config)
    }

    fun restoreConfiguration(
        context: Context,
        backupFileName: String,
    ): Map<String, ConfigSection>? {
        val backupFile = File(context.filesDir, backupFileName)
        return if (backupFile.exists()) {
            readIniFromFile(backupFile)
        } else {
            null
        }
    }

    fun calculateConfigHash(config: Map<String, ConfigSection>): String {
        val content =
            buildString {
                config.values.sortedBy { it.name }.forEach { section ->
                    append("[${section.name}]")
                    section.properties.toSortedMap().forEach { (key, value) ->
                        append("$key=$value")
                    }
                }
            }
        return content.hashCode().toString()
    }

    fun hasConfigChanged(
        oldConfig: Map<String, ConfigSection>,
        newConfig: Map<String, ConfigSection>,
    ): Boolean = calculateConfigHash(oldConfig) != calculateConfigHash(newConfig)
}



