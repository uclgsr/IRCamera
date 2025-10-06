package com.mpdc4gsr.module.thermalunified.activity
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class ManualStep2ComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isConnecting by remember { mutableStateOf(false) }
        var isConnected by remember { mutableStateOf(false) }
        var isCalibrating by remember { mutableStateOf(false) }
        var calibrationProgress by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Manual Setup - Step 2",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress indicator
                    SetupProgressIndicator(
                        currentStep = 2,
                        totalSteps = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Connection and calibration card
                    ConnectionSetupCard(
                        isConnecting = isConnecting,
                        isConnected = isConnected,
                        isCalibrating = isCalibrating,
                        calibrationProgress = calibrationProgress,
                        onConnect = {
                            isConnecting = true
                            // Simulate connection
                            coroutineScope.launch {
                                delay(3000L)
                                isConnecting = false
                                isConnected = true
                            }
                        },
                        onCalibrate = {
                            isCalibrating = true
                            calibrationProgress = 0f
                            // Simulate calibration
                            coroutineScope.launch {
                                for (i in 1..100) {
                                    delay(50L)
                                    calibrationProgress = i / 100f
                                }
                                isCalibrating = false
                            }
                        },
                        onFinish = { finish() }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Setup Progress",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFFF6B35),
                trackColor = Color(0xFF16131E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step $currentStep of $totalSteps - Final Step",
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun ConnectionSetupCard(
    isConnecting: Boolean,
    isConnected: Boolean,
    isCalibrating: Boolean,
    calibrationProgress: Float,
    onConnect: () -> Unit,
    onCalibrate: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status icon
            when {
                isCalibrating -> {
                    CircularProgressIndicator(
                        progress = { calibrationProgress },
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }
                isConnected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color(0xFF00FF00),
                        modifier = Modifier.size(64.dp)
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            // Title and status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    when {
                        isCalibrating -> "Calibrating Camera"
                        isConnecting -> "Connecting..."
                        isConnected -> "Connection Successful"
                        else -> "Connect & Calibrate"
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    when {
                        isCalibrating -> "Please wait while the camera calibrates (${(calibrationProgress * 100).toInt()}%)"
                        isConnecting -> "Establishing connection with thermal camera..."
                        isConnected -> "Camera connected and ready for calibration"
                        else -> "Connect to your thermal camera and perform initial calibration"
                    },
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            // Connection steps
            if (!isConnected && !isConnecting) {
                ConnectionSteps()
            }
            // Calibration info
            if (isConnected && !isCalibrating && calibrationProgress == 0f) {
                CalibrationInfo()
            }
            // Action buttons
            ActionButtons(
                isConnecting = isConnecting,
                isConnected = isConnected,
                isCalibrating = isCalibrating,
                calibrationComplete = calibrationProgress >= 1f,
                onConnect = onConnect,
                onCalibrate = onCalibrate,
                onFinish = onFinish
            )
        }
    }
}
@Composable
private fun ConnectionSteps() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Connection Steps",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            ConnectionStep("1", "Turn on thermal camera")
            ConnectionStep("2", "Enable camera pairing mode")
            ConnectionStep("3", "Tap connect to search for device")
        }
    }
}
@Composable
private fun ConnectionStep(
    step: String,
    instruction: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    Color(0xFFFF6B35),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            instruction,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
    }
}
@Composable
private fun CalibrationInfo() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Calibration Required",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Camera calibration ensures accurate temperature readings. This process takes about 30 seconds.",
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun ActionButtons(
    isConnecting: Boolean,
    isConnected: Boolean,
    isCalibrating: Boolean,
    calibrationComplete: Boolean,
    onConnect: () -> Unit,
    onCalibrate: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            calibrationComplete -> {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00AA00)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Complete",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Setup Complete",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            isConnected && !isCalibrating -> {
                Button(
                    onClick = onCalibrate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Calibrate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Calibration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            !isConnected && !isConnecting -> {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = "Connect",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Connect to Camera",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // Cancel button (always available)
        if (!calibrationComplete) {
            OutlinedButton(
                onClick = onFinish,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF7D8590)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Setup", fontSize = 14.sp)
            }
        }
    }
}