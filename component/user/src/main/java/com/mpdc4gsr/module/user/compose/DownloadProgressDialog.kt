package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DecimalFormat
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun DownloadProgressDialog(
    isVisible: Boolean,
    currentBytes: Long,
    totalBytes: Long,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { /* TODO: Implement non-cancellable
                     *   - Implement callback logic for onDismissRequest
                     *   - Handle data/state updates
                     *   - Provide user feedback
                     */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(RCore.string.ts004_download_doing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // File size info
                    val sizeText =
                        "${stringResource(RCore.string.detail_len)}: ${formatFileSize(currentBytes)}/${
                            formatFileSize(totalBytes)
                        }"
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    // Progress bar
                    val progress =
                        if (totalBytes > 0) (currentBytes.toFloat() / totalBytes.toFloat()) else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024) + "KB"
        size < 1024 * 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024) + "MB"
        else -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024 / 1024) + "GB"
    }
}
