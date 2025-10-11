package com.mpdc4gsr.component.thermal.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.component.shared.app.config.FileConfig
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PDFListViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "PDFListViewModel"
    }

    // Data class for PDF items (matching the one in fragment)
    data class PDFItem(
        val path: String,
        val name: String,
        val size: Long,
        val pageCount: Int,
        val dateModified: Long,
        val isAnalysisReport: Boolean = false,
    )

    // State flows for Compose
    private val _pdfItems = MutableStateFlow<List<PDFItem>>(emptyList())
    val pdfItems: StateFlow<List<PDFItem>> = _pdfItems.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPDFItems()
    }

    private fun loadPDFItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.update { true }
            try {
                val items = getPDFItemsList()
                withContext(Dispatchers.Main) {
                    _pdfItems.update { items }
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun enterSelectionMode(itemPath: String? = null) {
        _isSelectionMode.update { true }
        itemPath?.let { path ->
            _selectedItems.update { setOf(path) }
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.update { false }
        _selectedItems.update { emptySet() }
    }

    fun clearSelection() {
        _selectedItems.update { emptySet() }
        _isSelectionMode.update { false }
    }

    fun toggleItemSelection(itemPath: String) {
        _selectedItems.update { currentSet ->
            val mutableSet = currentSet.toMutableSet()
            if (mutableSet.contains(itemPath)) {
                mutableSet.remove(itemPath)
            } else {
                mutableSet.add(itemPath)
            }
            mutableSet
        }
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.update { false }
        }
    }

    // File operations
    fun deleteSelectedItems() {
        val selectedPaths = _selectedItems.value
        val itemsToDelete = _pdfItems.value.filter { selectedPaths.contains(it.path) }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach { item ->
                try {
                    File(item.path).delete()
                } catch (e: Exception) {
                }
            }
            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadPDFItems() // Refresh the list
            }
        }
    }

    fun refreshPDFList() {
        loadPDFItems()
    }

    // Get PDF items from file system
    private suspend fun getPDFItemsList(): List<PDFItem> =
        try {
            val items = mutableListOf<PDFItem>()
            // Scan the PDF directory for PDF files
            val pdfDir = File(FileConfig.getPdfDir())
            if (pdfDir.exists() && pdfDir.isDirectory) {
                val pdfFiles =
                    pdfDir.listFiles { file ->
                        file.isFile && file.name.lowercase().endsWith(".pdf")
                    }
                pdfFiles?.forEach { pdfFile ->
                    try {
                        // Determine if this is an analysis report based on filename patterns
                        val isAnalysisReport =
                            pdfFile.name.contains("analysis", ignoreCase = true) ||
                                pdfFile.name.contains("report", ignoreCase = true) ||
                                pdfFile.name.contains("thermal", ignoreCase = true)
                        // For now, we'll use a default page count of 1
                        // In a production app, you would use a PDF library to get actual page count
                        val pageCount = 1
                        items.add(
                            PDFItem(
                                path = pdfFile.absolutePath,
                                name = pdfFile.name,
                                size = pdfFile.length(),
                                pageCount = pageCount,
                                dateModified = pdfFile.lastModified(),
                                isAnalysisReport = isAnalysisReport,
                            ),
                        )
                    } catch (e: Exception) {
                    }
                }
            } else {
            }
            // Sort by date modified (newest first)
            val sortedItems = items.sortedByDescending { it.dateModified }
            sortedItems
        } catch (e: Exception) {
            emptyList()
        }

    fun showMoreActions(item: PDFItem) {
        // Placeholder for more actions functionality
    }
}



