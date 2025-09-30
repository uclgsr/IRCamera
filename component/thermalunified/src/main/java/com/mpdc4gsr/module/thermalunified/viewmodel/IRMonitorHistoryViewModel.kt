package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.dao.ThermalDao
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ViewModel for IR Monitor History functionality
 * Manages thermal monitoring session history with filtering and selection capabilities
 */
class IRMonitorHistoryViewModel : BaseViewModel() {

    // Data classes for history management
    data class HistoryItem(
        val id: String,
        val sessionName: String,
        val startTime: Long,
        val duration: Long,
        val sampleCount: Int,
        val avgTemperature: Float,
        val maxTemperature: Float,
        val minTemperature: Float,
        val sessionType: SessionType,
        val dataFilePath: String
    )

    enum class SessionType(val displayName: String) {
        MONITORING("Monitor"),
        CAPTURE("Capture"),
        ANALYSIS("Analysis"),
        CALIBRATION("Calibration")
    }

    enum class HistoryFilter(
        val displayName: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        ALL("All", Icons.Default.ViewList),
        TODAY("Today", Icons.Default.Today),
        WEEK("This Week", Icons.Default.DateRange),
        MONTH("This Month", Icons.Default.CalendarMonth)
    }

    // StateFlow properties expected by the Compose fragment
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // UI events channel for one-time events
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // Internal data storage
    private var allHistoryItems: List<HistoryItem> = emptyList()

    init {
        refreshHistory()
    }

    fun changeFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun exportSelectedItems() {
        // Implement export functionality for selected history items
        launchWithErrorHandling {
            val selectedList = _selectedItems.value
            if (selectedList.isEmpty()) {
                _uiEvents.emit(UiEvent.ShowMessage("No items selected for export"))
                return@launchWithErrorHandling
            }
            
            // Create export data from selected items
            val exportData = historyItems.value.filter { selectedList.contains(it.id) }
            
            // Emit export event with data
            _uiEvents.emit(UiEvent.ExportData(exportData))
            
            // Show success message and clear selection
            _uiEvents.emit(UiEvent.ShowMessage("Exported ${exportData.size} items"))
            clearSelection()
        }
    }

    fun deleteSelectedItems() {
        launchWithErrorHandling {
            val selectedIds = _selectedItems.value
            if (selectedIds.isNotEmpty()) {
                // Remove selected items from the database on IO thread
                withContext(Dispatchers.IO) {
                    selectedIds.forEach { id ->
                        val item = allHistoryItems.find { it.id == id }
                        item?.let {
                            // Convert id back to startTime for database operation
                            val startTime = it.startTime
                            AppDatabase.getInstance().thermalDao().delDetail(startTime)
                        }
                    }
                }

                // Refresh the data after deletion
                refreshHistory()
                clearSelection()
            }
        }
    }

    fun refreshHistory() {
        launchWithLoading {
            try {
                // Perform database operations on IO thread
                val historyItems = withContext(Dispatchers.IO) {
                    val recordList: List<ThermalDao.Record> =
                        AppDatabase.getInstance().thermalDao().queryRecordList()

                    // Convert database records to HistoryItem objects
                    recordList.mapIndexed { index, record ->
                        // Query additional details for temperature statistics
                        val detailList = AppDatabase.getInstance().thermalDao().queryDetail(record.startTime)

                        // Calculate temperature statistics from detail data
                        val temperatures = detailList.map { it.thermal }
                        val maxTemperatures = detailList.map { it.thermalMax }
                        val minTemperatures = detailList.map { it.thermalMin }

                        val avgTemp = if (temperatures.isNotEmpty()) temperatures.average().toFloat() else 0f
                        val maxTemp = maxTemperatures.maxOrNull() ?: 0f
                        val minTemp = minTemperatures.minOrNull() ?: 0f

                        HistoryItem(
                            id = record.startTime.toString(),
                            sessionName = "Session ${index + 1}",
                            startTime = record.startTime,
                            duration = record.duration.toLong() * 1000L, // Convert seconds to milliseconds
                            sampleCount = detailList.size,
                            avgTemperature = avgTemp,
                            maxTemperature = maxTemp,
                            minTemperature = minTemp,
                            sessionType = when (record.type) {
                                "point" -> SessionType.MONITORING
                                "line" -> SessionType.ANALYSIS
                                "area" -> SessionType.CAPTURE
                                else -> SessionType.MONITORING
                            },
                            dataFilePath = detailList.firstOrNull()?.irImagePath ?: "" // Use image path from first entity if available
                        )
                    }
                }

                // Update the data on main thread
                allHistoryItems = historyItems
                applyFilter()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleItemSelection(id: String) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(id)) {
            currentSelected.remove(id)
        } else {
            currentSelected.add(id)
        }
        _selectedItems.value = currentSelected

        // Exit selection mode if no items are selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun viewHistoryDetails(item: HistoryItem) {
        // Implement navigation to details screen with history item data
        launchWithErrorHandling {
            // Emit navigation event with the selected item
            _uiEvents.emit(UiEvent.NavigateToDetails(item))
        }
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    private fun applyFilter() {
        val filteredItems = when (_selectedFilter.value) {
            HistoryFilter.ALL -> allHistoryItems
            HistoryFilter.TODAY -> filterByToday()
            HistoryFilter.WEEK -> filterByThisWeek()
            HistoryFilter.MONTH -> filterByThisMonth()
        }
        _historyItems.value = filteredItems.sortedByDescending { it.startTime }
    }

    private fun filterByToday(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrow = calendar.apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        return allHistoryItems.filter { it.startTime in today until tomorrow }
    }

    private fun filterByThisWeek(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis

        return allHistoryItems.filter { it.startTime in weekStart until weekEnd }
    }

    private fun filterByThisMonth(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        return allHistoryItems.filter { it.startTime in monthStart until monthEnd }
    }

    // UI Event sealed class for one-time events
    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        data class ExportData(val items: List<HistoryItem>) : UiEvent()
        data class NavigateToDetails(val item: HistoryItem) : UiEvent()
    }
}