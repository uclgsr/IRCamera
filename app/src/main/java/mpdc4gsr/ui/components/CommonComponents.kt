package mpdc4gsr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Common Compose components used across migrated Activities.
 * Provides consistent styling and behavior patterns.
 */
object CommonComponents {

    /**
     * Standard top app bar with consistent theming
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun IRCameraTopAppBar(
        title: String,
        onNavigationClick: () -> Unit,
        showNavigationIcon: Boolean = true
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (showNavigationIcon) {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF16131E),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }

    /**
     * Standard loading indicator
     */
    @Composable
    fun LoadingIndicator(
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF16131E)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    /**
     * Standard error screen with retry button
     */
    @Composable
    fun ErrorScreen(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF16131E))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                color = Color(0x80FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Retry",
                    color = Color.White
                )
            }
        }
    }

    /**
     * Standard section header
     */
    @Composable
    fun SectionHeader(
        text: String,
        modifier: Modifier = Modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.padding(vertical = 8.dp)
        )
    }

    /**
     * Standard card container matching app theme
     */
    @Composable
    fun IRCameraCard(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            content()
        }
    }
}