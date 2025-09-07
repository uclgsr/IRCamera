package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.preference.PreferenceManager
import com.csl.irCamera.R

/**
 * GSR Recording Settings Activity
 * Configure recording parameters, device settings, and data collection options
 */
class GSRSettingsActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsActivity::class.java))
        }
    }

    private lateinit var prefs: SharedPreferences
    
    // GSR Settings
    private lateinit var gsrSamplingRateSpinner: Spinner
    private lateinit var gsrRangeSpinner: Spinner
    private lateinit var gsrCalibrationSwitch: Switch
    
    // Video Settings
    private lateinit var videoResolutionSpinner: Spinner
    private lateinit var videoFrameRateSpinner: Spinner
    private lateinit var enableVideoSwitch: Switch
    private lateinit var enableStabilizationSwitch: Switch
    
    // RAW Capture Settings
    private lateinit var enableRawCaptureSwitch: Switch
    private lateinit var rawFrameRateSpinner: Spinner
    private lateinit var rawQualitySpinner: Spinner
    
    // Session Settings
    private lateinit var autoExportSwitch: Switch
    private lateinit var dataRetentionSpinner: Spinner
    private lateinit var sessionPrefixEdit: EditText
    
    // Sync Settings
    private lateinit var enableTimeSyncSwitch: Switch
    private lateinit var syncToleranceSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gsr_settings)
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        setupUI()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Recording Settings"
        
        // GSR Settings
        gsrSamplingRateSpinner = findViewById(R.id.gsr_sampling_rate_spinner)
        gsrRangeSpinner = findViewById(R.id.gsr_range_spinner)
        gsrCalibrationSwitch = findViewById(R.id.gsr_calibration_switch)
        
        // Video Settings
        videoResolutionSpinner = findViewById(R.id.video_resolution_spinner)
        videoFrameRateSpinner = findViewById(R.id.video_frame_rate_spinner)
        enableVideoSwitch = findViewById(R.id.enable_video_switch)
        enableStabilizationSwitch = findViewById(R.id.enable_stabilization_switch)
        
        // RAW Capture Settings
        enableRawCaptureSwitch = findViewById(R.id.enable_raw_capture_switch)
        rawFrameRateSpinner = findViewById(R.id.raw_frame_rate_spinner)
        rawQualitySpinner = findViewById(R.id.raw_quality_spinner)
        
        // Session Settings
        autoExportSwitch = findViewById(R.id.auto_export_switch)
        dataRetentionSpinner = findViewById(R.id.data_retention_spinner)
        sessionPrefixEdit = findViewById(R.id.session_prefix_edit)
        
        // Sync Settings
        enableTimeSyncSwitch = findViewById(R.id.enable_time_sync_switch)
        syncToleranceSpinner = findViewById(R.id.sync_tolerance_spinner)
        
        setupSpinners()
    }

    private fun setupSpinners() {
        // GSR Sampling Rate options
        val samplingRates = arrayOf("32 Hz", "64 Hz", "128 Hz", "256 Hz", "512 Hz")
        gsrSamplingRateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, samplingRates).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // GSR Range options
        val gsrRanges = arrayOf("Auto Range", "40 kΩ - 4 MΩ", "10 kΩ - 1 MΩ", "4 kΩ - 400 kΩ")
        gsrRangeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gsrRanges).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Video Resolution options
        val videoResolutions = arrayOf("4K UHD (3840×2160)", "Full HD (1920×1080)", "HD (1280×720)", "SD (720×480)")
        videoResolutionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, videoResolutions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Video Frame Rate options
        val videoFrameRates = arrayOf("30 fps", "60 fps", "24 fps", "15 fps")
        videoFrameRateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, videoFrameRates).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // RAW Frame Rate options
        val rawFrameRates = arrayOf("30 fps", "15 fps", "10 fps", "5 fps", "1 fps")
        rawFrameRateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rawFrameRates).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // RAW Quality options
        val rawQualities = arrayOf("Maximum (Level 3)", "High (Level 2)", "Standard (Level 1)")
        rawQualitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rawQualities).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Data Retention options
        val dataRetentions = arrayOf("Keep Forever", "30 Days", "7 Days", "24 Hours")
        dataRetentionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataRetentions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Sync Tolerance options
        val syncTolerances = arrayOf("1 ms", "5 ms", "10 ms", "50 ms", "100 ms")
        syncToleranceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, syncTolerances).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadCurrentSettings() {
        // Load GSR Settings
        gsrSamplingRateSpinner.setSelection(prefs.getInt("gsr_sampling_rate", 2)) // Default: 128 Hz
        gsrRangeSpinner.setSelection(prefs.getInt("gsr_range", 0)) // Default: Auto Range
        gsrCalibrationSwitch.isChecked = prefs.getBoolean("gsr_calibration", true)
        
        // Load Video Settings
        videoResolutionSpinner.setSelection(prefs.getInt("video_resolution", 0)) // Default: 4K UHD
        videoFrameRateSpinner.setSelection(prefs.getInt("video_frame_rate", 0)) // Default: 30 fps
        enableVideoSwitch.isChecked = prefs.getBoolean("enable_video", true)
        enableStabilizationSwitch.isChecked = prefs.getBoolean("enable_stabilization", true)
        
        // Load RAW Capture Settings
        enableRawCaptureSwitch.isChecked = prefs.getBoolean("enable_raw_capture", false)
        rawFrameRateSpinner.setSelection(prefs.getInt("raw_frame_rate", 0)) // Default: 30 fps
        rawQualitySpinner.setSelection(prefs.getInt("raw_quality", 0)) // Default: Maximum
        
        // Load Session Settings
        autoExportSwitch.isChecked = prefs.getBoolean("auto_export", false)
        dataRetentionSpinner.setSelection(prefs.getInt("data_retention", 0)) // Default: Keep Forever
        sessionPrefixEdit.setText(prefs.getString("session_prefix", "GSR_Session"))
        
        // Load Sync Settings
        enableTimeSyncSwitch.isChecked = prefs.getBoolean("enable_time_sync", true)
        syncToleranceSpinner.setSelection(prefs.getInt("sync_tolerance", 1)) // Default: 5 ms
    }

    private fun setupListeners() {
        // Auto-save settings when changed
        val saveListener = { saveCurrentSettings() }
        
        gsrSamplingRateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { saveListener() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        enableVideoSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        enableRawCaptureSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        autoExportSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        enableTimeSyncSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
    }

    private fun saveCurrentSettings() {
        prefs.edit().apply {
            // GSR Settings
            putInt("gsr_sampling_rate", gsrSamplingRateSpinner.selectedItemPosition)
            putInt("gsr_range", gsrRangeSpinner.selectedItemPosition)
            putBoolean("gsr_calibration", gsrCalibrationSwitch.isChecked)
            
            // Video Settings
            putInt("video_resolution", videoResolutionSpinner.selectedItemPosition)
            putInt("video_frame_rate", videoFrameRateSpinner.selectedItemPosition)
            putBoolean("enable_video", enableVideoSwitch.isChecked)
            putBoolean("enable_stabilization", enableStabilizationSwitch.isChecked)
            
            // RAW Capture Settings
            putBoolean("enable_raw_capture", enableRawCaptureSwitch.isChecked)
            putInt("raw_frame_rate", rawFrameRateSpinner.selectedItemPosition)
            putInt("raw_quality", rawQualitySpinner.selectedItemPosition)
            
            // Session Settings
            putBoolean("auto_export", autoExportSwitch.isChecked)
            putInt("data_retention", dataRetentionSpinner.selectedItemPosition)
            putString("session_prefix", sessionPrefixEdit.text.toString())
            
            // Sync Settings
            putBoolean("enable_time_sync", enableTimeSyncSwitch.isChecked)
            putInt("sync_tolerance", syncToleranceSpinner.selectedItemPosition)
            
            apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        saveCurrentSettings()
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        saveCurrentSettings()
        super.onBackPressed()
    }
}