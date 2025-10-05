package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.PDFListViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Type alias for cleaner code
typealias PDFItem = PDFListViewModel.PDFItem

class PDFListComposeFragment : BaseComposeFragment<PDFListViewModel>() {

    private var isTC007 by mutableStateOf(false)

    override fun createViewModel(): PDFListViewModel {
        return viewModels<PDFListViewModel>().value
    }

    companion object {
        fun newInstance(isTC007: Boolean): PDFListComposeFragment {
            return PDFListComposeFragment().apply {
                this.isTC007 = isTC007
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: PDFListViewModel) {
        val context = LocalContext.current

        // Observe ViewModel state
        val pdfItems by viewModel.pdfItems.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with device type indicator
                PDFListHeader(
                    isTC007 = isTC007,
                    totalReports = pdfItems.size
                )

                // Selection toolbar
                if (isSelectionMode) {
                    PDFSelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onShareSelected = {
                            shareSelectedPDFs(context, selectedItems.toList())
                        },
                        onDeleteSelected = { viewModel.deleteSelectedItems() },
                        onExportSelected = {
                            exportSelectedPDFs(context, selectedItems.toList())
                        }
                    )
                }

                // PDF list content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        pdfItems.isEmpty() -> {
                            EmptyPDFState(
                                onRefresh = { viewModel.refreshPDFList() }
                            )
                        }

                        else -> {
                            PDFList(
                                pdfs = pdfItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.path)
                                    } else {
                                        openPDF(context, item.path)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item.path)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PDFListHeader(
        isTC007: Boolean,
        totalReports: Int
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analysis Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(
                            text = if (isTC007) "TC007 Device" else "Standard Device",
                            color = if (isTC007) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalReports reports",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Reports",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: androidx.compose.ui.graphics.Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun PDFSelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onShareSelected: () -> Unit,
        onDeleteSelected: () -> Unit,
        onExportSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount report${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onShareSelected) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onExportSelected) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onClearSelection) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading reports...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyPDFState(
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = "No reports",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "No Reports Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Generate thermal analysis reports to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }

    @Composable
    private fun PDFList(
        pdfs: List<PDFItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (PDFItem) -> Unit,
        onItemLongClick: (PDFItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pdfs) { item ->
                PDFListItem(
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun PDFListItem(
        item: PDFItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // PDF info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isAnalysisReport) {
                            StatusChip(
                                text = "ANALYSIS",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = SimpleDateFormat(
                            "MMM dd, yyyy HH:mm",
                            Locale.getDefault()
                        ).format(Date(item.dateModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Selection indicator or actions
                if (isSelectionMode) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Not Selected",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More actions"
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun openPDF(context: android.content.Context, path: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    ),
                    "application/pdf"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - maybe show a toast or use internal PDF viewer
        }
    }

    private fun shareSelectedPDFs(context: android.content.Context, selectedPaths: List<String>) {
        try {
            val uris = selectedPaths.map { path ->
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path)
                )
            }

            val intent = Intent().apply {
                if (uris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Reports"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun exportSelectedPDFs(context: android.content.Context, selectedPaths: List<String>) {
        // Implementation for exporting PDFs to external storage
        // This would typically involve copying files to a user-accessible location
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }
}