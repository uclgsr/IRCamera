package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrSettingsBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity

/**
 * GSR Recording Settings Activity
 * Configure recording parameters, device settings, and data collection options
 */
class GSRSettingsActivity : BaseBindingActivity<ActivityGsrSettingsBinding>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsActivity::class.java))
        }
    }

    private lateinit var prefs: SharedPreferences

    override fun initContentLayoutId() = R.layout.activity_gsr_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setupUI()
        loadCurrentSettings()
        setupListeners()

        // Setup modern back handling
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveCurrentSettings()
                    finish()
                }
            },
        )
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Recording Settings"

        // All UI components are now accessible via binding
        // No findViewById calls needed - view binding provides type-safe access

        setupSpinners()
    }

    private fun setupSpinners() {
        // GSR Sampling Rate options
        val samplingRates = arrayOf("32 Hz", "64 Hz", "128 Hz", "256 Hz", "512 Hz")
        binding.gsrSamplingRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, samplingRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // GSR Range options
        val gsrRanges = arrayOf("Auto Range", "40 kΩ - 4 MΩ", "10 kΩ - 1 MΩ", "4 kΩ - 400 kΩ")
        binding.gsrRangeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, gsrRanges).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Video Resolution options
        val videoResolutions = arrayOf("4K UHD (3840×2160)", "Full HD (1920×1080)", "HD (1280×720)", "SD (720×480)")
        binding.videoResolutionSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, videoResolutions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Video Frame Rate options
        val videoFrameRates = arrayOf("30 fps", "60 fps", "24 fps", "15 fps")
        binding.videoFrameRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, videoFrameRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // RAW Frame Rate options
        val rawFrameRates = arrayOf("30 fps", "15 fps", "10 fps", "5 fps", "1 fps")
        binding.rawFrameRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, rawFrameRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // RAW Quality options
        val rawQualities = arrayOf("Maximum (Level 3)", "High (Level 2)", "Standard (Level 1)")
        binding.rawQualitySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, rawQualities).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Data Retention options
        val dataRetentions = arrayOf("Keep Forever", "30 Days", "7 Days", "24 Hours")
        binding.dataRetentionSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, dataRetentions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Sync Tolerance options
        val syncTolerances = arrayOf("1 ms", "5 ms", "10 ms", "50 ms", "100 ms")
        binding.syncToleranceSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, syncTolerances).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun loadCurrentSettings() {
        // Load GSR Settings
        binding.gsrSamplingRateSpinner.setSelection(prefs.getInt("gsr_sampling_rate", 2)) // Default: 128 Hz
        binding.gsrRangeSpinner.setSelection(prefs.getInt("gsr_range", 0)) // Default: Auto Range
        binding.gsrCalibrationSwitch.isChecked = prefs.getBoolean("gsr_calibration", true)

        // Load Video Settings
        binding.videoResolutionSpinner.setSelection(prefs.getInt("video_resolution", 0)) // Default: 4K UHD
        binding.videoFrameRateSpinner.setSelection(prefs.getInt("video_frame_rate", 0)) // Default: 30 fps
        binding.enableVideoSwitch.isChecked = prefs.getBoolean("enable_video", true)
        binding.enableStabilizationSwitch.isChecked = prefs.getBoolean("enable_stabilization", true)

        // Load RAW Capture Settings
        binding.enableRawCaptureSwitch.isChecked = prefs.getBoolean("enable_raw_capture", false)
        binding.rawFrameRateSpinner.setSelection(prefs.getInt("raw_frame_rate", 0)) // Default: 30 fps
        binding.rawQualitySpinner.setSelection(prefs.getInt("raw_quality", 0)) // Default: Maximum

        // Load Session Settings
        binding.autoExportSwitch.isChecked = prefs.getBoolean("auto_export", false)
        binding.dataRetentionSpinner.setSelection(prefs.getInt("data_retention", 0)) // Default: Keep Forever
        binding.sessionPrefixEdit.setText(prefs.getString("session_prefix", "GSR_Session"))

        // Load Sync Settings
        binding.enableTimeSyncSwitch.isChecked = prefs.getBoolean("enable_time_sync", true)
        binding.syncToleranceSpinner.setSelection(prefs.getInt("sync_tolerance", 1)) // Default: 5 ms
    }

    private fun setupListeners() {
        // Auto-save settings when changed
        val saveListener = { saveCurrentSettings() }

        binding.gsrSamplingRateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    saveListener()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.enableVideoSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.enableRawCaptureSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.autoExportSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.enableTimeSyncSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
    }

    private fun saveCurrentSettings() {
        prefs.edit().apply {
            // GSR Settings
            putInt("gsr_sampling_rate", binding.gsrSamplingRateSpinner.selectedItemPosition)
            putInt("gsr_range", binding.gsrRangeSpinner.selectedItemPosition)
            putBoolean("gsr_calibration", binding.gsrCalibrationSwitch.isChecked)

            // Video Settings
            putInt("video_resolution", binding.videoResolutionSpinner.selectedItemPosition)
            putInt("video_frame_rate", binding.videoFrameRateSpinner.selectedItemPosition)
            putBoolean("enable_video", binding.enableVideoSwitch.isChecked)
            putBoolean("enable_stabilization", binding.enableStabilizationSwitch.isChecked)

            // RAW Capture Settings
            putBoolean("enable_raw_capture", binding.enableRawCaptureSwitch.isChecked)
            putInt("raw_frame_rate", binding.rawFrameRateSpinner.selectedItemPosition)
            putInt("raw_quality", binding.rawQualitySpinner.selectedItemPosition)

            // Session Settings
            putBoolean("auto_export", binding.autoExportSwitch.isChecked)
            putInt("data_retention", binding.dataRetentionSpinner.selectedItemPosition)
            putString("session_prefix", binding.sessionPrefixEdit.text.toString())

            // Sync Settings
            putBoolean("enable_time_sync", binding.enableTimeSyncSwitch.isChecked)
            putInt("sync_tolerance", binding.syncToleranceSpinner.selectedItemPosition)

            apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        saveCurrentSettings()
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle via OnBackPressedCallback instead
        onBackPressedDispatcher.onBackPressed()
    }
}
