package mpdc4gsr.ui_components

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced Comprehensive Sensor Status Dashboard Fragment
 * Implements TODO requirement: "Provide clear UI indicators for each sensor's status
 * (connected, streaming, error). Connection status indicators for each sensor."
 *
 * This fragment is droppable and all views are scrollable.
 */
class SensorDashboardFragment : Fragment() {

    companion object {
        private const val TAG = "SensorDashboardFragment"

        private const val COLOR_CONNECTED = Color.GREEN
        private const val COLOR_STREAMING = Color.BLUE
        private const val COLOR_ERROR = Color.RED
        private const val COLOR_DISCONNECTED = Color.GRAY
        private const val COLOR_SIMULATION = Color.YELLOW

        fun newInstance(): SensorDashboardFragment {
            return SensorDashboardFragment()
        }

        // Helper method to get fragment instance from fragment manager
        fun getInstance(fragmentManager: androidx.fragment.app.FragmentManager): SensorDashboardFragment? {
            return fragmentManager.findFragmentByTag("sensor_dashboard") as? SensorDashboardFragment
        }
    }

    // UI Components
    private lateinit var titleText: TextView
    private lateinit var overallStatusText: TextView
    private lateinit var recordingIndicator: ImageView
    private lateinit var recordingTimer: TextView
    private lateinit var sensorsTitleContainer: LinearLayout
    private lateinit var sensorsTitle: TextView
    private lateinit var collapseExpandIcon: ImageView
    private lateinit var sensorsContainer: LinearLayout

    // State tracking
    private val sensorStatusViews = mutableMapOf<String, SensorStatusView>()
    private var isRecording = false
    private var recordingStartTime = 0L
    private var timerUpdateJob: Job? = null
    private var isCollapsed = false

    private var currentSessionId: String? = null
    private var activeSensorCount = 0
    private var errorSensorCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensor_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        initializeDefaultSensors()
        
