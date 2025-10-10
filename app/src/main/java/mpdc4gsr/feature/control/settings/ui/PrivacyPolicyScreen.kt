package mpdc4gsr.feature.control.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@Composable
fun PrivacyPolicyScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Privacy Policy",
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
            Text(
                text = "Last Updated: January 2024",
                color = Color.Gray,
                fontSize = 14.sp
            )
            PolicySection(
                title = "Data Collection",
                content = "This application collects multi-modal sensor data including GSR (Galvanic Skin Response), thermal imaging, and RGB camera data for research purposes. All data is collected with explicit user consent."
            )
            PolicySection(
                title = "Data Storage",
                content = "Collected data is stored locally on the device and can be exported by the user. No data is transmitted to external servers without explicit user action."
            )
            PolicySection(
                title = "Data Usage",
                content = "Data collected through this application is intended for research purposes only. Users maintain full control over their data and can delete it at any time."
            )
            PolicySection(
                title = "Third-Party Access",
                content = "No third-party services have access to your data. Data export and sharing are entirely controlled by the user."
            )
            PolicySection(
                title = "Data Security",
                content = "We implement appropriate technical measures to protect your data from unauthorized access, alteration, or destruction."
            )
            PolicySection(
                title = "User Rights",
                content = "You have the right to access, modify, or delete your data at any time. You can also request a copy of all data collected."
            )
            PolicySection(
                title = "Contact",
                content = "For questions or concerns about this privacy policy, please contact the research team at UCL GSR."
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicyScreenPreview() {
    IRCameraTheme {
        PrivacyPolicyScreen()
    }
}

