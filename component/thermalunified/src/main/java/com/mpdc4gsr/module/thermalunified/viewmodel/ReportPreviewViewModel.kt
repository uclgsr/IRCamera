package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportPreviewViewModel : BaseViewModel() {

    private val _selectedLayout = MutableStateFlow(0)
    val selectedLayout: StateFlow<Int> = _selectedLayout.asStateFlow()

    private val _showImages = MutableStateFlow(true)
    val showImages: StateFlow<Boolean> = _showImages.asStateFlow()

    private val _showMetadata = MutableStateFlow(true)
    val showMetadata: StateFlow<Boolean> = _showMetadata.asStateFlow()

    private val _showWatermark = MutableStateFlow(false)
    val showWatermark: StateFlow<Boolean> = _showWatermark.asStateFlow()

    fun selectLayout(index: Int) {
        launchWithErrorHandling {
            _selectedLayout.value = index
        }
    }

    fun toggleImages() {
        launchWithErrorHandling {
            _showImages.value = !_showImages.value
        }
    }

    fun toggleMetadata() {
        launchWithErrorHandling {
            _showMetadata.value = !_showMetadata.value
        }
    }

    fun toggleWatermark() {
        launchWithErrorHandling {
            _showWatermark.value = !_showWatermark.value
        }
    }

    fun generatePreview() {
        launchWithLoading {
            // TODO: Generate actual preview
        }
    }

    fun proceedToSecond() {
        launchWithErrorHandling {
            // TODO: Navigate to second step
        }
    }
}