        // Restore collapsed state if needed
        savedInstanceState?.getBoolean("isCollapsed", false)?.let { collapsed ->
            if (collapsed) {
                // Post to ensure views are measured first
                view.post {
                    isCollapsed = false // Set to false first so toggleSensorsCollapse() works correctly
                    toggleSensorsCollapse()
                }
            }
        }
    }

    private fun setupUI(view: View) {
        titleText = view.findViewById(R.id.titleText)
        overallStatusText = view.findViewById(R.id.overallStatusText)
        recordingIndicator = view.findViewById(R.id.recordingIndicator)
        recordingTimer = view.findViewById(R.id.recordingTimer)
        sensorsTitleContainer = view.findViewById(R.id.sensorsTitleContainer)
        sensorsTitle = view.findViewById(R.id.sensorsTitle)
        collapseExpandIcon = view.findViewById(R.id.collapseExpandIcon)
        sensorsContainer = view.findViewById(R.id.sensorsContainer)

        // Set initial states
        overallStatusText.setTextColor(COLOR_DISCONNECTED)
        recordingIndicator.setBackgroundColor(COLOR_DISCONNECTED)
        
        // Set up collapse/expand click listener
        sensorsTitleContainer.setOnClickListener {
            toggleSensorsCollapse()
        }
    }

    /**
     * Toggle the collapse/expand state of the sensors container with smooth animation
     */
    private fun toggleSensorsCollapse() {
        isCollapsed = !isCollapsed
        
        if (isCollapsed) {
            collapseSensors()
        } else {
            expandSensors()
        }
    }

    /**
     * Collapse the sensors container with animation
     */
    private fun collapseSensors() {
        val initialHeight = sensorsContainer.height
        
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = sensorsContainer.layoutParams
            layoutParams.height = value
            sensorsContainer.layoutParams = layoutParams
        }
        
        // Rotate arrow icon to point right (collapsed state)
        val iconRotation = ObjectAnimator.ofFloat(collapseExpandIcon, "rotation", 0f, -90f)
        iconRotation.duration = 300
        iconRotation.interpolator = AccelerateDecelerateInterpolator()
        
        animator.start()
        iconRotation.start()
    }

    /**
     * Expand the sensors container with animation
     */
    private fun expandSensors() {
        // Measure the height needed for full content
        sensorsContainer.measure(
            View.MeasureSpec.makeMeasureSpec(sensorsContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = sensorsContainer.measuredHeight
        
        // Fallback to wrap content if measurement fails
        val finalTargetHeight = if (targetHeight > 0) targetHeight else ViewGroup.LayoutParams.WRAP_CONTENT
        
        val animator = if (finalTargetHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
            // If we can't measure, just reset the height immediately
            sensorsContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            sensorsContainer.requestLayout()
            null
        } else {
            ValueAnimator.ofInt(0, finalTargetHeight).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    val layoutParams = sensorsContainer.layoutParams
                    layoutParams.height = value
                    sensorsContainer.layoutParams = layoutParams
                }
            }
        }
        
        // Rotate arrow icon to point down (expanded state)
        val iconRotation = ObjectAnimator.ofFloat(collapseExpandIcon, "rotation", -90f, 0f)
        iconRotation.duration = 300
        iconRotation.interpolator = AccelerateDecelerateInterpolator()
        
        animator?.start()
        iconRotation.start()
    }

    private fun initializeDefaultSensors() {
        addSensorStatusView("thermal_camera", "TC001 Thermal Camera", SensorType.THERMAL)
        addSensorStatusView("rgb_camera", "RGB Camera", SensorType.RGB)
        addSensorStatusView("shimmer_gsr", "Shimmer GSR Sensor", SensorType.GSR)
        addSensorStatusView("audio_recorder", "Audio Recorder", SensorType.AUDIO)
    }

    private fun addSensorStatusView(sensorId: String, displayName: String, type: SensorType) {
        val statusView = SensorStatusView(requireContext(), sensorId, displayName, type)
        sensorStatusViews[sensorId] = statusView
        sensorsContainer.addView(statusView)
    }

    /**
     * Update sensor status with clear visual indicators
     * Implements TODO requirement: "sensor icon or label turns green when connected and red or grey if disconnected"
     */
    fun updateSensorStatus(sensorId: String, status: SensorStatus, message: String? = null) {
        sensorStatusViews[sensorId]?.updateStatus(status, message)
        updateOverallStatus()
    }

    /**
     * Update recording status with prominent indicator
     * Implements TODO requirement: "When a session is recording, the user should see a prominent indicator
     * (e.g. a red "Recording" dot and timer)"
     */
    fun updateRecordingStatus(recording: Boolean, sessionId: String? = null) {
        isRecording = recording
        currentSessionId = sessionId

        if (recording) {
            recordingStartTime = System.currentTimeMillis()
            recordingIndicator.setBackgroundColor(COLOR_ERROR)
            startRecordingTimer()
        } else {
            recordingIndicator.setBackgroundColor(COLOR_DISCONNECTED)
            recordingTimer.text = "Ready to Record"
            stopRecordingTimer()
        }

        updateOverallStatus()
    }

    /**
     * Show simulation mode warning
     * Implements TODO requirement: "indicate if the thermal camera is in simulation mode vs real
     * (perhaps a warning icon or text if simulation is active) so users know the data source"
     */
    fun showSimulationWarning(sensorId: String, isSimulation: Boolean) {
        sensorStatusViews[sensorId]?.showSimulationMode(isSimulation)
    }

    /**
     * Display error notification to user
     * Implements TODO requirement: "Surface any sensor errors to the user in real time"
     */
    fun showSensorError(sensorId: String, errorMessage: String) {
        sensorStatusViews[sensorId]?.showError(errorMessage)
        Toast.makeText(requireContext(), "$sensorId: $errorMessage", Toast.LENGTH_LONG).show()
    }

    fun updateMultiDeviceStatus(connectedCount: Int, streamingCount: Int, maxDevices: Int) {
        val shimmerStatusView = sensorStatusViews["shimmer_gsr"]
        shimmerStatusView?.updateMultiDeviceInfo(connectedCount, streamingCount, maxDevices)
    }

    /**
     * Programmatically set the collapsed state of the sensors container
     */
    fun setSensorsCollapsed(collapsed: Boolean) {
        if (isCollapsed != collapsed) {
            toggleSensorsCollapse()
        }
    }

    /**
     * Get the current collapsed state of the sensors container
     */
    fun isSensorsCollapsed(): Boolean = isCollapsed

    private fun updateOverallStatus() {
        activeSensorCount = sensorStatusViews.values.count { it.isConnected() }
        errorSensorCount = sensorStatusViews.values.count { it.hasError() }

        val statusText = when {
            isRecording -> "RECORDING - Session: ${currentSessionId ?: "Unknown"}"
            errorSensorCount > 0 -> "${errorSensorCount} Sensor Error(s) Detected"
            activeSensorCount == 0 -> "No Sensors Connected"
            activeSensorCount < sensorStatusViews.size -> "${activeSensorCount}/${sensorStatusViews.size} Sensors Connected"
            else -> "All Sensors Connected & Ready"
        }

        val statusColor = when {
            isRecording -> COLOR_ERROR
            errorSensorCount > 0 -> COLOR_ERROR
            activeSensorCount == 0 -> COLOR_DISCONNECTED
            activeSensorCount < sensorStatusViews.size -> COLOR_SIMULATION
            else -> COLOR_CONNECTED
        }

        overallStatusText.text = statusText
        overallStatusText.setTextColor(statusColor)
    }

    private fun startRecordingTimer() {
        timerUpdateJob?.cancel()
        timerUpdateJob = lifecycleScope.launch {
            while (isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val formattedTime = formatElapsedTime(elapsed)
                recordingTimer.text = "RECORDING: $formattedTime"
                // Calculate delay to next second boundary to minimize drift
                val delayToNextSecond = 1000 - (elapsed % 1000)
                delay(delayToNextSecond)
            }
        }
    }

    private fun stopRecordingTimer() {
        timerUpdateJob?.cancel()
    }

    private fun formatElapsedTime(elapsedMs: Long): String {
        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / (1000 * 60)) % 60
        val hours = (elapsedMs / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isCollapsed", isCollapsed)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecordingTimer()
    }

    enum class SensorType {
        THERMAL, RGB, GSR, AUDIO
    }

    enum class SensorStatus {
        DISCONNECTED, CONNECTING, CONNECTED, STREAMING, ERROR, SIMULATION
    }

    private class SensorStatusView(
        context: Context,
        private val sensorId: String,
        private val displayName: String,
        private val sensorType: SensorType
    ) : LinearLayout(context) {

        private val statusIcon: ImageView
        private val nameText: TextView
        private val statusText: TextView
        private val detailsText: TextView

        private var currentStatus = SensorStatus.DISCONNECTED
        private var hasErrorState = false
        private var isSimulationMode = false

        init {
            orientation = HORIZONTAL
            setPadding(8, 4, 8, 4)

            statusIcon = ImageView(context).apply {
                layoutParams = LayoutParams(20, 20).apply {
                    marginEnd = 12
                    gravity = Gravity.CENTER_VERTICAL
                }
                setBackgroundColor(COLOR_DISCONNECTED)
            }
            addView(statusIcon)

            val textContainer = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }

            nameText = TextView(context).apply {
                text = displayName
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
            }
            textContainer.addView(nameText)

            statusText = TextView(context).apply {
                text = "Disconnected"
                textSize = 12f
                setTextColor(COLOR_DISCONNECTED)
            }
            textContainer.addView(statusText)

            detailsText = TextView(context).apply {
                text = ""
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_dark))
                visibility = GONE
            }
            textContainer.addView(detailsText)

            addView(textContainer)
        }

        fun updateStatus(status: SensorStatus, message: String? = null) {
            currentStatus = status
            hasErrorState = (status == SensorStatus.ERROR)

            val (statusColor, statusString) = when (status) {
                SensorStatus.DISCONNECTED -> COLOR_DISCONNECTED to "Disconnected"
                SensorStatus.CONNECTING -> COLOR_SIMULATION to "Connecting..."
                SensorStatus.CONNECTED -> COLOR_CONNECTED to "Connected"
                SensorStatus.STREAMING -> COLOR_STREAMING to "Streaming"
                SensorStatus.ERROR -> COLOR_ERROR to "Error"
                SensorStatus.SIMULATION -> COLOR_SIMULATION to "Simulation Mode"
            }

            statusIcon.setBackgroundColor(statusColor)
            statusText.text = statusString
            statusText.setTextColor(statusColor)

            if (message != null) {
                detailsText.text = message
                detailsText.visibility = VISIBLE
            } else {
                detailsText.visibility = GONE
            }
        }

        fun showSimulationMode(isSimulation: Boolean) {
            if (isSimulation) {
                isSimulationMode = true
                detailsText.text = "Using simulated data - no hardware detected"
                detailsText.setTextColor(COLOR_SIMULATION)
                detailsText.visibility = VISIBLE
            } else {
                // Only hide the text if it was previously in simulation mode.
                if (isSimulationMode) {
                    detailsText.visibility = GONE
                }
                isSimulationMode = false
            }
        }

        fun showError(errorMessage: String) {
            updateStatus(SensorStatus.ERROR, errorMessage)
        }

        fun updateMultiDeviceInfo(connectedCount: Int, streamingCount: Int, maxDevices: Int) {
            if (sensorType == SensorType.GSR) {
                detailsText.text =
                    "Multi-device: $connectedCount/$maxDevices connected, $streamingCount streaming"
                detailsText.visibility = VISIBLE
            }
        }

        fun isConnected(): Boolean =
            currentStatus in listOf(SensorStatus.CONNECTED, SensorStatus.STREAMING)

        fun hasError(): Boolean = hasErrorState
    }
}