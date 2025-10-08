package mpdc4gsr.presentation.screens.gsr

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRSettingsScreenPlaceholder(
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("GSR Settings Screen - Use GSRSettingsComposeActivity")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    onNavigateToGSRPlot: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Session Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Session Details Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use SessionDetailComposeActivity for full functionality")
                    Button(onClick = onNavigateToGSRPlot) {
                        Text("View Plot")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GSRPlotScreenPlaceholder(
    sessionId: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Plot", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Plot Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use GSRPlotComposeActivity for full functionality")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRDataViewScreen(
    filePath: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Data View", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Data View Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("File Path: $filePath")
                    Text("Use GSRDataViewComposeActivity for full functionality")
                }
            }
        }
    }
}