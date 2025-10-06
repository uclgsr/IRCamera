package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.R

@Composable
fun IRConfigInputDialogCompose(
    type: IRConfigInputType,
    isTC007: Boolean,
    initialValue: Float? = null,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialValue?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    val dialogData = remember(type, isTC007) {
        when (type) {
            IRConfigInputType.TEMP -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_environment)} ${
                    UnitTools.showConfigC(-10, if (isTC007) 50 else 55)
                }",
                unit = UnitTools.showUnit(),
                showUnit = true,
                validator = { value ->
                    value in UnitTools.showUnitValue(-10f)..UnitTools.showUnitValue(
                        if (isTC007) 50f else 55f
                    )
                }
            )

            IRConfigInputType.DIS -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)",
                unit = "m",
                showUnit = true,
                validator = { value -> value in 0.2f..(if (isTC007) 4f else 5f) }
            )

            IRConfigInputType.EM -> IRConfigDialogData(
                title = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)",
                unit = "",
                showUnit = false,
                validator = { value -> value in (if (isTC007) 0.1f else 0.01f)..1f }
            )
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.73f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = dialogData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Input field with unit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                handleConfirm(
                                    inputText,
                                    dialogData.validator,
                                    context,
                                    onConfirm,
                                    onDismiss
                                )
                            }
                        ),
                        singleLine = true
                    )
                    if (dialogData.showUnit) {
                        Text(
                            text = dialogData.unit,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            handleConfirm(
                                inputText,
                                dialogData.validator,
                                context,
                                onConfirm,
                                onDismiss
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

private fun handleConfirm(
    inputText: String,
    validator: (Float) -> Boolean,
    context: android.content.Context,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val input = inputText.toFloat()
        if (validator(input)) {
            onConfirm(input)
            onDismiss()
        } else {
            TToast.shortToast(context, R.string.tip_input_format)
        }
    } catch (e: NumberFormatException) {
        TToast.shortToast(context, R.string.tip_input_format)
    }
}

enum class IRConfigInputType {
    TEMP, DIS, EM
}

private data class IRConfigDialogData(
    val title: String,
    val unit: String,
    val showUnit: Boolean,
    val validator: (Float) -> Boolean
)