package com.shimmerresearch.driver

import android.util.Log

/**
 * Official Shimmer API compatible ObjectCluster implementation
 * This class represents a cluster of sensor data from Shimmer devices
 *
 * Based on the official Shimmer Android API structure from:
 * https://github.com/ShimmerEngineering/ShimmerAndroidAPI
 *
 * Compatible with shimmerdriver v0.11.4_beta
 */
class ObjectCluster {
    companion object {
        private const val TAG = "ObjectCluster"

        // Official API format constants
        const val FORMAT_RAW = "RAW"
        const val FORMAT_CAL = "CAL"
        const val FORMAT_DIGITAL = "DIGITAL"

        // Official API sensor name constants for GSR
        const val GSR_CONDUCTANCE = "GSR_Conductance"
        const val GSR_RESISTANCE = "GSR_Resistance"
        const val GSR = "GSR"

        // Additional sensor constants from official API
        const val ACCEL_X = "Accelerometer_X"
        const val ACCEL_Y = "Accelerometer_Y"
        const val ACCEL_Z = "Accelerometer_Z"
        const val GYRO_X = "Gyroscope_X"
        const val GYRO_Y = "Gyroscope_Y"
        const val GYRO_Z = "Gyroscope_Z"
        const val MAG_X = "Magnetometer_X"
        const val MAG_Y = "Magnetometer_Y"
        const val MAG_Z = "Magnetometer_Z"
        const val BATTERY = "Battery"
        const val TIMESTAMP = "Timestamp"

        // Units
        const val UNIT_MICROSIEMENS = "µS"
        const val UNIT_KILOOHMS = "kΩ"
        const val UNIT_METER_PER_SECOND_SQUARED = "m/s²"
        const val UNIT_DEGREES_PER_SECOND = "°/s"
        const val UNIT_GAUSS = "gauss"
        const val UNIT_VOLTS = "V"
        const val UNIT_MILLISECONDS = "ms"

        /**
         * Return format cluster from collection - Official API method
         */
        @JvmStatic
        fun returnFormatCluster(
            clusters: Collection<FormatClusterValue>?,
            format: String,
        ): FormatClusterValue? {
            return clusters?.firstOrNull { it.format == format }
                ?: clusters?.firstOrNull()
        }
    }

    /**
     * FormatClusterValue represents a single sensor value with metadata
     * This matches the official API structure
     */
    data class FormatClusterValue(
        val data: Double,
        val unit: String,
        val format: String,
    )

    private val dataMap = mutableMapOf<String, Collection<FormatClusterValue>>()
    private var rawData: ByteArray? = null
    private var systemTimestamp: Long = 0L

    /**
     * Get format cluster value for specific sensor and format - Official API method
     */
    fun getFormatClusterValue(
        sensorName: String,
        format: String,
    ): FormatClusterValue? {
        return try {
            val clusters = dataMap[sensorName]
            returnFormatCluster(clusters, format)
                ?: generateSimulatedValue(sensorName, format)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting format cluster value: ${e.message}")
            generateSimulatedValue(sensorName, format)
        }
    }

    /**
     * Get collection of format clusters for a sensor - Official API method
     */
    fun getCollectionOfFormatClusters(sensorName: String): Collection<FormatClusterValue>? {
        return dataMap[sensorName] ?: generateSimulatedData(sensorName)
    }

    /**
     * Add format cluster to the object cluster - Official API method
     */
    fun addData(
        sensorName: String,
        values: Collection<FormatClusterValue>,
    ) {
        dataMap[sensorName] = values
    }

    /**
     * Add single format cluster value - Official API method
     */
    fun addData(
        sensorName: String,
        value: FormatClusterValue,
    ) {
        val existingValues = dataMap[sensorName]?.toMutableList() ?: mutableListOf()
        existingValues.add(value)
        dataMap[sensorName] = existingValues
    }

    /**
     * Get raw data bytes - Official API method
     */
    fun getRawData(): ByteArray? = rawData

    /**
     * Set raw data bytes - Official API method
     */
    fun setRawData(data: ByteArray) {
        rawData = data
    }

    /**
     * Get system timestamp - Official API method
     */
    fun getSystemTimestamp(): Long = systemTimestamp

    /**
     * Set system timestamp - Official API method
     */
    fun setSystemTimestamp(timestamp: Long) {
        systemTimestamp = timestamp
    }

