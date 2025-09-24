package mpdc4gsr.camera.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.controller.SensorStatusSummary
import mpdc4gsr.controller.DetailedSensorStatus

class RecordingStatusIndicator
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val statusIcon: ImageView
    private val statusText: TextView
    private val durationText: TextView
    private val sensorsText: TextView

    private var isRecording = false
    private var startTime = 0L
    private var sessionId = ""
    private var activeSensors = emptySet<SensorSelectionDialog.SensorType>()

    init {
        orientation = VERTICAL
        setPadding(16, 8, 16, 8)
        gravity = Gravity.CENTER

        statusIcon =
            ImageView(context).apply {
                layoutParams =
                    LayoutParams(32, 32).apply {
                        gravity = Gravity.CENTER
                        bottomMargin = 4
                    }

                setBackgroundColor(Color.LTGRAY)
            }
        addView(statusIcon)

        statusText =
            TextView(context).apply {
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                gravity = Gravity.CENTER
            }
        addView(statusText)

        durationText =
            TextView(context).apply {
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                gravity = Gravity.CENTER
            }
        addView(durationText)

        sensorsText =
            TextView(context).apply {
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, android.R.color.tertiary_text_dark))
                gravity = Gravity.CENTER
            }
        addView(sensorsText)

        updateDisplay()
    }

    fun startRecording(
        sessionId: String,
        sensors: Set<SensorSelectionDialog.SensorType>,
    ) {
        this.sessionId = sessionId
        this.activeSensors = sensors
        this.startTime = System.currentTimeMillis()
        this.isRecording = true

        updateDisplay()

        startDurationCounter()
    }

    fun stopRecording() {
        this.isRecording = false
        updateDisplay()
    }

    fun updateSensorStatus(
        sensor: SensorSelectionDialog.SensorType,
        status: String,
    ) {

        updateDisplay()
    }

    fun updateWithSensorSummary(summary: SensorStatusSummary) {

        if (summary.isSessionActive) {
            statusIcon.setBackgroundColor(Color.RED)
            statusText.text = "[REC]"
            statusText.setTextColor(Color.RED)

            val sensorDisplay = mutableListOf<String>()
            summary.sensors.forEach { sensorStatus: DetailedSensorStatus ->
                val icon =
                    when {
                        sensorStatus.sensorType.contains("RGB", ignoreCase = true) -> "[CAM]"
                        sensorStatus.sensorType.contains("Thermal", ignoreCase = true) -> "[THM]"
                        sensorStatus.sensorType.contains("GSR", ignoreCase = true) -> "[GSR]"
                        else -> "[SEN]"
                    }

                val statusIcon =
                    when {
                        sensorStatus.isRecording -> "[OK]"
                        sensorStatus.isInitialized -> "[RDY]"
                        else -> "[ERR]"
                    }

                sensorDisplay.add("$icon$statusIcon")
            }

            sensorsText.text = sensorDisplay.joinToString(" ")
            visibility = VISIBLE
        } else {
            statusIcon.setBackgroundColor(Color.GRAY)
            statusText.text =
                when {
                    summary.totalSensorsInitialized == 0 -> "[ERR] NO SENSORS"
                    summary.totalSensorsInitialized < summary.totalSensorsConfigured -> "[WARN] PARTIAL SETUP"
                    else -> "[RDY] READY"
                }
            statusText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            durationText.text = ""

            if (summary.totalSensorsInitialized > 0) {
                val sensorDisplay = mutableListOf<String>()
                summary.sensors.forEach { sensorStatus: DetailedSensorStatus ->
                    val icon =
                        when {
                            sensorStatus.sensorType.contains("RGB", ignoreCase = true) -> "[CAM]"
                            sensorStatus.sensorType.contains("Thermal", ignoreCase = true) -> "[THM]"
                            sensorStatus.sensorType.contains("GSR", ignoreCase = true) -> "[GSR]"
                            else -> "[SEN]"
                        }
                    sensorDisplay.add("$icon[OK]")
                }
                sensorsText.text = sensorDisplay.joinToString(" ") + " ready"
            } else {
                sensorsText.text = "Check sensor connections"
            }

            visibility = VISIBLE
        }
    }

    private fun updateDisplay() {
        if (isRecording) {
            statusIcon.setBackgroundColor(Color.RED)
            statusText.text = "[REC]"
            statusText.setTextColor(Color.RED)

            sensorsText.text =
                activeSensors.joinToString(" • ") {
                    when (it) {
                        SensorSelectionDialog.SensorType.THERMAL -> "[THM]"
                        SensorSelectionDialog.SensorType.RGB -> "[CAM]"
                        SensorSelectionDialog.SensorType.GSR -> "[GSR]"
                    }
                }

            visibility = VISIBLE
        } else {
            statusIcon.setBackgroundColor(Color.GRAY)
            statusText.text = "[STOP]"
            statusText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            durationText.text = ""
            sensorsText.text = ""

            visibility = GONE
        }
    }

    private fun startDurationCounter() {
        if (!isRecording) return

        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60

        durationText.text = String.format("%02d:%02d", minutes, seconds)

        postDelayed({ startDurationCounter() }, 1000)
    }

    fun setVisible(visible: Boolean) {
        visibility = if (visible) VISIBLE else GONE
    }
}
