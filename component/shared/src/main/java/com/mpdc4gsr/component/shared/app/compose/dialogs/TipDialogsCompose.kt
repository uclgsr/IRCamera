package com.mpdc4gsr.component.shared.app.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TipDialog(
    title: String = "",
    message: String,
    positiveText: String = "Confirm",
    negativeText: String = "Cancel",
    showCancel: Boolean = true,
    showRestartTips: Boolean = false,
    restartTipsText: String = "Device will restart",
    cancelable: Boolean = false,
    onPositive: () -> Unit,
    onNegative: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.72f else 0.5f
    @Suppress("UNCHECKED_CAST")
    Dialog(
        onDismissRequest =
            (
                if (cancelable) {
                    onDismiss
                } else {
                }
            ) as () -> Unit,
        properties =
            DialogProperties(
                dismissOnBackPress = cancelable,
                dismissOnClickOutside = cancelable,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(widthFraction)
                    .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showRestartTips) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restartTipsText,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = {
                                onNegative()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = negativeText,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onPositive()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            text = positiveText,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageDialog(
    @DrawableRes iconRes: Int? = null,
    message: String,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.7f else 0.45f
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(widthFraction)
                    .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
        ) {
            Box {
                Column(
                    modifier =
                        Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (iconRes != null) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Message icon",
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
fun EmissivityDialog(
    title: String = "Emissivity Settings",
    currentValue: Float,
    minValue: Float = 0.1f,
    maxValue: Float = 1.0f,
    onValueChange: (Float) -> Unit,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember { mutableStateOf(currentValue) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.5f
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(widthFraction)
                    .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Text(
                    text = String.format("%.2f", sliderValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = minValue..maxValue,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = String.format("%.1f", minValue),
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                    Text(
                        text = String.format("%.1f", maxValue),
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(sliderValue)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = "Confirm", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


