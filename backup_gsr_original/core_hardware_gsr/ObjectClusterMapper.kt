package mpdc4gsr.core.hardware.gsr

import android.os.SystemClock
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.FormatCluster
import com.shimmerresearch.driver.ObjectCluster
import mpdc4gsr.core.hardware.gsr.model.GSRSample
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class GsrSamplePayload(
    val sample: GSRSample,
    val wallClockMs: Long,
)

private val isoFormatter: ThreadLocal<SimpleDateFormat> =
    ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

fun ObjectCluster.toGsrSamplePayload(): GsrSamplePayload? {
    return runCatching {
        val timestampFormats =
            getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP)
        val timestampCluster =
            ObjectCluster.returnFormatCluster(timestampFormats, "CAL") as? FormatCluster
        val wallClockMs = timestampCluster?.mData?.toLong() ?: System.currentTimeMillis()
        val isoTimestamp = isoFormatter.get().format(Date(wallClockMs))
        val monotonicNs = SystemClock.elapsedRealtimeNanos()

        val gsrFormats =
            getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE)
        val gsrCalCluster = ObjectCluster.returnFormatCluster(gsrFormats, "CAL") as? FormatCluster
        val gsrCalibrated = gsrCalCluster?.mData ?: return null
        val gsrRawCluster = ObjectCluster.returnFormatCluster(gsrFormats, "RAW") as? FormatCluster
        val gsrRaw =
            gsrRawCluster?.mData?.roundToInt()
                ?: (gsrCalibrated * (4095.0 / 100.0)).roundToInt().coerceIn(0, 4095)

        val ppgFormats =
            getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.INT_EXP_ADC_A13)
        val ppgRawCluster = ObjectCluster.returnFormatCluster(ppgFormats, "RAW") as? FormatCluster
        val ppgRaw = ppgRawCluster?.mData?.roundToInt() ?: 0

        val rssi =
            runCatching {
                val field = ObjectCluster::class.java.getField("mRssi")
                (field.get(this) as? Number)?.toInt()
            }.getOrNull() ?: -50

        val sample =
            GSRSample.fromRawData(
                timestamp = monotonicNs,
                timestampIso = isoTimestamp,
                gsrCalibratedValue = gsrCalibrated,
                gsrRawValue = gsrRaw,
                ppgRawValue = ppgRaw,
                connectionRssi = rssi,
            )
        GsrSamplePayload(sample, wallClockMs)
    }.getOrNull()
}
