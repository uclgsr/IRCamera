package mpdc4gsr.activities

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.viewmodel.BaseViewModel

class CameraNetworkDemoViewModel : BaseViewModel()

/**
 * Camera Integration and Network Demo Activity
 * 
 * Demonstrates the newly implemented Compose activities:
 * - DualModeCameraActivityCompose: Advanced camera integration
 * - DevicePairingActivityCompose: Network device pairing
 * 
 * These represent the latest additions to the Compose migration,
 * focusing on core functionality for camera and network operations.
 */
class CameraNetworkDemoActivity : BaseComposeActivity<CameraNetworkDemoViewModel>() {

    override fun createViewModel(): CameraNetworkDemoViewModel = CameraNetworkDemoViewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: CameraNetworkDemoViewModel) {
        val context = LocalContext.current
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Camera Integration & Network",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Introduction Card
                IntroductionCard()
                
                // Camera Integration Section
                DemoSection(
                    title = "Camera Integration",
                    description = "Advanced dual-mode camera functionality with simultaneous RGB and thermal operation",
                    icon = Icons.Default.CameraAlt,
                    onLaunchClick = {
                        startActivity(Intent(context, DualModeCameraActivityCompose::class.java))
                    }
                )
                
                // Network Pairing Section  
                DemoSection(
                    title = "Network Device Pairing",
                    description = "Real-time device discovery, pairing, and network communication management",
                    icon = Icons.Default.DeviceHub,
                    onLaunchClick = {
                        startActivity(Intent(context, DevicePairingActivityCompose::class.java))
                    }
                )
                
                // Migration Benefits Card
                MigrationBenefitsCard()
            }
        }
    }
    
    @Composable
    private fun IntroductionCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Latest Compose Migration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera Integration and Network functionality have been successfully migrated to Jetpack Compose, bringing modern UI patterns and improved performance to core application features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
    
    @Composable
    private fun DemoSection(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onLaunchClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = onLaunchClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch Demo")
                }
            }
        }
    }
    
    @Composable
    private fun MigrationBenefitsCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upgrade,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Migration Benefits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                val benefits = listOf(
                    "Modern Material Design 3 interface",
                    "Reactive state management with StateFlow",
                    "Improved performance and responsiveness",
                    "Enhanced accessibility support",
                    "Consistent theming and animations",
                    "Simplified maintenance and testing",
                    "Future-proof architecture",
                    "Seamless navigation integration"
                )
                
                benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}