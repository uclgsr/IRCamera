package mpdc4gsr.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.ui.components.TitleBar
import mpdc4gsr.ui.components.settings.SettingsCard
import mpdc4gsr.ui.components.settings.SettingsRow
import mpdc4gsr.ui.theme.IRCameraTheme

@Composable
fun AppInfoScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "App Information",
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
            // App Icon and Name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "IR Camera",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-Modal Sensor Platform",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            // Version Information
            SettingsCard(
                title = "Version Information",
                icon = Icons.Default.Info
            ) {
                SettingsRow(
                    label = "Version",
                    value = "1.0.0"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Build Number",
                    value = "2024.01.001"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Release Date",
                    value = "January 2024"
                )
            }
            // Developer Information
            SettingsCard(
                title = "Developer",
                icon = Icons.Default.Code
            ) {
                SettingsRow(
                    label = "Organization",
                    value = "UCL GSR"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Project",
                    value = "Multi-Modal Data Collection"
                )
            }
            // Legal
            SettingsCard(
                title = "Legal",
                icon = Icons.Default.Gavel
            ) {
                Text(
                    text = "© 2024 UCL GSR. All rights reserved.\n\nThis application is for research purposes only.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppInfoScreenPreview() {
    IRCameraTheme {
        AppInfoScreen()
    }
}
