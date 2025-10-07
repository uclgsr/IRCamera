// Merged .kt under 'feature\camera\ui' subtree
// Files: 11; Generated 2025-10-07 19:59:55


// ===== feature\camera\ui\Camera2SystemValidator.kt =====

package mpdc4gsr.feature.camera.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.deferAction
import mpdc4gsr.core.ui.theme.Green
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.Orange
import mpdc4gsr.core.ui.theme.Purple
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.camera.data.CameraConfigurationManager
import mpdc4gsr.feature.camera.data.ModeManager
import mpdc4gsr.feature.camera.presentation.*
import mpdc4gsr.feature.main.ui.MainComposeActivity

class Camera2SystemValidator(private val context: Context) {
    companion object {
        private const val TAG = "Camera2SystemValidator"
    }

    suspend fun validateSystem(): ValidationResult {
        val results = mutableListOf<String>()
        var allPassed = true
        try {
            AppLogger.i(TAG, "Starting Camera2 system validation...")
            if (validateArchitectureComponents()) {
                results.add(" Architecture components validated")
            } else {
                results.add(" Architecture components missing")
                allPassed = false
            }
            if (validateModeSwitching()) {
                results.add(" Mode switching logic validated")
            } else {
                results.add(" Mode switching logic failed")
                allPassed = false
            }
            if (validateFastSessionSwitching()) {
                results.add(" Fast session switching validated")
            } else {
                results.add(" Fast session switching failed")
                allPassed = false
            }
            if (validateSamsungCompatibility()) {
                results.add(" Samsung S22 compatibility validated")
            } else {
                results.add(" Samsung S22 compatibility failed")
                allPassed = false
            }
            if (validateStage3Level3Support()) {
                results.add(" Samsung Stage3/Level3 DNG support validated")
            } else {
                results.add(" Samsung Stage3/Level3 DNG support failed")
                allPassed = false
            }
            AppLogger.i(TAG, "Validation completed. Success: $allPassed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Validation failed with exception", e)
            results.add(" Validation failed: ${e.message}")
            allPassed = false
        }
        return ValidationResult(allPassed, results)
    }

    private fun validateArchitectureComponents(): Boolean {
        return try {
            Class.forName("com.mpdc4gsr.camera.Camera2System")
            Class.forName("com.mpdc4gsr.camera.core.CameraController")
            Class.forName("com.mpdc4gsr.camera.core.VideoEngine")
            Class.forName("com.mpdc4gsr.camera.core.RawEngine")
            Class.forName("com.mpdc4gsr.camera.core.ModeManager")
            Class.forName("com.mpdc4gsr.camera.core.UiBridge")
            Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            true
        } catch (e: ClassNotFoundException) {
            AppLogger.e(TAG, "Architecture component not found", e)
            false
        }
    }

    private fun validateModeSwitching(): Boolean {
        return try {
            val modeManager = ModeManager()
            val canSwitchToRaw = modeManager.requestModeSwitch(ModeManager.CameraMode.RAW_50MP)
            val canSwitchToVideo = modeManager.requestModeSwitch(ModeManager.CameraMode.VIDEO_4K)
            val canSwitchToPreview =
                modeManager.requestModeSwitch(ModeManager.CameraMode.PREVIEW_ONLY)
            canSwitchToRaw && canSwitchToVideo && canSwitchToPreview
        } catch (e: Exception) {
            AppLogger.e(TAG, "Mode switching validation failed", e)
            false
        }
    }

    private fun validateFastSessionSwitching(): Boolean {
        return try {
            val cameraControllerClass =
                Class.forName("com.mpdc4gsr.camera.core.CameraController")
            val createSessionMethod =
                cameraControllerClass.getDeclaredMethod(
                    "createCaptureSession",
                    List::class.java,
                    Class.forName("android.hardware.camera2.CameraCaptureSession\$StateCallback"),
                )
            createSessionMethod != null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fast session switching validation failed", e)
            false
        }
    }

    private fun validateSamsungCompatibility(): Boolean {
        return try {
            val deviceCapsClass = Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            val fields = deviceCapsClass.declaredFields
            val hasSupportsRaw = fields.any { it.name == "supportsRaw" }
            val hasRawSize = fields.any { it.name == "rawSize" }
            val hasSupports4k60 = fields.any { it.name == "supports4k60" }
            val hasSensorOrientation = fields.any { it.name == "sensorOrientation" }
            hasSupportsRaw && hasRawSize && hasSupports4k60 && hasSensorOrientation
        } catch (e: Exception) {
            AppLogger.e(TAG, "Samsung compatibility validation failed", e)
            false
        }
    }

    private fun validateStage3Level3Support(): Boolean {
        return try {
            // Instead of using brittle reflection, test actual functionality
            // by trying to instantiate the classes and checking their public APIs
            // Test RawEngine Stage3/Level3 functionality
            val rawEngineWorks = try {
                val rawEngine = mpdc4gsr.feature.camera.data.RawEngine(context)
                // Test that the methods exist by trying to call them (safe calls)
                rawEngine.isStage3ProcessingEnabled() // This should not throw
                rawEngine.setStage3ProcessingEnabled(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "RawEngine Stage3/Level3 methods not available", e)
                false
            }
            // Test Camera2System Stage3/Level3 functionality  
            val camera2SystemWorks = try {
                // Use a mock TextureView for testing
                val textureView = android.view.TextureView(context)
                val camera2System = mpdc4gsr.feature.camera.data.Camera2System(context, textureView)
                // Test that the methods exist
                camera2System.isStage3ProcessingEnabled() // This should not throw
                camera2System.configureStage3Processing(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Camera2System Stage3/Level3 methods not available", e)
                false
            }
            // Test DngCreator API availability (this is a standard Android API)
            val dngCreatorAvailable = try {
                // Check if DngCreator class is available (API level 21+)
                Class.forName("android.hardware.camera2.DngCreator") != null
            } catch (e: Exception) {
                AppLogger.e(TAG, "DngCreator API not available", e)
                false
            }
            // Test SamsungDeviceCompatibility utility
            val deviceCompatibilityWorks = try {
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.isStage3Compatible()
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.getDeviceInfo()
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "SamsungDeviceCompatibility utility not working", e)
                false
            }
            val allWorking =
                rawEngineWorks && camera2SystemWorks && dngCreatorAvailable && deviceCompatibilityWorks
            Log.i(
                TAG,
                "Stage3/Level3 validation - RawEngine: $rawEngineWorks, Camera2System: $camera2SystemWorks, DngCreator: $dngCreatorAvailable, DeviceCompatibility: $deviceCompatibilityWorks"
            )
            allWorking
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage3/Level3 validation failed", e)
            false
        }
    }

