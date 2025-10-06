package mpdc4gsr.feature.network.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun ConnectScreen(
    onDeviceSelected: (ConnectedDevice) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Sample device types - will be replaced with actual device data
    val deviceTypes = remember {
        listOf(
            ConnectedDevice("TC001", "TOPDON TC001 Thermal Camera", true),
            ConnectedDevice("TC007", "TOPDON TC007 Thermal Camera", false)
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title bar replacing TitleView
        TitleBar(
            title = "Connect Device", // Match @string/tc_connect_device
            showBackButton = true,
            onBackClick = onBackClick
        )
        // Tips text with matching margins and styling
        Text(
            text = "Select your thermal camera device to connect", // Match @string/tc_connect_tips 
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(
                    start = 20.dp, // Match layout_marginStart
                    end = 20.dp, // Match layout_marginEnd  
                    top = 30.dp // Match layout_marginTop
                )
        )
        // Device list replacing RecyclerView
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp), // Match layout_marginTop
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deviceTypes) { device ->
                DeviceItem(
                    device = device,
                    onSelected = { onDeviceSelected(device) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: ConnectedDevice,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelected,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                Text(
                    text = if (device.isConnected) "Connected" else "Not connected",
                    color = if (device.isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            // Connection status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (device.isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
            ) {}
        }
    }
}

data class ConnectedDevice(
    val id: String,
    val name: String,
    val isConnected: Boolean
)
@Preview(showBackground = true)
@Composable
private fun ConnectScreenPreview() {
    IRCameraTheme {
        ConnectScreen()
    }
}