package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mpdc4gsr.module.thermalunified.compose.ThermalCameraScreen
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

/**
 * Task B: Complete Thermal Camera Activity using Compose
 * 
 * This activity demonstrates:
 * - Complete migration of thermal camera UI to Compose
 * - Preservation of existing IrSurfaceView functionality
 * - Modern Material 3 UI with thermal imaging colors
 * - Enhanced temperature data visualization
 * - Improved recording controls and status indicators
 */
class ThermalCameraComposeActivity : ComponentActivity() {

    private val viewModel: ThermalFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                ThermalCameraContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ThermalCameraContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Thermal Camera",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            ThermalCameraScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}