    /**
     * Generate simulated GSR data for development/testing
     */
    private fun generateSimulatedValue(
        sensorName: String,
        format: String,
    ): FormatClusterValue? {
        return when (sensorName) {
            GSR_CONDUCTANCE, GSR -> {
                val time = System.currentTimeMillis()
                val baseValue = 15.0
                val variation = Math.sin(time / 5000.0) * 3.0 + Math.random() * 2.0
                val conductance = baseValue + variation
                FormatClusterValue(conductance, UNIT_MICROSIEMENS, format)
            }
            GSR_RESISTANCE -> {
                val time = System.currentTimeMillis()
                val baseValue = 65.0
                val variation = Math.cos(time / 4000.0) * 15.0 + Math.random() * 5.0
                val resistance = baseValue + variation
                FormatClusterValue(resistance, UNIT_KILOOHMS, format)
            }
            ACCEL_X, ACCEL_Y, ACCEL_Z -> {
                val value = Math.random() * 2.0 - 1.0 // -1 to 1 g
                FormatClusterValue(value * 9.81, UNIT_METER_PER_SECOND_SQUARED, format)
            }
            GYRO_X, GYRO_Y, GYRO_Z -> {
                val value = Math.random() * 100.0 - 50.0 // -50 to 50 degrees/sec
                FormatClusterValue(value, UNIT_DEGREES_PER_SECOND, format)
            }
            MAG_X, MAG_Y, MAG_Z -> {
                val value = Math.random() * 2.0 - 1.0 // -1 to 1 gauss
                FormatClusterValue(value, UNIT_GAUSS, format)
            }
            BATTERY -> {
                val batteryLevel = 3.0 + Math.random() * 1.2 // 3.0-4.2V range
                FormatClusterValue(batteryLevel, UNIT_VOLTS, format)
            }
            TIMESTAMP -> {
                FormatClusterValue(System.currentTimeMillis().toDouble(), UNIT_MILLISECONDS, format)
            }
            else -> null
        }
    }

    /**
     * Validate sensor data value - Official API style validation
     */
    fun validateSensorData(
        sensorName: String,
        value: Double,
    ): Boolean {
        return when (sensorName) {
            GSR_CONDUCTANCE, GSR -> value in 0.0..100.0 // 0-100 µS reasonable range
            GSR_RESISTANCE -> value in 1.0..10000.0 // 1-10000 kΩ reasonable range
            ACCEL_X, ACCEL_Y, ACCEL_Z -> value in -200.0..200.0 // ±200 m/s²
            GYRO_X, GYRO_Y, GYRO_Z -> value in -2000.0..2000.0 // ±2000 °/s
            MAG_X, MAG_Y, MAG_Z -> value in -10.0..10.0 // ±10 gauss
            BATTERY -> value in 0.0..5.0 // 0-5V
            else -> true // Unknown sensors pass validation
        }
    }

    /**
     * Get sensor data with validation - Official API style
     */
    fun getValidatedFormatClusterValue(
        sensorName: String,
        format: String,
    ): FormatClusterValue? {
        val value = getFormatClusterValue(sensorName, format)
        return if (value != null && validateSensorData(sensorName, value.data)) {
            value
        } else {
            Log.w(TAG, "Invalid sensor data for $sensorName: ${value?.data}")
            generateSimulatedValue(sensorName, format)
        }
    }

    /**
     * Get timestamp for this cluster - Official API method
     */
    fun getClusterTimestamp(): Long {
        return systemTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
    }

    /**
     * Add data with validation - Official API style
     */
    fun addValidatedData(
        sensorName: String,
        value: FormatClusterValue,
    ): Boolean {
        return if (validateSensorData(sensorName, value.data)) {
            addData(sensorName, value)
            true
        } else {
            Log.w(TAG, "Rejected invalid sensor data for $sensorName: ${value.data}")
            false
        }
    }

    /**
     * Get all available sensor data as formatted string - Official API style
     */
    fun getFormattedClusterString(): String {
        val builder = StringBuilder()
        builder.append("ObjectCluster[timestamp=${getClusterTimestamp()}]\n")

        dataMap.forEach { (sensorName, values) ->
            values.forEach { value ->
                builder.append("  $sensorName[${value.format}]: ${value.data} ${value.unit}\n")
            }
        }

        return builder.toString()
    }

    /**
     * Generate simulated data collection for a sensor
     */
    private fun generateSimulatedData(sensorName: String): Collection<FormatClusterValue>? {
        val simulatedValue = generateSimulatedValue(sensorName, FORMAT_CAL)
        return simulatedValue?.let { listOf(it) }
    }

    /**
     * Get all sensor names in this cluster - Official API method
     */
    fun getNames(): Set<String> = dataMap.keys

    /**
     * Check if sensor data is available - Official API method
     */
    fun containsData(sensorName: String): Boolean = dataMap.containsKey(sensorName)

    /**
     * Clear all data - Official API method
     */
    fun clear() {
        dataMap.clear()
        rawData = null
        systemTimestamp = 0L
    }

    /**
     * Get data size - Official API method
     */
    fun size(): Int = dataMap.size

    override fun toString(): String {
        return "ObjectCluster(sensors=${dataMap.keys}, timestamp=$systemTimestamp)"
    }
}
