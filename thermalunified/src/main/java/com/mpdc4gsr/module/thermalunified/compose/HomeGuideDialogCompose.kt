package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.compose.utils.deferAction

@Composable
fun HomeGuideDialogCompose(
    initialStep: Int = 1,
    onNextStep: (step: Int) -> Unit = {},
    onSkinClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentStep by remember(initialStep) { mutableIntStateOf(initialStep) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .blur(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (targetState > initialState) 300 else -300 }
                    ) + fadeIn() togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { if (targetState > initialState) -300 else 300 }
                            ) + fadeOut()
                },
                label = "guide_step"
            ) { step ->
                GuideStepContent(
                    step = step,
                    onNext = {
                        when (step) {
                            1 -> {
                                currentStep = 2
                                onNextStep(1)
                            }

                            2 -> {
                                currentStep = 3
                                onNextStep(2)
                            }

                            3 -> {
                                onNextStep(3)
                                onDismiss()
                            }
                        }
                    },
                    onSkinClick = {
                        onSkinClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun GuideStepContent(
    step: Int,
    onNext: () -> Unit,
    onSkinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> GuideStep1Content(onNext, onSkinClick)
                2 -> GuideStep2Content(onNext, onSkinClick)
                3 -> GuideStep3Content(onNext)
            }
        }
    }
}

@Composable
private fun GuideStep1Content(onNext: () -> Unit, onSkinClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 1: Getting Started",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Welcome to the thermal camera guide. This will help you get started with thermal imaging.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = deferAction { onSkinClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skin Detection")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun GuideStep2Content(onNext: () -> Unit, onSkinClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 2: Camera Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Position the camera properly and adjust the focus for optimal thermal imaging results.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = deferAction { onSkinClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skin Detection")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun GuideStep3Content(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 3: Ready to Go!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You're all set! Start using the thermal camera to capture and analyze thermal images.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = deferAction { onNext() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I Know")
        }
    }
}

@Composable
fun HomeGuideDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        HomeGuideDialogCompose(
            initialStep = 1,
            onNextStep = { step ->
                println("Guide step: $step")
            },
            onSkinClick = {
                println("Skin detection clicked")
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}