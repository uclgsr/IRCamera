package mpdc4gsr.feature.control.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.Spacing
import mpdc4gsr.core.designsystem.components.common.NavigationBreadcrumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("About") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        NavigationBreadcrumb(
            currentScreen = "About",
            previousScreen = "Settings"
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.normal, Alignment.CenterVertically)
        ) {
            Text(
                text = "IRCamera",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.paddingFromBaseline(top = Spacing.extraLarge)
            )
            Text(
                text = "Version 1.10.000",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
            Text(
                text = "Thermal imaging and GSR sensor data collection application",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
            Text(
                text = "Modernized with Jetpack Compose for enhanced user experience and maintainability.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.paddingFromBaseline(top = Spacing.large)
            )
        }
    }
}

