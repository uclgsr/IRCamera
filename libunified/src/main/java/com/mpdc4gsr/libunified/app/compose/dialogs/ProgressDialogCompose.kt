package com.mpdc4gsr.libunified.app.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProgressDialog(
    message: String = "",
    progress: Float = -1f,
    cancelable: Boolean = true,
    onDismiss: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val widthFraction = if (isPortrait) 0.52f else 0.35f
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
                        .padding(24.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
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
                    .width(screenWidthDp - 36.dp)
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
                    text = "Select Color",
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                val commonColors =
                    listOf(
                        Color.Red,
                        Color.Green,
                        Color.Blue,
                        Color.Yellow,
                        Color.Cyan,
                        Color.Magenta,
                        Color.White,
                        Color.Gray,
                        Color.Black,
                        Color(0xFFFFA500),
                        Color(0xFF800080),
                        Color(0xFFFFC0CB),
                    )
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns =
                        androidx.compose.foundation.lazy.grid.GridCells
                            .Fixed(4),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(commonColors.size) { index ->
                        val color = commonColors[index]
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .background(
                                        color = color,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ).clickable {
                                        selectedColor = color.toArgb()
                                    }.then(
                                        if (selectedColor == color.toArgb()) {
                                            Modifier.border(
                                                width = 3.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
