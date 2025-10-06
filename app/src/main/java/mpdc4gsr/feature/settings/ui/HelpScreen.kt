package mpdc4gsr.feature.settings.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun HelpScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Help & Support",
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
            // Quick Start Guide
            SettingsCard(
                title = "Quick Start Guide",
                icon = Icons.AutoMirrored.Filled.MenuBook
            ) {
                Text(
                    text = "1. Connect your sensors (GSR, Thermal Camera)\n" +
                            "2. Configure sensor settings\n" +
                            "3. Start recording\n" +
                            "4. Export your data",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            // FAQ
            SettingsCard(
                title = "Frequently Asked Questions",
                icon = Icons.Default.QuestionAnswer
            ) {
                FAQItem(
                    question = "How do I connect the GSR sensor?",
                    answer = "Enable Bluetooth and scan for devices in Network Settings."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FAQItem(
                    question = "How do I calibrate the thermal camera?",
                    answer = "Go to Settings > Calibration and follow the on-screen instructions."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FAQItem(
                    question = "Where is my data stored?",
                    answer = "Data is stored locally on your device in the configured storage location."
                )
            }
            // Support Contact
            SettingsCard(
                title = "Technical Support",
                icon = Icons.Default.Support
            ) {
                SettingsRow(
                    label = "Email",
                    value = "support@uclgsr.ac.uk"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Documentation",
                    value = "docs.uclgsr.ac.uk"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Open email client with support email pre-filled
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:support@uclgsr.ac.uk")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "IRCamera App Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Email client not available
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support")
                }
            }
            // Troubleshooting
            SettingsCard(
                title = "Troubleshooting",
                icon = Icons.Default.Build
            ) {
                Text(
                    text = "If you encounter issues:\n\n" +
                            "• Check device connections\n" +
                            "• Restart the application\n" +
                            "• Run diagnostics\n" +
                            "• Check system logs",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
@Composable
private fun FAQItem(
    question: String,
    answer: String
) {
    Column {
        Text(
            text = question,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    IRCameraTheme {
        HelpScreen()
    }
}
