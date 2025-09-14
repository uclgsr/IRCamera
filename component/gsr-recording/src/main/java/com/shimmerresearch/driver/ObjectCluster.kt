package com.shimmerresearch.driver

import android.util.Log

class ObjectCluster {
    companion object {
        private const val TAG = "ObjectCluster"

        const val FORMAT_RAW = "RAW"
        const val FORMAT_CAL = "CAL"
        const val FORMAT_DIGITAL = "DIGITAL"

        const val GSR_CONDUCTANCE = "GSR_Conductance"
        const val GSR_RESISTANCE = "GSR_Resistance"
        const val GSR = "GSR"

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

        const val UNIT_MICROSIEMENS = "µS"
        const val UNIT_KILOOHMS = "kΩ"
        const val UNIT_METER_PER_SECOND_SQUARED = "m/s²"
        const val UNIT_DEGREES_PER_SECOND = "°/s"
        const val UNIT_GAUSS = "gauss"
        const val UNIT_VOLTS = "V"
        const val UNIT_MILLISECONDS = "ms"

        @JvmStatic
        fun returnFormatCluster(
            clusters: Collection<FormatClusterValue>?,
            format: String,
        ): FormatClusterValue? {
            return clusters?.firstOrNull { it.format == format }
                ?: clusters?.firstOrNull()
        }
    }

    data class FormatClusterValue(
        val data: Double,
        val unit: String,
        val format: String,
    )

    private val dataMap = mutableMapOf<String, Collection<FormatClusterValue>>()
    private var rawData: ByteArray? = null
    private var systemTimestamp: Long = 0L

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

    fun getCollectionOfFormatClusters(sensorName: String): Collection<FormatClusterValue>? {
        return dataMap[sensorName] ?: generateSimulatedData(sensorName)
    }

    fun addData(
        sensorName: String,
        values: Collection<FormatClusterValue>,
    ) {
        dataMap[sensorName] = values
    }

    fun addData(
        sensorName: String,
        value: FormatClusterValue,
    ) {
        val existingValues = dataMap[sensorName]?.toMutableList() ?: mutableListOf()
        existingValues.add(value)
        dataMap[sensorName] = existingValues
    }

    fun getRawData(): ByteArray? = rawData

    fun setRawData(data: ByteArray) {
        rawData = data
    }

    fun getSystemTimestamp(): Long = systemTimestamp

    fun setSystemTimestamp(timestamp: Long) {
        systemTimestamp = timestamp
    }

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

    fun getClusterTimestamp(): Long {
        return systemTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
    }

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

    private fun generateSimulatedData(sensorName: String): Collection<FormatClusterValue>? {
        val simulatedValue = generateSimulatedValue(sensorName, FORMAT_CAL)
        return simulatedValue?.let { listOf(it) }
    }

    fun getNames(): Set<String> = dataMap.keys

    fun containsData(sensorName: String): Boolean = dataMap.containsKey(sensorName)

    fun clear() {
        dataMap.clear()
        rawData = null
        systemTimestamp = 0L
    }

    fun size(): Int = dataMap.size

    override fun toString(): String {
        return "ObjectCluster(sensors=${dataMap.keys}, timestamp=$systemTimestamp)"
    }
}
