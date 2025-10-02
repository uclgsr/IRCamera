package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportPreviewViewModel : BaseViewModel() {

    private val _selectedLayout = MutableStateFlow(0)
    val selectedLayout: StateFlow<Int> = _selectedLayout.asStateFlow()

    private val _showImages = MutableStateFlow(true)
    val showImages: StateFlow<Boolean> = _showImages.asStateFlow()

    private val _showMetadata = MutableStateFlow(true)
    val showMetadata: StateFlow<Boolean> = _showMetadata.asStateFlow()

    private val _showWatermark = MutableStateFlow(false)
    val showWatermark: StateFlow<Boolean> = _showWatermark.asStateFlow()

    private val _previewGenerated = MutableStateFlow(false)
    val previewGenerated: StateFlow<Boolean> = _previewGenerated.asStateFlow()

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
            delay(1000)
            _previewGenerated.value = true
        }
    }

    fun proceedToSecond(context: Context) {
        launchWithErrorHandling {
            NavigationManager.build(RouterConfig.REPORT_PREVIEW_SECOND)
                .navigation(context)
        }
    }
}
