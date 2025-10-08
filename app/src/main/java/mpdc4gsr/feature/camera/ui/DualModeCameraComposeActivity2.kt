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

class DualModeCameraComposeActivity2 : BaseComposeActivity<DualModeCameraViewModel>() {
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
                                    this@DualModeCameraComposeActivity2,
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
                    // Current page - already on DualModeCameraComposeActivity2
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