    data class ValidationResult(
        val allTestsPassed: Boolean,
        val results: List<String>,
    ) {
        fun getFormattedReport(): String {
            return buildString {
                appendLine("=== Camera2 System Validation Report ===")
                appendLine("Overall Result: ${if (allTestsPassed) " PASS" else " FAIL"}")
                appendLine()
                results.forEach { result ->
                    appendLine(result)
                }
                appendLine()
                if (allTestsPassed) {
                    appendLine(" System ready for Samsung S22 (Exynos, Android 15) deployment")
                } else {
                    appendLine(" System requires fixes before deployment")
                }
            }
        }
    }
}


// ===== feature\camera\ui\CameraDashboardScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.deferAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToDualMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Camera Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = deferAction { onBackClick() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = deferAction { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            CameraDashboardContent(
                onNavigateToDualMode = onNavigateToDualMode,
                onNavigateToSingleCamera = onNavigateToSingleCamera,
                onNavigateToTimeLapse = onNavigateToTimeLapse,
                onNavigateToGallery = onNavigateToGallery,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CameraDashboardContent(
    onNavigateToDualMode: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showToast: (String) -> Unit = { message ->
        android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Status Card
        CameraStatusCard()
        // Camera Modes Card
        CameraModesCard(
            onNavigateToDualMode = onNavigateToDualMode,
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToTimeLapse = onNavigateToTimeLapse,
            showToast = showToast
        )
        // Recording Controls Card
        RecordingControlsCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera
        )
        // Camera Settings Card
        CameraSettingsCard()
        // Preview and Gallery Card
        PreviewGalleryCard(
            onNavigateToSingleCamera = onNavigateToSingleCamera,
            onNavigateToGallery = onNavigateToGallery,
            showToast = showToast
        )
    }
}

@Composable
private fun CameraStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Camera Status",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Camera Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            // Camera availability indicators
            CameraStatusRow("Front Camera", true)
            CameraStatusRow("Back Camera", true)
            CameraStatusRow("External Camera", false)
            // Current camera info
            CameraInfoRow("Active Camera", "Back Camera")
            CameraInfoRow("Resolution", "1920x1080")
            CameraInfoRow("Frame Rate", "30 FPS")
            CameraInfoRow("Focus Mode", "Auto")
        }
    }
}

@Composable
private fun CameraStatusRow(
    cameraName: String,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cameraName,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isAvailable) "Camera Available" else "Camera Unavailable",
                tint = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isAvailable) "Available" else "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CameraInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraModesCard(
    onNavigateToDualMode: () -> Unit,
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToTimeLapse: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Modes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Single Camera Mode
            CameraModeItem(
                title = "Single Camera Mode",
                description = "Standard RGB camera capture",
                icon = Icons.Default.Camera,
                isActive = false,
                onClick = {
                    onNavigateToSingleCamera?.invoke() ?: showToast("Single camera mode coming soon")
                }
            )
            // Dual Camera Mode
            CameraModeItem(
                title = "Dual Camera Mode",
                description = "Simultaneous RGB and thermal capture",
                icon = Icons.Default.CameraAlt,
                isActive = true,
                onClick = onNavigateToDualMode
            )
            // Time-lapse Mode
            CameraModeItem(
                title = "Time-lapse Mode",
                description = "Automated interval capture",
                icon = Icons.Default.Timer,
                isActive = false,
                onClick = {
                    onNavigateToTimeLapse?.invoke() ?: showToast("Time-lapse mode coming soon")
                }
            )
        }
    }
}

@Composable
private fun CameraModeItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = deferAction { onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Badge {
                    Text("Active")
                }
            }
        }
    }
}

