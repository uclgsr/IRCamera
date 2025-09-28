package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * ConnectScreen composable - replaces activity_device_type.xml
 * Maintains the same layout structure and spacing as the reference implementation
 */
@Composable
fun ConnectScreen(
    onDeviceSelected: (DeviceType) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Sample device types - will be replaced with actual device data
    val deviceTypes = remember {
        listOf(
            DeviceType("TC001", "TOPDON TC001 Thermal Camera", true),
            DeviceType("TC007", "TOPDON TC007 Thermal Camera", false)
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e)) // Match reference background color
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
            color = Color(0xCCFFFFFF), // Match #ccffffff from XML
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

/**
 * Individual device item component
 * Replaces item view from RecyclerView adapter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: DeviceType,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelected,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A) // Slightly lighter than background
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
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = if (device.isConnected) "Connected" else "Not connected",
                    color = if (device.isConnected) Color(0xFF4CAF50) else Color(0xFFFFFFFF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Connection status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (device.isConnected) Color(0xFF4CAF50) else Color(0xFF757575)
            ) {}
        }
    }
}

/**
 * Data class representing a device type
 * Matches the structure expected by the thermal camera connection logic
 */
data class DeviceType(
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