package mpdc4gsr.feature.gsr.data

import java.io.Serializable

data class GSRPlotData(
    val timestamps: List<Double>,
    val gsrValues: List<Double>,
    val ppgValues: List<Double>,
    val gsrMovingAverage: List<Double>,
    val ppgMovingAverage: List<Double>,
    val gsrEvents: List<GSREvent>,
    val statistics: List<TimeWindowStats>,
    val metadata: PlotMetadata
) : Serializable

data class GSREvent(
    val timestamp: Double,
    val type: String,
    val magnitude: Double,
    val gsrValue: Double
) : Serializable

data class TimeWindowStats(
    val startTime: Double,
    val endTime: Double,
    val mean: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double,
    val count: Int
) : Serializable

data class PlotMetadata(
    val fileName: String,
    val duration: Double,
    val samplingRate: Double,
    val dataPoints: Int
) : Serializable