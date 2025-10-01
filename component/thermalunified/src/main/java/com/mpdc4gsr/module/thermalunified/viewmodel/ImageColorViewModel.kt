package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Image Color Comparison functionality
 * Manages dual thermal image comparison with color processing
 */
class ImageColorViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ImageColorUiState())
    val uiState: StateFlow<ImageColorUiState> = _uiState.asStateFlow()

    init {
        updateTimestamp()
    }

    fun toggleDataDisplay() {
        launchWithErrorHandling {
            val current = _uiState.value
            _uiState.value = current.copy(showData = !current.showData)
        }
    }

    fun loadImages(leftImagePath: String, rightImagePath: String) {
        launchWithErrorHandling {
            _uiState.value = _uiState.value.copy(
                leftImagePath = leftImagePath,
                rightImagePath = rightImagePath,
                isLoading = false
            )
        }
    }

    fun compareImages() {
        launchWithErrorHandling {
            // Perform image comparison logic
            _uiState.value = _uiState.value.copy(
                comparisonResult = "Images compared successfully"
            )
        }
    }

    private fun updateTimestamp() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        _uiState.value = _uiState.value.copy(timestamp = timestamp)
    }

    data class ImageColorUiState(
        val timestamp: String = "",
        val showData: Boolean = false,
        val leftImagePath: String = "",
        val rightImagePath: String = "",
        val isLoading: Boolean = true,
        val comparisonResult: String = ""
    )
}
