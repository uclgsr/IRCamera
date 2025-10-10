package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class ManualStep1ComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        val context = LocalContext.current
        var currentStep by remember { mutableIntStateOf(1) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Manual Setup - Step 1",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF16131E),
                            ),
                    )
                },
                containerColor = Color(0xFF16131E),
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color(0xFF16131E))
                            .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Progress indicator
                    SetupProgressIndicator(
                        currentStep = 1,
                        totalSteps = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Main setup card
                    ManualSetupCard(
                        step = currentStep,
                        onNextStep = {
                            val intent = Intent(context, ManualStep2ComposeActivity::class.java)
                            context.startActivity(intent)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Setup Progress",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps.toFloat() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                color = Color(0xFFFF6B35),
                trackColor = Color(0xFF16131E),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step $currentStep of $totalSteps",
                color = Color(0xFF7D8590),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ManualSetupCard(
    step: Int,
    onNextStep: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Setup icon
            Icon(
                Icons.Default.Build,
                contentDescription = "Setup",
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(64.dp),
            )
            // Title
            Text(
                "Thermal Camera Setup",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            // Instructions
            SetupInstructions()
            // Setup checklist
            SetupChecklist()
            // Continue button
            Button(
                onClick = onNextStep,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35),
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(
                    "Continue to Step 2",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SetupInstructions() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Device Preparation",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        InstructionItem(
            step = "1",
            instruction = "Ensure your thermal camera device is powered on and ready",
        )
        InstructionItem(
            step = "2",
            instruction = "Check that Bluetooth is enabled on your mobile device",
        )
        InstructionItem(
            step = "3",
            instruction = "Place the thermal camera within 3 meters of your phone",
        )
    }
}

@Composable
private fun InstructionItem(
    step: String,
    instruction: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .background(
                        Color(0xFFFF6B35),
                        RoundedCornerShape(12.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            instruction,
            color = Color(0xFF7D8590),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SetupChecklist() {
    var devicePowered by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var cameraInRange by remember { mutableStateOf(false) }
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF16131E),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Setup Checklist",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            ChecklistItem(
                text = "Thermal camera powered on",
                checked = devicePowered,
                onCheckedChange = { devicePowered = it },
            )
            ChecklistItem(
                text = "Bluetooth enabled",
                checked = bluetoothEnabled,
                onCheckedChange = { bluetoothEnabled = it },
            )
            ChecklistItem(
                text = "Camera within range",
                checked = cameraInRange,
                onCheckedChange = { cameraInRange = it },
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = Color(0xFFFF6B35),
                    uncheckedColor = Color(0xFF7D8590),
                    checkmarkColor = Color.White,
                ),
        )
        Text(
            text,
            color = if (checked) Color.White else Color(0xFF7D8590),
            fontSize = 14.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
