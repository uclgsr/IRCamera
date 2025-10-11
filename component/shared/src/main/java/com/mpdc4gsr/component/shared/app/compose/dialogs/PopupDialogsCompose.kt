package com.mpdc4gsr.component.shared.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmissivityTipPopup(
    title: String = "",
    materialText: String = "",
    environmentTemp: Float,
    distance: Float,
    emissivity: Float,
    environmentLabel: String = "Environment",
    distanceLabel: String = "Distance",
    emissivityLabel: String = "Emissivity",
    showCheckbox: Boolean = true,
    checkboxLabel: String = "Don't show again",
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    var isChecked by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.85f else 0.55f
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
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                if (materialText.isNotEmpty()) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5),
                            ),
                    ) {
                        Text(
                            text = materialText,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            modifier =
                                Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                        )
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false),
                ) {
                    EmissivityInfoRow(
                        label = "$environmentLabel:",
                        value = String.format("%.1f°C", environmentTemp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$distanceLabel:",
                        value = String.format("%.1fm", distance),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EmissivityInfoRow(
                        label = "$emissivityLabel:",
                        value = String.format("%.2f", emissivity),
                    )
                }
                if (showCheckbox) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
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
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = "Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(isChecked)
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

@Composable
private fun EmissivityInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFF8F8F8),
                    shape = RoundedCornerShape(6.dp),
                ).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
        )
    }
}


