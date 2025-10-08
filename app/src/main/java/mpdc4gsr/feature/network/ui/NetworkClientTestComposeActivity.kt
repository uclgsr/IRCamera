package mpdc4gsr.feature.network.ui

import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.network.presentation.NetworkTestViewModel

@AndroidEntryPoint
class NetworkClientTestComposeActivity : BaseComposeActivity<NetworkTestViewModel>() {
    override fun createViewModel(): NetworkTestViewModel =
        viewModels<NetworkTestViewModel>().value

    @Composable
    override fun Content(viewModel: NetworkTestViewModel) {
        IRCameraTheme {
            NetworkClientTestScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkClientTestScreen(
    viewModel: NetworkTestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    val port by viewModel.port.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Network Test") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.connect() }) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Disconnect" else "Connect",
                        tint = if (isRunning) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Network Test Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Connection Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("IP Address: $ipAddress")
                    Text("Port: $port")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.connect() },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text("Connect")
                }
                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }
            }
            
            Button(
                onClick = { viewModel.sendTestMessage("Test message") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Test Message")
            }
        }
    }
}
