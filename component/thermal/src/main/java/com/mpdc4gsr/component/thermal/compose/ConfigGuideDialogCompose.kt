package com.mpdc4gsr.component.thermal.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.component.shared.app.compose.theme.Spacing
import com.mpdc4gsr.component.thermal.R

@Composable
fun ConfigGuideDialogCompose(
    isTC007: Boolean,
    initialStep: Int = 1,
    onDismiss: () -> Unit,
    onComplete: () -> Unit = {},
) {
    var currentStep by remember(initialStep) { mutableIntStateOf(initialStep) }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .blur(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (targetState > initialState) 300 else -300 },
                    ) + fadeIn() togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { if (targetState > initialState) -300 else 300 },
                        ) + fadeOut()
                },
                label = "config_step",
            ) { step ->
                when (step) {
                    1 ->
                        ConfigStep1Content(
                            isTC007 = isTC007,
                            onNext = { currentStep = 2 },
                        )

                    2 ->
                        ConfigStep2Content(
                            isTC007 = isTC007,
                            onComplete = {
                                onComplete()
                                onDismiss()
                            },
                        )
                }
            }
        }
    }
}

@Composable
private fun ConfigStep1Content(
    isTC007: Boolean,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.small),
        shape = RoundedCornerShape(Spacing.normal),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.normal),
        ) {
            Text(
                text = "Thermal Configuration Guide",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Step 1: Basic Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_environment)} (-10~${if (isTC007) 50 else 55}°C)",
                description = "Set the ambient temperature for accurate thermal measurements",
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)",
                description = "Configure the distance to target for proper calibration",
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Button(
                onClick = onNext,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.touchTarget),
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ConfigStep2Content(
    isTC007: Boolean,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.small),
        shape = RoundedCornerShape(Spacing.normal),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal),
        ) {
            Text(
                text = "Step 2: Emissivity Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ConfigParameterCard(
                title = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)",
                description = "Select appropriate emissivity value for your target material",
            )
            Text(
                text = "Common Materials",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(getEmissivityPresets(isTC007)) { preset ->
                    EmissivityPresetCard(preset = preset)
                }
            }
            Button(
                onClick = onComplete,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.touchTarget),
            ) {
                Text("I Know")
            }
        }
    }
}

@Composable
private fun ConfigParameterCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmissivityPresetCard(
    preset: EmissivityPreset,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = preset.material,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (preset.description.isNotEmpty()) {
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = preset.emissivity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun getEmissivityPresets(isTC007: Boolean): List<EmissivityPreset> =
    listOf(
        EmissivityPreset("Human Skin", 0.98f, "Human body temperature measurement"),
        EmissivityPreset("Water", 0.96f, "Liquid water surface"),
        EmissivityPreset("Concrete", 0.95f, "Concrete surfaces and walls"),
        EmissivityPreset("Plastic", 0.94f, "Most plastic materials"),
        EmissivityPreset("Wood", 0.90f, "Dry wood surfaces"),
        EmissivityPreset("Paint", 0.90f, "Painted surfaces (non-metallic)"),
        EmissivityPreset("Brick", 0.85f, "Red brick and ceramic"),
        EmissivityPreset("Stainless Steel", 0.16f, "Polished stainless steel"),
        EmissivityPreset("Aluminum", if (isTC007) 0.1f else 0.05f, "Oxidized aluminum"),
        EmissivityPreset("Copper", 0.04f, "Polished copper surface"),
    ).filter {
        it.emissivity >= (if (isTC007) 0.1f else 0.01f) && it.emissivity <= 1.0f
    }

data class EmissivityPreset(
    val material: String,
    val emissivity: Float,
    val description: String = "",
)

@Composable
fun ConfigGuideDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        ConfigGuideDialogCompose(
            isTC007 = false,
            initialStep = 1,
            onDismiss = { showDialog = false },
            onComplete = {
                showDialog = false
            },
        )
    }
}



