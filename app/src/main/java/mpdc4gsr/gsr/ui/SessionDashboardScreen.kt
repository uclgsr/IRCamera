package mpdc4gsr.gsr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.model.SessionSnapshot

@Composable
fun SessionDashboardScreen(
    sessionState: StateFlow<SessionSnapshot?>,
    onStartSimulation: () -> Unit,
    onStop: () -> Unit,
) {
    val snapshot by sessionState.collectAsState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "GSR Multi-Modal Recorder",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onStartSimulation) {
                    Text("Start Simulation Session")
                }
                Button(onClick = onStop) {
                    Text("Stop Session")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            snapshot?.let { state ->
                SessionSummary(state)
                Spacer(modifier = Modifier.height(16.dp))
                RecorderStatusList(state.recorderStates)
                Spacer(modifier = Modifier.height(16.dp))
                DeviceList(state.connectedDevices)
            } ?: run {
                Text("No session active")
            }
        }
    }
}

@Composable
private fun SessionSummary(snapshot: SessionSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Session ID: ${snapshot.sessionId}", fontWeight = FontWeight.Bold)
            Text("Label: ${snapshot.label}")
            Text("Elapsed: ${snapshot.elapsedMillis / 1000}s")
            Text("Recording: ${snapshot.isRecording}")
            Text("Timeline offset: ${snapshot.globalTimeline.offsetMillis.formatMs()} ms")
        }
    }
}

@Composable
private fun RecorderStatusList(recorderStates: Map<RecorderKind, RecorderState>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Modalities", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            recorderStates.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(entry.key.name)
                    Text(entry.value.name)
                }
                if (index < recorderStates.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceList(devices: List<DeviceDescriptor>) {
    Text("Devices", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(devices) { device ->
            DeviceCard(device)
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceDescriptor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(device.displayName, fontWeight = FontWeight.Bold)
            Text("Type: ${device.type}")
            Text("State: ${device.connectionState}")
            device.shimmerMacAddress?.let { Text("MAC: $it") }
            device.batteryPercent?.let { Text("Battery: $it%") }
            device.timeOffsetMillis?.let { Text("Offset: ${it.formatMs()} ms") }
        }
    }
}

private fun Double.formatMs(): String = String.format("%.2f", this)
