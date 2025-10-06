package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcaseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Component Showcase",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            ComponentShowcaseContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ComponentShowcaseContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.normal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        // Typography showcase
        TypographyShowcase()
        // Color palette showcase
        ColorPaletteShowcase()
        // Interactive components
        InteractiveComponentsShowcase()
        // Status indicators
        StatusIndicatorsShowcase()
        // Card layouts
        CardLayoutsShowcase()
        // Navigation components
        NavigationComponentsShowcase()
    }
}

@Composable
private fun TypographyShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Typography Styles",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "Headline Large",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "Headline Medium",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Headline Small",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Title Large",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Title Medium",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Body Large - This is the standard body text for longer content and descriptions.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Body Medium - This is commonly used for secondary information.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Body Small - Used for captions and supplementary text.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ColorPaletteShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Thermal Imaging Color Palette",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                ColorSwatch("Primary", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                ColorSwatch("Secondary", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                ColorSwatch("Tertiary", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                ColorSwatch("Error", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                ColorSwatch("Background", MaterialTheme.colorScheme.background, Modifier.weight(1f))
                ColorSwatch("Surface", MaterialTheme.colorScheme.surface, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            name,
            modifier = Modifier
                .padding(Spacing.small)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
private fun InteractiveComponentsShowcase() {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    var switchState by remember { mutableStateOf(true) }
    var selectedChip by remember { mutableStateOf("Option 1") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Interactive Components",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Button")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Outlined")
                }
                TextButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Text")
                }
            }
            // Slider
            Column {
                Text("Slider: ${(sliderValue * 100).toInt()}%")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Switch Control")
                Switch(
                    checked = switchState,
                    onCheckedChange = { switchState = it }
                )
            }
            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                listOf("Option 1", "Option 2", "Option 3").forEach { option ->
                    FilterChip(
                        onClick = { selectedChip = option },
                        label = { Text(option) },
                        selected = selectedChip == option
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Status Indicators",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Progress indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text("Linear Progress")
                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Circular Progress")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.normal)
                ) {
                    CircularProgressIndicator(
                        progress = { 0.75f }
                    )
                    CircularProgressIndicator()
                }
            }
            // Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Badges:")
                Badge { Text("New") }
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) { Text("Error") }
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) { Text("Warning") }
            }
            // Status icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusIcon(Icons.Default.CheckCircle, "Success", MaterialTheme.colorScheme.primary)
                StatusIcon(Icons.Default.Warning, "Warning", MaterialTheme.colorScheme.tertiary)
                StatusIcon(Icons.Default.Error, "Error", MaterialTheme.colorScheme.error)
                StatusIcon(Icons.Default.Info, "Info", MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(Spacing.large)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CardLayoutsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Card Layouts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Information card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Component Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Information Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "This is an example of an information card layout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Action card
            Card(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Touch Interaction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Clickable Action Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "This card responds to tap interactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Component",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationComponentsShowcase() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                "Navigation Components",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "The unified navigation system provides:",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                NavigationFeature("Type-safe navigation routes")
                NavigationFeature("Smooth page transitions")
                NavigationFeature("Deep linking support")
                NavigationFeature("State preservation")
                NavigationFeature("Back stack management")
            }
            Text(
                "All navigation is handled through the UnifiedNavigation system, " +
                        "providing consistent behavior across the entire application.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationFeature(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Feature Available",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.normal)
        )
        Text(
            feature,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}