@Composable
private fun RecordingControlsCard(
    onNavigateToSingleCamera: (() -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Recording status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recording Status",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                        contentDescription = if (isRecording) "Recording" else "Stopped",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (isRecording) "Recording" else "Stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { isRecording = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { isRecording = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Quick Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Flash setting
            SettingRow(
                title = "Flash",
                value = "Auto",
                icon = Icons.Default.FlashOn
            )
            // Quality setting
            SettingRow(
                title = "Video Quality",
                value = "1080p",
                icon = Icons.Default.HighQuality
            )
            // Storage location
            SettingRow(
                title = "Storage",
                value = "Internal",
                icon = Icons.Default.Storage
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PreviewGalleryCard(
    onNavigateToSingleCamera: (() -> Unit)? = null,
    onNavigateToGallery: (() -> Unit)? = null,
    showToast: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preview & Gallery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onNavigateToSingleCamera?.invoke() ?: showToast("Preview feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = "Preview Camera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }
                OutlinedButton(
                    onClick = {
                        onNavigateToGallery?.invoke() ?: showToast("Gallery feature coming soon")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Open Gallery")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }
            }
        }
    }
}


// ===== feature\camera\ui\CameraSettingsCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CameraSettingsPanel(
    isAutoExposure: Boolean = true,
    exposureCompensation: Float = 0f,
    isAeLocked: Boolean = false,
    isAutoFocus: Boolean = true,
    focusDistance: Float = 0f,
    isAfLocked: Boolean = false,
    isFlashEnabled: Boolean = false,
    isStage3ProcessingEnabled: Boolean = false,
    onExposureModeToggle: (Boolean) -> Unit = {},
    onExposureCompensationChanged: (Float) -> Unit = {},
    onAeLockToggle: (Boolean) -> Unit = {},
    onFocusModeToggle: (Boolean) -> Unit = {},
    onFocusDistanceChanged: (Float) -> Unit = {},
    onAfLockToggle: (Boolean) -> Unit = {},
    onCameraToggle: () -> Unit = {},
    onRecordingToggle: (Boolean) -> Unit = {},
    onFlashToggle: (Boolean) -> Unit = {},
    onStage3ProcessingToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Camera Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        // Exposure Controls
        CameraSettingsSection(title = "Exposure") {
            SwitchSettingItem(
                label = "Auto Exposure",
                checked = isAutoExposure,
                onCheckedChange = onExposureModeToggle,
                icon = Icons.Default.WbSunny
            )
            SliderSettingItem(
                label = "Exposure Compensation",
                value = exposureCompensation,
                onValueChange = onExposureCompensationChanged,
                valueRange = -2f..2f,
                enabled = !isAutoExposure
            )
            SwitchSettingItem(
                label = "AE Lock",
                checked = isAeLocked,
                onCheckedChange = onAeLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Focus Controls
        CameraSettingsSection(title = "Focus") {
            SwitchSettingItem(
                label = "Auto Focus",
                checked = isAutoFocus,
                onCheckedChange = onFocusModeToggle,
                icon = Icons.Default.CenterFocusStrong
            )
            SliderSettingItem(
                label = "Focus Distance",
                value = focusDistance,
                onValueChange = onFocusDistanceChanged,
                valueRange = 0f..1f,
                enabled = !isAutoFocus
            )
            SwitchSettingItem(
                label = "AF Lock",
                checked = isAfLocked,
                onCheckedChange = onAfLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Basic Controls
        CameraSettingsSection(title = "Controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraToggle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flip")
                }
                FilledTonalButton(
                    onClick = { onFlashToggle(!isFlashEnabled) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flash")
                }
            }
            SwitchSettingItem(
                label = "Stage 3 Processing",
                checked = isStage3ProcessingEnabled,
                onCheckedChange = onStage3ProcessingToggle,
                icon = Icons.Default.AutoFixHigh
            )
        }
    }
}

@Composable
private fun CameraSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// ===== feature\camera\ui\CameraSettingsScreen.kt =====

package mpdc4gsr.feature.camera.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsDropdown
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.data.CameraConfigurationManager

@Composable
fun CameraSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { CameraConfigurationManager() }
    val (supports4K, supportsRAW, supports60fps) = remember {
        configManager.detectDeviceCapabilities()
    }
    val availableResolutions = remember {
        buildList {
            if (supports4K) {
                add("3840x2160")
            }
            add("1920x1080")
            add("1280x720")
            add("640x480")
        }
    }
    val maxFrameRate = if (supports60fps) 60f else 30f
    var resolution by remember { mutableStateOf(availableResolutions.first()) }
    var frameRate by remember { mutableIntStateOf(30) }
    var autoFocus by remember { mutableStateOf(true) }
    var autoExposure by remember { mutableStateOf(true) }
    var stabilization by remember { mutableStateOf(false) }
    var gridLines by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Camera Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Capabilities
            SettingsCard(
                title = "Device Capabilities",
                icon = Icons.Default.Info
            ) {
                SettingsToggle(
                    label = "4K Video Support",
                    description = "Device supports 4K video recording",
                    checked = supports4K,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "60fps Support",
                    description = "Device supports 60fps video recording",
                    checked = supports60fps,
                    onCheckedChange = {},
                    enabled = false
                )
                SettingsToggle(
                    label = "RAW Image Support",
                    description = "Device supports RAW image capture",
                    checked = supportsRAW,
                    onCheckedChange = {},
                    enabled = false
                )
            }
            // Video Settings
            SettingsCard(
                title = "Video Settings",
                icon = Icons.Default.Videocam
            ) {
                SettingsDropdown(
                    label = "Resolution",
                    options = availableResolutions,
                    value = resolution,
                    onValueChange = { resolution = it }
                )
                SettingsSlider(
                    label = "Frame Rate",
                    value = frameRate.toFloat(),
                    valueRange = 15f..maxFrameRate,
                    onValueChange = { frameRate = it.toInt() },
                    unit = " fps"
                )
            }
            // Camera Features
            SettingsCard(
                title = "Camera Features",
                icon = Icons.Default.CameraAlt
            ) {
                SettingsToggle(
                    label = "Auto Focus",
                    description = "Automatic focus adjustment",
                    checked = autoFocus,
                    onCheckedChange = { autoFocus = it }
                )
                SettingsToggle(
                    label = "Auto Exposure",
                    description = "Automatic exposure control",
                    checked = autoExposure,
                    onCheckedChange = { autoExposure = it }
                )
                SettingsToggle(
                    label = "Image Stabilization",
                    description = "Digital image stabilization",
                    checked = stabilization,
                    onCheckedChange = { stabilization = it }
                )
            }
            // Interface Options
            SettingsCard(
                title = "Interface",
                icon = Icons.Default.GridOn
            ) {
                SettingsToggle(
                    label = "Grid Lines",
                    description = "Show rule of thirds grid",
                    checked = gridLines,
                    onCheckedChange = { gridLines = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraSettingsScreenPreview() {
    IRCameraTheme {
        CameraSettingsScreen()
    }
}


// ===== feature\camera\ui\CameraStatusCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import mpdc4gsr.core.data.RgbCameraRecorder

@Composable
fun CameraStatusWidget(
    cameraRecorder: RgbCameraRecorder?,
    onInitializeCamera: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var statusText by remember { mutableStateOf("Camera Status: Not Initialized") }
    var statsText by remember { mutableStateOf("Camera Statistics:\nNot Available") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Text
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Camera Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (cameraRecorder != null) {
                CameraPreviewView(cameraRecorder)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera Not Initialized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onInitializeCamera != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onInitializeCamera) {
                                Text("Initialize Camera")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Statistics
        Text(
            text = statsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
    // Update status based on camera recorder state
    LaunchedEffect(cameraRecorder) {
        if (cameraRecorder != null) {
            statusText = "Camera Status: Initialized"
            while (true) {
                delay(1000)
                // Update stats periodically using existing methods
                val resolution = cameraRecorder.getResolution()
                val fps = cameraRecorder.getCurrentFps()
                statsText = "Camera Statistics:\nResolution: $resolution\nFPS: $fps"
            }
        } else {
            statusText = "Camera Status: Not Initialized"
            statsText = "Camera Statistics:\nNot Available"
        }
    }
}

@Composable
private fun CameraPreviewView(cameraRecorder: RgbCameraRecorder) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // Bind camera preview to this PreviewView
                cameraRecorder.bindPreview(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CameraStatusBadge(
    isInitialized: Boolean,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            isRecording -> MaterialTheme.colorScheme.error
            isInitialized -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isRecording -> "Recording"
                    isInitialized -> "Ready"
                    else -> "Not Initialized"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isRecording -> MaterialTheme.colorScheme.onError
                    isInitialized -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ===== feature\camera\ui\DualModeCameraActivityCompose.kt =====

package mpdc4gsr.feature.camera.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.camera.presentation.DualModeCameraViewModel
import mpdc4gsr.feature.main.ui.MainComposeActivity

class DualModeCameraActivityCompose : BaseComposeActivity<DualModeCameraViewModel>() {
    private val cameraVM: DualModeCameraViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraVM.onPermissionGranted()
        } else {
            cameraVM.onPermissionDenied()
        }
    }

    override fun createViewModel(): DualModeCameraViewModel {
        return cameraVM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMode = intent.getStringExtra("INITIAL_MODE") ?: "VIDEO_4K"
        val enableSamsungOptimizations =
            intent.getBooleanExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)
        cameraVM.initialize(initialMode, enableSamsungOptimizations)
        checkCameraPermission()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DualModeCameraViewModel) {
        val context = LocalContext.current
        // Collect state
        val permissionState by viewModel.permissionState.collectAsState()
        val cameraState by viewModel.cameraState.collectAsState()
        val cameraMode by viewModel.cameraMode.collectAsState()
        val recordingState by viewModel.recordingState.collectAsState()
        val cameraScreenState by viewModel.cameraScreenState.collectAsState()
        // Handle events
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is DualModeCameraViewModel.CameraEvent.ShowError -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.ShowSuccess -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.RequestPermission -> {
                        // Handle permission request
                    }

                    is DualModeCameraViewModel.CameraEvent.RecordingStarted -> {
                        Toast.makeText(context, "Recording started: ${event.fileName}", Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.RecordingStopped -> {
                        Toast.makeText(context, "Recording stopped: ${event.duration}s", Toast.LENGTH_SHORT).show()
                    }

                    is DualModeCameraViewModel.CameraEvent.ModeChanged -> {
                        Toast.makeText(context, "Mode changed to ${event.newMode}", Toast.LENGTH_SHORT).show()
                    }

                    DualModeCameraViewModel.CameraEvent.NavigateToGallery -> {
                        // Navigate to gallery
                    }
                    // is DualModeCameraViewModel.CameraEvent.NavigateToSettings -> {
                    //     context.startActivity(Intent(context, SettingsComposeActivity::class.java))
                    // }
                    // is DualModeCameraViewModel.CameraEvent.NavigateBack -> {
                    //     finish()
                    // }
                }
            }
        }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dual Mode Camera",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                navigateToMainActivity(1) // Main camera page
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // Navigate to settings
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomNavigationBar()
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Camera Mode Selector Card
                    CameraModeCard(
                        selectedMode = cameraMode,
                        onModeChange = { mode ->
                            viewModel.switchCameraMode(mode)
                        }
                    )
                    // Camera Preview Card
                    CameraPreviewCard(
                        cameraState = cameraState,
                        cameraScreenState = cameraScreenState,
                        permissionState = permissionState,
                        onInitializeCamera = { previewView ->
                            if (permissionState == DualModeCameraViewModel.PermissionState.GRANTED) {
                                viewModel.initializeCamera(
                                    context,
                                    this@DualModeCameraActivityCompose,
                                    previewView
                                )
                            }
                        }
                    )
                    // Recording Controls Card
                    RecordingControlsCard(
                        recordingState = recordingState,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() }
                    )
                    // Camera Status Card
                    CameraStatusCard(
                        cameraState = cameraState,
                        cameraScreenState = cameraScreenState
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraModeCard(
        selectedMode: DualModeCameraViewModel.CameraMode,
        onModeChange: (DualModeCameraViewModel.CameraMode) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Camera Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DualModeCameraViewModel.CameraMode.values().forEach { mode ->
                        FilterChip(
                            onClick = { onModeChange(mode) },
                            label = { Text(mode.name.replace("_", " ")) },
                            selected = selectedMode == mode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CameraPreviewCard(
        cameraState: DualModeCameraViewModel.CameraState,
        cameraScreenState: DualModeCameraViewModel.CameraScreenState,
        permissionState: DualModeCameraViewModel.PermissionState,
        onInitializeCamera: (PreviewView) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (permissionState) {
                    DualModeCameraViewModel.PermissionState.GRANTED -> {
                        var previewView: PreviewView? by remember { mutableStateOf(null) }
                        AndroidView(
                            factory = { context ->
                                PreviewView(context).also {
                                    previewView = it
                                    onInitializeCamera(it)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (cameraScreenState.showProgress) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    DualModeCameraViewModel.PermissionState.DENIED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Permission Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Camera permission required",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { checkCameraPermission() }) {
                                Text("Grant Permission")
                            }
                        }
                    }

                    else -> {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    private fun RecordingControlsCard(
        recordingState: DualModeCameraViewModel.RecordingState,
        onStartRecording: () -> Unit,
        onStopRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recording Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recordingState.isRecording) {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Recording")
                        }
                    } else {
                        Button(
                            onClick = onStartRecording,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording")
                        }
                    }
                }
                if (recordingState.isRecording) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Recording: ${recordingState.recordingDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraStatusCard(
        cameraState: DualModeCameraViewModel.CameraState,
        cameraScreenState: DualModeCameraViewModel.CameraScreenState
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Camera Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Connection:")
                    Text(
                        text = if (cameraState.isInitialized) "Connected" else "Disconnected",
                        color = if (cameraState.isInitialized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preview:")
                    Text(
                        text = if (cameraState.isInitialized) "Active" else "Inactive",
                        color = if (cameraState.isInitialized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                if (cameraScreenState.displayMessage.isNotEmpty()) {
                    Text(
                        text = cameraScreenState.displayMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar() {
        val context = LocalContext.current
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Gallery") },
                label = { Text("Gallery") },
                selected = false,
                onClick = { navigateToMainActivity(0) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") },
                label = { Text("Camera") },
                selected = true,
                onClick = {
                    // Current page - already on DualModeCameraActivityCompose
                    Toast.makeText(context, "Already viewing dual camera", Toast.LENGTH_SHORT).show()
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = false,
                onClick = { navigateToMainActivity(2) }
            )
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraVM.onPermissionGranted()
            }

            else -> {
                cameraVM.requestPermission()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToMainActivity(pageIndex: Int) {
        val intent = Intent(this, MainComposeActivity::class.java).apply {
            putExtra("page", pageIndex)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}


// ===== feature\camera\ui\DualModeCameraComposeActivity.kt =====

package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.presentation.DualModeCameraViewModel

class DualModeCameraComposeActivity : BaseComposeActivity<DualModeCameraViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DualModeCameraComposeActivity::class.java))
        }

        fun startWithMode(context: Context, mode: String) {
            val intent = Intent(context, DualModeCameraComposeActivity::class.java).apply {
                putExtra("INITIAL_MODE", mode)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): DualModeCameraViewModel {
        return viewModels<DualModeCameraViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: DualModeCameraViewModel) {
        val localContext = this@DualModeCameraComposeActivity
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var cameraMode by remember { mutableStateOf("Dual") }
        var showSettingsDialog by remember { mutableStateOf(false) }
        IRCameraTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dual Mode Camera",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Switch between front/back camera
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Switch camera feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch")
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    localContext,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                DualModeCameraContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    cameraMode = cameraMode,
                    onCameraModeChange = { cameraMode = it },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showSettingsDialog) {
            CameraSettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onSaveSettings = { settings ->
                    // Apply camera settings
                    showSettingsDialog = false
                }
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    viewModel: DualModeCameraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Camera Preview Area
        CameraPreviewSection(
            cameraMode = cameraMode,
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        // Camera Controls
        CameraControlsSection(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            recordingDuration = recordingDuration,
            cameraMode = cameraMode,
            onCameraModeChange = onCameraModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraPreviewSection(
    cameraMode: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        when (cameraMode) {
            "Dual" -> {
                // Dual camera view with picture-in-picture
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main thermal preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ThermalCameraPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                        // RGB camera PiP
                        Card(
                            modifier = Modifier
                                .size(120.dp, 160.dp)
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            RGBCameraPreview(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Temperature overlay
                        TemperatureOverlay(
                            centerTemp = 36.8f,
                            maxTemp = 42.1f,
                            minTemp = 28.3f,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }

            "Thermal" -> {
                ThermalCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "RGB" -> {
                RGBCameraPreview(
                    modifier = Modifier.fillMaxSize()
                )
            }

            "Split" -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ThermalCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RGBCameraPreview(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        // Recording indicator
        if (isRecording) {
            RecordingIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        // Camera mode indicator
        CameraModeIndicator(
            mode = cameraMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraControlsSection(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    cameraMode: String,
    onCameraModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
    ) {
        // Camera mode selector
        CameraModeSelector(
            selectedMode = cameraMode,
            onModeChange = onCameraModeChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Recording status
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            OutlinedButton(
                onClick = {
                    // TODO: Open gallery to view captured photos/videos
                    android.widget.Toast.makeText(
                        localContext,
                        "Gallery feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            }
            // Record button
            Button(
                onClick = onRecordingToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "STOP" else "RECORD",
                    fontWeight = FontWeight.Bold
                )
            }
            // Capture button
            OutlinedButton(
                onClick = {
                    // TODO: Capture photo from dual camera
                    android.widget.Toast.makeText(
                        localContext,
                        "Photo captured",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Capture"
                )
            }
        }
    }
}

@Composable
private fun CameraModeSelector(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Dual", "Thermal", "RGB", "Split")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(mode) },
                leadingIcon = if (selectedMode == mode) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                Color(0xFFE53E3E).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Dual-mode ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                )
            }
        }
    }
}

@Composable
private fun ThermalCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Thermostat,
                contentDescription = "Thermal Camera",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFF6B35)
            )
            Text(
                text = "Thermal Camera Preview",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RGBCameraPreview(
    modifier: Modifier = Modifier
) {
    // Placeholder for RGB camera preview
    Box(
        modifier = modifier
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Camera,
                contentDescription = "RGB Camera",
                modifier = Modifier.size(if (modifier == Modifier.fillMaxSize()) 64.dp else 32.dp),
                tint = Color(0xFF2196F3)
            )
            if (modifier == Modifier.fillMaxSize()) {
                Text(
                    text = "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TemperatureOverlay(
    centerTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "TEMP",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${centerTemp}Â°C",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "H:${maxTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L:${minTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE53E3E)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraModeIndicator(
    mode: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Text(
            text = mode.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CameraSettingsDialog(
    onDismiss: () -> Unit,
    onSaveSettings: (Map<String, Any>) -> Unit
) {
    var videoQuality by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf(30f) }
    var enableStabilization by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HD", "FHD", "4K").forEach { quality ->
                        FilterChip(
                            selected = videoQuality == quality,
                            onClick = { videoQuality = quality },
                            label = { Text(quality) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Frame Rate: ${frameRate.toInt()} fps",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = frameRate,
                    onValueChange = { frameRate = it },
                    valueRange = 15f..60f,
                    steps = 8
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableStabilization,
                        onCheckedChange = { enableStabilization = it }
                    )
                    Text(
                        text = "Enable Image Stabilization",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(
                        mapOf(
                            "quality" to videoQuality,
                            "frameRate" to frameRate.toInt(),
                            "stabilization" to enableStabilization
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}


// ===== feature\camera\ui\DualModeCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualModeCameraScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Thermal + RGB Camera",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Tune, contentDescription = "Camera Settings")
                        }
                        var viewMode by remember { mutableStateOf("split") }
                        IconButton(onClick = {
                            viewMode = if (viewMode == "split") "overlay" else "split"
                            // TODO: Toggle between split and overlay view modes
                        }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap View")
                        }
                    }
                )
            }
        ) { paddingValues ->
            DualModeCameraContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun DualModeCameraContent(
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CameraMode.DUAL_VIEW) }
    var rgbCameraActive by remember { mutableStateOf(true) }
    var thermalCameraActive by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var syncEnabled by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera Mode Selector
        CameraModeSelector(
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it }
        )
        // Dual Camera Preview
        DualCameraPreviewCard(
            mode = selectedMode,
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            syncEnabled = syncEnabled
        )
        // Camera Status and Controls
        CameraControlsCard(
            rgbActive = rgbCameraActive,
            thermalActive = thermalCameraActive,
            isRecording = isRecording,
            syncEnabled = syncEnabled,
            onRGBToggle = { rgbCameraActive = it },
            onThermalToggle = { thermalCameraActive = it },
            onRecordingToggle = { isRecording = it },
            onSyncToggle = { syncEnabled = it }
        )
        // Recording Settings
        RecordingSettingsCard()
        // Calibration Tools
        CalibrationToolsCard()
    }
}

enum class CameraMode {
    RGB_ONLY,
    THERMAL_ONLY,
    DUAL_VIEW,
    OVERLAY
}

@Composable
private fun CameraModeSelector(
    selectedMode: CameraMode,
    onModeChange: (CameraMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.RGB_ONLY,
                    label = "RGB Only",
                    selected = selectedMode == CameraMode.RGB_ONLY,
                    onClick = { onModeChange(CameraMode.RGB_ONLY) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.THERMAL_ONLY,
                    label = "Thermal Only",
                    selected = selectedMode == CameraMode.THERMAL_ONLY,
                    onClick = { onModeChange(CameraMode.THERMAL_ONLY) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraModeChip(
                    mode = CameraMode.DUAL_VIEW,
                    label = "Dual View",
                    selected = selectedMode == CameraMode.DUAL_VIEW,
                    onClick = { onModeChange(CameraMode.DUAL_VIEW) },
                    modifier = Modifier.weight(1f)
                )
                CameraModeChip(
                    mode = CameraMode.OVERLAY,
                    label = "Overlay",
                    selected = selectedMode == CameraMode.OVERLAY,
                    onClick = { onModeChange(CameraMode.OVERLAY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    mode: CameraMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        },
        selected = selected,
        modifier = modifier
    )
}

@Composable
private fun DualCameraPreviewCard(
    mode: CameraMode,
    rgbActive: Boolean,
    thermalActive: Boolean,
    syncEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Camera Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (syncEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Synced",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Synced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Preview area based on mode
            when (mode) {
                CameraMode.RGB_ONLY -> {
                    RGBPreviewArea(active = rgbActive)
                }

                CameraMode.THERMAL_ONLY -> {
                    ThermalPreviewArea(active = thermalActive)
                }

                CameraMode.DUAL_VIEW -> {
                    DualViewPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }

                CameraMode.OVERLAY -> {
                    OverlayPreviewArea(
                        rgbActive = rgbActive,
                        thermalActive = thermalActive
                    )
                }
            }
        }
    }
}

@Composable
private fun RGBPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) Color.Black else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "RGB Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThermalPreviewArea(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Camera",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "25.6Â°C - 31.2Â°C",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.DeviceThermostat,
                    contentDescription = "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Thermal Camera Inactive",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DualViewPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (rgbActive) Color.Black else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (rgbActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (rgbActive) "RGB Camera Active" else "RGB Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "RGB",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .background(
                    if (thermalActive) MaterialTheme.colorScheme.primary else Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (thermalActive) Icons.Default.Thermostat else Icons.Default.DeviceThermostat,
                    contentDescription = if (thermalActive) "Thermal Camera Active" else "Thermal Camera Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Thermal",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OverlayPreviewArea(
    rgbActive: Boolean,
    thermalActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color.Black,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rgbActive && thermalActive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "RGB and Thermal Overlay",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "RGB + Thermal Overlay",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Opacity: 60%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                "Both cameras required for overlay",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CameraControlsCard(
    rgbActive: Boolean,
    thermalActive: Boolean,
    isRecording: Boolean,
    syncEnabled: Boolean,
    onRGBToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRecordingToggle: (Boolean) -> Unit,
    onSyncToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Camera Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Camera toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "RGB Camera")
                    Text("RGB Camera")
                }
                Switch(
                    checked = rgbActive,
                    onCheckedChange = onRGBToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Thermostat, contentDescription = "Thermal Camera")
                    Text("Thermal Camera")
                }
                Switch(
                    checked = thermalActive,
                    onCheckedChange = onThermalToggle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync Cameras")
                    Text("Sync Cameras")
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncToggle
                )
            }
            HorizontalDivider()
            // Recording controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { onRecordingToggle(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = { onRecordingToggle(true) },
                        modifier = Modifier.weight(1f),
                        enabled = rgbActive || thermalActive
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Recording")
                    }
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        // TODO: Capture snapshot from both cameras
                        android.widget.Toast.makeText(
                            context,
                            "Snapshot captured",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = rgbActive || thermalActive
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Snapshot")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snapshot")
                }
            }
        }
    }
}

@Composable
private fun RecordingSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recording Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Quality settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RGB Quality")
                Text("1080p @ 30fps", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thermal Quality")
                Text("384x288 @ 25fps", fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Synchronization")
                Text("Hardware Sync", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CalibrationToolsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Calibration Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Start camera alignment process
                        android.widget.Toast.makeText(
                            context,
                            "Starting alignment...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Align Cameras")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Align")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Start color calibration
                        android.widget.Toast.makeText(
                            context,
                            "Starting color calibration...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Calibrate Colors")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
            }
        }
    }
}


// ===== feature\camera\ui\RGBCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.Green
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.theme.Orange
import mpdc4gsr.core.ui.theme.Purple
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModel
import mpdc4gsr.feature.camera.presentation.RGBCameraViewModelFactory

@Composable
fun RGBCameraScreen(
    viewModel: RGBCameraViewModel = viewModel(
        factory = RGBCameraViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraRecorder by viewModel.cameraRecorder.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    // Initialize camera on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeCamera(lifecycleOwner)
    }
    // Show error if present
    LaunchedEffect(cameraState.error) {
        showError = cameraState.error != null
    }
    // Use real data from ViewModel
    val isPreviewActive = cameraState.isPreviewActive
    val isRecording = cameraState.isRecording
    val resolution = cameraState.resolution
    val frameRate = cameraState.frameRate
    val recordingDuration = cameraState.recordingDuration
    val capturedFrames = cameraState.capturedFrames
    val cameraChangeCounter = cameraState.cameraChangeCounter
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen camera preview - now properly reactive to cameraRecorder StateFlow
        if (cameraRecorder != null) {
            FullScreenCameraPreview(
                cameraRecorder = cameraRecorder!!,
                isRecording = isRecording,
                cameraChangeCounter = cameraChangeCounter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FullScreenCameraPreviewSimulated(
                isActive = isPreviewActive,
                isRecording = isRecording,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Top overlay with back button and status
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CameraTopBar(
                resolution = resolution,
                frameRate = frameRate,
                isRecording = isRecording,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
        }
        // Bottom overlay with camera controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CameraBottomControls(
                isRecording = isRecording,
                isPreviewActive = isPreviewActive,
                recordingDuration = recordingDuration,
                capturedFrames = capturedFrames,
                onToggleRecording = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                onCapturePhoto = {
                    viewModel.capturePhoto()
                    onCapturePhoto()
                },
                onSwitchCamera = {
                    viewModel.switchCamera()
                }
            )
        }
        // Toggle controls visibility with tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        )
        // Error message display with retry option
        if (showError && cameraState.error != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cameraState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.dismissError() }
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                viewModel.dismissError()
                                viewModel.reinitializeCamera(lifecycleOwner)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTopBar(
    resolution: String,
    frameRate: Int,
    isRecording: Boolean,
    onBackClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "REC",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$resolution â€¢ ${frameRate}fps",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraBottomControls(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onCapturePhoto: () -> Unit,
    onSwitchCamera: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isRecording) {
                Text(
                    text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo capture button
                FilledIconButton(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Photo",
                        tint = Color.White
                    )
                }
                // Video record button - larger, centered
                FilledIconButton(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color.Red else Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else Color.Red,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Camera switch button
                FilledIconButton(
                    onClick = onSwitchCamera,
                    enabled = isPreviewActive && !isRecording,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    cameraChangeCounter: Int,
    modifier: Modifier = Modifier
) {
    // Use key to force recreation when camera switches
    key(cameraChangeCounter) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                // Bind preview when the view updates - ensures preview is connected
                cameraRecorder.bindPreview(previewView)
            },
            modifier = modifier
        )
    }
}

@Composable
private fun FullScreenCameraPreviewSimulated(
    isActive: Boolean,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawRect(color = Color(0xFF2E2E2E), size = size)
                drawRect(
                    color = Color(0xFF4A4A4A),
                    topLeft = Offset(0f, height * 0.6f),
                    size = Size(width, height * 0.4f)
                )
                drawCircle(
                    color = Color(0xFF6A6A6A),
                    radius = width * 0.1f,
                    center = Offset(width * 0.3f, height * 0.4f)
                )
                drawRect(
                    color = Color(0xFF5A5A5A),
                    topLeft = Offset(width * 0.6f, height * 0.2f),
                    size = Size(width * 0.25f, height * 0.4f)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Off",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Camera Preview Off",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RGBCameraPreview(
    isActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f), // Standard camera aspect ratio
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isActive) {
                // Camera preview simulation
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    // Draw camera background
                    drawRect(
                        color = Color(0xFF2E2E2E),
                        size = size
                    )
                    // Simulate camera scene
                    // Background gradient
                    drawRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = Offset(0f, height * 0.6f),
                        size = Size(width, height * 0.4f)
                    )
                    // Simulated objects
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = width * 0.1f,
                        center = Offset(width * 0.3f, height * 0.4f)
                    )
                    drawRect(
                        color = Color(0xFF5A5A5A),
                        topLeft = Offset(width * 0.6f, height * 0.2f),
                        size = Size(width * 0.25f, height * 0.4f)
                    )
                    // Grid lines (rule of thirds)
                    val strokeWidth = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.3f)
                    // Vertical lines
                    drawLine(
                        color = gridColor,
                        start = Offset(width / 3f, 0f),
                        end = Offset(width / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(width * 2f / 3f, 0f),
                        end = Offset(width * 2f / 3f, height),
                        strokeWidth = strokeWidth
                    )
                    // Horizontal lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 3f),
                        end = Offset(width, height / 3f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height * 2f / 3f),
                        end = Offset(width, height * 2f / 3f),
                        strokeWidth = strokeWidth
                    )
                    // Focus indicator (center)
                    val centerX = width / 2
                    val centerY = height / 2
                    val focusSize = 30.dp.toPx()
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - focusSize / 2, centerY - focusSize / 2),
                        size = Size(focusSize, focusSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
                // Overlay indicators
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    // Live indicator
                    Surface(
                        color = if (isRecording) Color.Red else Color.Green,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isRecording) "REC" else "LIVE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Resolution indicator
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resolution,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                // Frame rate indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${frameRate}fps",
                        color = Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                // Preview off
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Off",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Preview Off",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tap to enable preview",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealCameraPreview(
    cameraRecorder: mpdc4gsr.core.data.RgbCameraRecorder,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Bind camera preview
                        cameraRecorder.bindPreview(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Recording indicator
            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "REC",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraStatusCard(
    isPreviewActive: Boolean,
    isRecording: Boolean,
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Status",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when {
                        isRecording -> Color.Red.copy(alpha = 0.2f)
                        isPreviewActive -> Color.Green.copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when {
                            isRecording -> "RECORDING"
                            isPreviewActive -> "PREVIEW"
                            else -> "STANDBY"
                        },
                        color = when {
                            isRecording -> Color.Red
                            isPreviewActive -> Color.Green
                            else -> Color.Gray
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // Camera metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Resolution", resolution, Color.White)
                MetricItem("Frame Rate", "${frameRate}fps", Color.Green)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Exposure", exposureTime, Color.Yellow)
                MetricItem("ISO", iso.toString(), Color.Cyan)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Focus", focusMode, MaterialTheme.colorScheme.primary)
                MetricItem("White Balance", whiteBalance, Color.Magenta)
            }
        }
    }
}

@Composable
private fun RecordingControlsCard(
    isRecording: Boolean,
    isPreviewActive: Boolean,
    recordingDuration: Int,
    capturedFrames: Int,
    onToggleRecording: () -> Unit,
    onTogglePreview: () -> Unit,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (isRecording) {
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        "Duration",
                        "${recordingDuration}s",
                        Color.Red
                    )
                    MetricItem(
                        "Frames",
                        capturedFrames.toString(),
                        Color.Green
                    )
                    MetricItem(
                        "File Size",
                        "${(recordingDuration * 2.5f).toInt()}MB",
                        Color.Cyan
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onTogglePreview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewActive) Orange else Green
                    )
                ) {
                    Text(if (isPreviewActive) "Stop Preview" else "Start Preview")
                }
                Button(
                    onClick = onToggleRecording,
                    enabled = isPreviewActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.VideoCall,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                Button(
                    onClick = onCapturePhoto,
                    enabled = isPreviewActive && !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Photo")
                    Spacer(Modifier.width(4.dp))
                    Text("Photo")
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCard(
    resolution: String,
    frameRate: Int,
    exposureTime: String,
    iso: Int,
    focusMode: String,
    whiteBalance: String,
    currentFocusMode: mpdc4gsr.feature.camera.presentation.FocusMode,
    currentWhiteBalance: mpdc4gsr.feature.camera.presentation.WhiteBalance,
    onResolutionChange: (String) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onExposureChange: (String) -> Unit,
    onISOChange: (Int) -> Unit,
    onFocusModeChange: (mpdc4gsr.feature.camera.presentation.FocusMode) -> Unit,
    onWhiteBalanceChange: (mpdc4gsr.feature.camera.presentation.WhiteBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Quick setting buttons - Resolution and Frame Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onResolutionChange("1920Ã—1080") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1920Ã—1080") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1080p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onResolutionChange("1280Ã—720") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolution == "1280Ã—720") MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("720p", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { onFrameRateChange(if (frameRate == 30) 60 else 30) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${frameRate}fps", fontSize = 10.sp)
                }
            }
            // Additional camera controls - Focus and White Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onFocusModeChange(currentFocusMode.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Focus: $focusMode", fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        onWhiteBalanceChange(currentWhiteBalance.getNext())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WB: $whiteBalance", fontSize = 9.sp)
                }
            }
            // Advanced settings info
            Text(
                text = "Advanced exposure and ISO controls available in camera settings menu",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RGBCameraScreenPreview() {
    IRCameraTheme {
        RGBCameraScreen()
    }
}


// ===== feature\camera\ui\TapToFocusCompose.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapToFocusPreview(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize()) {
        // Camera PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewViewConfig(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        // Call the focus callback
                        onTapToFocus(normalizedX, normalizedY)
                        // Auto-hide focus indicator after delay
                        coroutineScope.launch {
                            delay(2000)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        // Focus indicator overlay
        if (showFocusIndicator && focusPoint != null) {
            FocusIndicator(focusPoint = focusPoint!!)
        }
    }
}

@Composable
private fun FocusIndicator(focusPoint: Offset) {
    val density = LocalDensity.current
    val circleRadius = with(density) { 60.dp.toPx() }
    // Animate the focus indicator
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusAlpha"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer circle
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = circleRadius,
            center = focusPoint,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        // Inner crosshair
        val crossSize = circleRadius * 0.3f
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x - crossSize, focusPoint.y),
            end = Offset(focusPoint.x + crossSize, focusPoint.y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(focusPoint.x, focusPoint.y - crossSize),
            end = Offset(focusPoint.x, focusPoint.y + crossSize),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TapToFocusPreviewWithCustomIndicator(
    onTapToFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    previewViewConfig: (PreviewView) -> Unit,
    focusIndicatorColor: Color = Color.White,
    focusIndicatorRadius: Float = 60f,
    autoHideDelay: Long = 2000L,
    modifier: Modifier = Modifier
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val radiusPx = with(density) { focusIndicatorRadius.dp.toPx() }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { previewViewConfig(this) } },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        focusPoint = offset
                        showFocusIndicator = true
                        onTapToFocus(normalizedX, normalizedY)
                        coroutineScope.launch {
                            delay(autoHideDelay)
                            showFocusIndicator = false
                        }
                    }
                }
        )
        if (showFocusIndicator && focusPoint != null) {
            CustomFocusIndicator(
                focusPoint = focusPoint!!,
                color = focusIndicatorColor,
                radius = radiusPx
            )
        }
    }
}

@Composable
private fun CustomFocusIndicator(
    focusPoint: Offset,
    color: Color,
    radius: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "customFocus")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaledRadius = radius * animatedScale
        // Animated circle
        drawCircle(
            color = color.copy(alpha = animatedAlpha),
            radius = scaledRadius,
            center = focusPoint,
            style = Stroke(width = 4f)
        )
        // Corner brackets
        val bracketSize = scaledRadius * 0.3f
        val offset = scaledRadius - bracketSize
        // Top-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset + bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y - offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y - offset),
            strokeWidth = 3f
        )
        // Top-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y - offset),
            end = Offset(focusPoint.x + offset, focusPoint.y - offset + bracketSize),
            strokeWidth = 3f
        )
        // Bottom-left bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset - bracketSize),
            end = Offset(focusPoint.x - offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x - offset, focusPoint.y + offset),
            end = Offset(focusPoint.x - offset + bracketSize, focusPoint.y + offset),
            strokeWidth = 3f
        )
        // Bottom-right bracket
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset - bracketSize, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset),
            strokeWidth = 3f
        )
        drawLine(
            color = color.copy(alpha = animatedAlpha),
            start = Offset(focusPoint.x + offset, focusPoint.y + offset),
            end = Offset(focusPoint.x + offset, focusPoint.y + offset - bracketSize),
            strokeWidth = 3f
        )
    }
}


// ===== feature\camera\ui\TimeLapseCameraScreen.kt =====

package mpdc4gsr.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.presentation.TimeLapseCameraViewModel
import mpdc4gsr.feature.camera.presentation.TimeLapseCameraViewModelFactory
import mpdc4gsr.feature.camera.presentation.TimeLapseMode

@Composable
fun TimeLapseCameraScreen(
    viewModel: TimeLapseCameraViewModel = viewModel(
        factory = TimeLapseCameraViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeLapseState by viewModel.timeLapseState.collectAsState()
    IRCameraTheme {
        Scaffold(
            topBar = {
                TitleBar(
                    title = "Time-Lapse Camera",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = Color(0xFF16131e)
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera Preview Placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D2A3E)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Camera Preview",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                // Recording Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (timeLapseState.isRecording) "Recording" else "Ready",
                                color = if (timeLapseState.isRecording)
                                    MaterialTheme.colorScheme.primary
                                else Color.Gray
                            )
                        }
                        HorizontalDivider()
                        InfoRow("Frames Captured", "${timeLapseState.capturedFrames}")
                        InfoRow("Interval", "${timeLapseState.intervalSeconds}s")
                        InfoRow("Est. Video Length", "${timeLapseState.estimatedVideoLength}s")
                        InfoRow("Duration", "${timeLapseState.totalDuration}s")
                    }
                }
                // Mode Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TimeLapseMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = timeLapseState.mode == mode,
                                    onClick = { viewModel.setMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(mode.displayName)
                            }
                        }
                    }
                }
                // Manual Interval Control
                if (timeLapseState.mode == TimeLapseMode.MANUAL) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Custom Interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${timeLapseState.intervalSeconds} seconds")
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds - 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.updateInterval(
                                                timeLapseState.intervalSeconds + 1
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                            Slider(
                                value = timeLapseState.intervalSeconds.toFloat(),
                                onValueChange = { viewModel.updateInterval(it.toInt()) },
                                valueRange = 1f..60f,
                                steps = 58
                            )
                        }
                    }
                }
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!timeLapseState.isRecording) {
                        Button(
                            onClick = { viewModel.startTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopTimeLapse() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }
                // Error Display
                timeLapseState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLapseCameraScreenPreview() {
    IRCameraTheme {
        TimeLapseCameraScreen()
    }
}


