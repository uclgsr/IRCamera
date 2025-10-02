package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.CarDetectBean
import com.mpdc4gsr.libunified.app.bean.CarDetectChildBean
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.utils.CarDetectData

/**
 * CarDetectDialog - Migrated to Jetpack Compose
 * Maintains API compatibility with the old databinding version
 */
class CarDetectDialog(context: Context, val listener: ((bean: CarDetectChildBean) -> Unit)) :
    Dialog(context, R.style.DefaultDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        val composeView = ComposeView(context).apply {
            setContent {
                LibUnifiedTheme {
                    CarDetectDialogContent(
                        detectList = CarDetectData.getDetectList(),
                        currentSelection = SharedManager.getCarDetectInfo(),
                        onDismiss = { dismiss() },
                        onItemSelected = { bean ->
                            listener(bean)
                            dismiss()
                        }
                    )
                }
            }
        }

        setContentView(composeView)
    }

    companion object {
        @JvmStatic
        @Deprecated("Use CarDetectData.getDetectList() instead", ReplaceWith("CarDetectData.getDetectList()"))
        fun getDetectList(): MutableList<CarDetectBean> {
            return CarDetectData.getDetectList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarDetectDialogContent(
    detectList: List<CarDetectBean>,
    currentSelection: CarDetectChildBean,
    onDismiss: () -> Unit,
    onItemSelected: (CarDetectChildBean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title bar
            TopAppBar(
                title = { Text(stringResource(R.string.car_detection_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.select_monitor_return)
                        )
                    }
                }
            )

            // List of detection categories and items
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                detectList.forEachIndexed { index, detectBean ->
                    item {
                        Text(
                            text = detectBean.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(detectBean.detectChildBeans) { childBean ->
                        CarDetectItem(
                            childBean = childBean,
                            isSelected = currentSelection.type == childBean.type &&
                                    currentSelection.pos == childBean.pos,
                            onClick = { onItemSelected(childBean) }
                        )
                    }

                    // Add divider between categories (except after last one)
                    if (index < detectList.size - 1) {
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarDetectItem(
    childBean: CarDetectChildBean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = childBean.item,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (!childBean.temperature.isNullOrEmpty()) {
                    Text(
                        text = childBean.temperature,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!childBean.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = childBean.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
