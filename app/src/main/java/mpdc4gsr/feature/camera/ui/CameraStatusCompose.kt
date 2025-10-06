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
