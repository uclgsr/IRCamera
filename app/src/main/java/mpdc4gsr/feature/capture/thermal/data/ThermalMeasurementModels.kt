package mpdc4gsr.feature.capture.thermal.data

import android.graphics.Point
import android.graphics.Rect

sealed class MeasurementArea {
    data class PointArea(val point: Point) : MeasurementArea()
    data class LineArea(val start: Point, val end: Point) : MeasurementArea()
    data class RectangleArea(val rect: Rect) : MeasurementArea()
    data class EllipseArea(val centerX: Int, val centerY: Int, val radiusX: Int, val radiusY: Int) : MeasurementArea()
    data class PolygonArea(val points: List<Point>) : MeasurementArea()
}

data class MeasurementResult(
    val minTemp: Float,
    val maxTemp: Float,
    val avgTemp: Float,
    val area: MeasurementArea
)

data class ThermalCalibrationData(
    val emissivity: Float = 0.95f,
    val distance: Float = 1.0f,
    val reflectedTemperature: Float = 20.0f,
    val ambientTemperature: Float = 25.0f,
    val humidity: Float = 50.0f
)

data class DeviceInfo(
    val model: String,
    val serialNumber: String,
    val firmwareVersion: String,
    val sdkVersion: String,
    val resolution: Pair<Int, Int>,
    val frameRate: Float,
    val temperatureRange: Pair<Float, Float>
)

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val voltage: Float
)

