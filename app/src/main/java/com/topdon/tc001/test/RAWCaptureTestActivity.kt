package com.topdon.tc001.test

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.csl.irCamera.databinding.ActivityRawCaptureTestBinding

class RAWCaptureTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRawCaptureTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawCaptureTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize UI components through view binding
        setupSpinner()
        setupSwitchListeners()
    }

    private fun setupSpinner() {
        binding.rawFrameRateSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("30 fps", "15 fps", "10 fps", "5 fps"),
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        binding.rawFrameRateSpinner.setSelection(0) // Default to 30fps
    }

    private fun setupSwitchListeners() {
        binding.enableRawCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.rawFrameRateSpinner.isEnabled = isChecked
            binding.rawFrameRateSpinner.alpha = if (isChecked) 1.0f else 0.5f
        }
    }
}
