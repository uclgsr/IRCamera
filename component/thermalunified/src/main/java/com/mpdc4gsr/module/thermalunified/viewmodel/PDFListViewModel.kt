package com.mpdc4gsr.module.thermalunified.viewmodel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val isAnalysisReport: Boolean = false
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
    // Load PDF items
    private fun loadPDFItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val items = getPDFItemsList()
                withContext(Dispatchers.Main) {
                    _pdfItems.value = items
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading PDF items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    // Selection mode methods
    fun enterSelectionMode(itemPath: String? = null) {
        _isSelectionMode.value = true
        itemPath?.let {
            _selectedItems.value = setOf(it)
        }
    }
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }
    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }
    fun toggleItemSelection(itemPath: String) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(itemPath)) {
            currentSelected.remove(itemPath)
        } else {
            currentSelected.add(itemPath)
        }
        _selectedItems.value = currentSelected
        // Exit selection mode if no items selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
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
                    Log.e(TAG, "Error deleting file: ${item.path}", e)
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
    private suspend fun getPDFItemsList(): List<PDFItem> {
        return try {
            val items = mutableListOf<PDFItem>()
            // Scan the PDF directory for PDF files
            val pdfDir = File(FileConfig.getPdfDir())
            Log.d(TAG, "Scanning PDF directory: ${pdfDir.absolutePath}")
            if (pdfDir.exists() && pdfDir.isDirectory) {
                val pdfFiles = pdfDir.listFiles { file ->
                    file.isFile && file.name.lowercase().endsWith(".pdf")
                }
                Log.d(TAG, "Found ${pdfFiles?.size ?: 0} PDF files")
                pdfFiles?.forEach { pdfFile ->
                    try {
                        // Determine if this is an analysis report based on filename patterns
                        val isAnalysisReport = pdfFile.name.contains("analysis", ignoreCase = true) ||
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
                                isAnalysisReport = isAnalysisReport
                            )
                        )
                        Log.d(TAG, "Added PDF: ${pdfFile.name} (${pdfFile.length()} bytes)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing PDF file: ${pdfFile.name}", e)
                    }
                }
            } else {
                Log.w(TAG, "PDF directory does not exist or is not a directory: ${pdfDir.absolutePath}")
            }
            // Sort by date modified (newest first)
            val sortedItems = items.sortedByDescending { it.dateModified }
            Log.d(TAG, "Returning ${sortedItems.size} PDF items")
            sortedItems
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF items list", e)
            emptyList()
        }
    }
    fun showMoreActions(item: PDFItem) {
        // Placeholder for more actions functionality
    }
}