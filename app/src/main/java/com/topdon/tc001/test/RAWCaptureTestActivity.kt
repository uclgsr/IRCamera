package com.topdon.tc001.test

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.topdon.tc001.camera.RGBCameraRecorder

/**
 * Test Activity to demonstrate RAW Image Capture UI
 * Shows the enhanced multi-modal recording interface with RAW capture options
 */
class RAWCaptureTestActivity : AppCompatActivity() {
    
    private lateinit var cameraPreview: android.view.TextureView
    private lateinit var enableVideoSwitch: Switch
    private lateinit var enable4KSwitch: Switch
    private lateinit var enableRawCaptureSwitch: Switch
    private lateinit var rawFrameRateSpinner: Spinner
    private lateinit var startStopButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create layout that matches the enhanced MultiModalRecordingActivity
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "RAW Image Capture Demo - MPDC4GSR"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
            setTextColor(Color.BLACK)
        }
        layout.addView(title)
        
        // Camera preview section
        val cameraSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 16)
        }
        
        val cameraTitle = TextView(this).apply {
            text = "📹 RGB Camera Preview"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        cameraSection.addView(cameraTitle)
        
        cameraPreview = android.view.TextureView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
            setBackgroundColor(Color.LTGRAY)
        }
        cameraSection.addView(cameraPreview)
        layout.addView(cameraSection)
        
        // Recording options section
        val optionsSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        
        val optionsTitle = TextView(this).apply {
            text = "🎥 Recording Options"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
            setTextColor(Color.BLACK)
        }
        optionsSection.addView(optionsTitle)
        
        // Video recording toggle
        val videoLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        videoLayout.addView(TextView(this).apply { 
            text = "📹 Record Video"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(Color.BLACK)
        })
        enableVideoSwitch = Switch(this).apply { 
            isChecked = true
            scaleX = 1.2f
            scaleY = 1.2f
        }
        videoLayout.addView(enableVideoSwitch)
        optionsSection.addView(videoLayout)
        
        // 4K recording toggle
        val resolution4KLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL 
            setPadding(0, 8, 0, 8)
        }
        resolution4KLayout.addView(TextView(this).apply { 
            text = "🎯 4K Recording (3840×2160)"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(Color.BLACK)
        })
        enable4KSwitch = Switch(this).apply { 
            isChecked = false
            scaleX = 1.2f
            scaleY = 1.2f
        }
        resolution4KLayout.addView(enable4KSwitch)
        optionsSection.addView(resolution4KLayout)
        
        // RAW capture toggle (HIGHLIGHT THIS AS NEW FEATURE)
        val rawLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
        }
        rawLayout.addView(TextView(this).apply { 
            text = "📸 RAW Image Capture (DNG)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(Color.parseColor("#1976D2"))
        })
        enableRawCaptureSwitch = Switch(this).apply { 
            isChecked = true  // Default to enabled to show the feature
            scaleX = 1.2f
            scaleY = 1.2f
            setOnCheckedChangeListener { _, isChecked ->
                rawFrameRateSpinner.isEnabled = isChecked
                if (isChecked) {
                    rawFrameRateSpinner.alpha = 1.0f
                } else {
                    rawFrameRateSpinner.alpha = 0.5f
                }
            }
        }
        rawLayout.addView(enableRawCaptureSwitch)
        optionsSection.addView(rawLayout)
        
        // RAW frame rate selection (HIGHLIGHT AS NEW)
        val frameRateLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 0, 8)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
        }
        frameRateLayout.addView(TextView(this).apply { 
            text = "⚡ RAW Frame Rate"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(Color.parseColor("#1976D2"))
        })
        rawFrameRateSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@RAWCaptureTestActivity,
                android.R.layout.simple_spinner_item,
                listOf("30 fps", "15 fps", "10 fps", "5 fps")
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(0) // Default to 30fps
        }
        frameRateLayout.addView(rawFrameRateSpinner)
        optionsSection.addView(frameRateLayout)
        layout.addView(optionsSection)
        
        // Session info
        val sessionSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        sessionSection.addView(TextView(this).apply { 
            text = "📋 Session ID: MultiModal_20250101_143022"
            textSize = 12f
            setTextColor(Color.GRAY)
        })
        layout.addView(sessionSection)
        
        // Start/Stop button
        startStopButton = Button(this).apply {
            text = "▶️ Start Multi-Modal Recording"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 20, 0, 20)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
        }
        layout.addView(startStopButton)
        
        // Status display
        statusText = TextView(this).apply {
            text = "Ready to record: 4K60FPS Video + 30fps RAW Images + 128Hz GSR"
            textSize = 14f
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(0, 16, 0, 0)
        }
        layout.addView(statusText)
        
        // Feature highlights
        val featuresSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#FFF3E0"))
        }
        
        featuresSection.addView(TextView(this).apply {
            text = "🎯 New RAW Capture Features:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#E65100"))
        })
        
        featuresSection.addView(TextView(this).apply {
            text = "• Samsung Level 3 RAW DNG images at configurable frame rates\n" +
                   "• Ground truth timing from Exynos/Snapdragon CPU\n" +
                   "• Parallel recording with 4K video and GSR data\n" +
                   "• Full sensor resolution (4032×3024 on Samsung S22)\n" +
                   "• Synchronized timestamps across all modalities"
            textSize = 12f
            setPadding(16, 8, 0, 0)
            setTextColor(Color.parseColor("#BF360C"))
        })
        layout.addView(featuresSection)
        
        setContentView(layout)
    }
}