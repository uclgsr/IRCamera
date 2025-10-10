package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ThermalInputDialogCompose(
    message: String,
    maxTemp: Float = 100f,
    minTemp: Float = 0f,
    maxColor: Color = Color.Red,
    minColor: Color = Color.Blue,
    positiveButtonText: String = "OK",
    negativeButtonText: String = "Cancel",
    onConfirm: (Float, Float, Int, Int) -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    var maxTempInput by remember { mutableStateOf(maxTemp.toString()) }
    var minTempInput by remember { mutableStateOf(minTemp.toString()) }
    val maxTempFocusRequester = remember { FocusRequester() }
    val minTempFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        maxTempFocusRequester.requestFocus()
    }
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
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title
                Text(
                    text = "Thermal Parameters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Message
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Temperature Range Inputs
                Column {
                    // Max Temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .background(maxColor, RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = maxTempInput,
                            onValueChange = { maxTempInput = it },
                            label = { Text("Max Temperature") },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .focusRequester(maxTempFocusRequester),
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next,
                                ),
                            keyboardActions =
                                KeyboardActions(
                                    onNext = { minTempFocusRequester.requestFocus() },
                                ),
                            singleLine = true,
                            suffix = { Text("°C") },
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Min Temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .background(minColor, RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = minTempInput,
                            onValueChange = { minTempInput = it },
                            label = { Text("Min Temperature") },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .focusRequester(minTempFocusRequester),
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done,
                                ),
                            keyboardActions =
                                KeyboardActions(
                                    onDone = {
                                        handleConfirm(
                                            maxTempInput,
                                            minTempInput,
                                            maxColor,
                                            minColor,
                                            onConfirm,
                                            onDismiss,
                                        )
                                    },
                                ),
                            singleLine = true,
                            suffix = { Text("°C") },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(negativeButtonText)
                    }
                    Button(
                        onClick = {
                            handleConfirm(
                                maxTempInput,
                                minTempInput,
                                maxColor,
                                minColor,
                                onConfirm,
                                onDismiss,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(positiveButtonText)
                    }
                }
            }
        }
    }
}

private fun handleConfirm(
    maxTempInput: String,
    minTempInput: String,
    maxColor: Color,
    minColor: Color,
    onConfirm: (Float, Float, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    try {
        val maxTemp = maxTempInput.toFloat()
        val minTemp = minTempInput.toFloat()
        if (maxTemp > minTemp) {
            onConfirm(
                maxTemp,
                minTemp,
                maxColor.toArgb(),
                minColor.toArgb(),
            )
            onDismiss()
        }
    } catch (e: NumberFormatException) {
        // Invalid input - stay open for correction
    }
}

class ThermalInputDialogComposeBuilder {
    private var message: String = ""
    private var maxTemp: Float = 100f
    private var minTemp: Float = 0f
    private var maxColor: Color = Color.Red
    private var minColor: Color = Color.Blue
    private var positiveButtonText: String = "OK"
    private var negativeButtonText: String = "Cancel"
    private var onConfirm: ((Float, Float, Int, Int) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    fun setMessage(message: String): ThermalInputDialogComposeBuilder {
        this.message = message
        return this
    }

    fun setTemperatureRange(
        max: Float,
        min: Float,
    ): ThermalInputDialogComposeBuilder {
        this.maxTemp = max
        this.minTemp = min
        return this
    }

    fun setColors(
        maxColor: Color,
        minColor: Color,
    ): ThermalInputDialogComposeBuilder {
        this.maxColor = maxColor
        this.minColor = minColor
        return this
    }

    fun setButtonTexts(
        positive: String,
        negative: String,
    ): ThermalInputDialogComposeBuilder {
        this.positiveButtonText = positive
        this.negativeButtonText = negative
        return this
    }

    fun setPositiveListener(listener: (Float, Float, Int, Int) -> Unit): ThermalInputDialogComposeBuilder {
        this.onConfirm = listener
        return this
    }

    fun setCancelListener(listener: () -> Unit): ThermalInputDialogComposeBuilder {
        this.onCancel = listener
        return this
    }

    @Composable
    fun show(onDismiss: () -> Unit) {
        ThermalInputDialogCompose(
            message = message,
            maxTemp = maxTemp,
            minTemp = minTemp,
            maxColor = maxColor,
            minColor = minColor,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            onConfirm = onConfirm ?: { _, _, _, _ -> },
            onCancel = onCancel ?: {},
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun ThermalInputDialogComposePreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        ThermalInputDialogCompose(
            message = "Configure thermal parameters for optimal imaging",
            maxTemp = 80f,
            minTemp = 20f,
            onConfirm = { maxTemp, minTemp, maxColor, minColor ->
            },
            onCancel = {
            },
            onDismiss = {
                showDialog = false
            },
        )
    }
}
