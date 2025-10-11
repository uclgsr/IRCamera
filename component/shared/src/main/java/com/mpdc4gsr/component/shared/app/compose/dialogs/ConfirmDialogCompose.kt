package com.mpdc4gsr.component.shared.app.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmDialog(
    title: String,
    message: String = "",
    showCancel: Boolean = true,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    showCheckbox: Boolean = false,
    checkboxLabel: String = "",
    onConfirm: (isChecked: Boolean) -> Unit,
    onDismiss: () -> Unit = {},
    showIcon: Boolean,
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.8f else 0.4f
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                        )
                        Text(
                            text = checkboxLabel,
                            fontSize = 14.sp,
                            color = Color.Black,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showCancel) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    Button(
                        onClick = { onConfirm(isChecked) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}


