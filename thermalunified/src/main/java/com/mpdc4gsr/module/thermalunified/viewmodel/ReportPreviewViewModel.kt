package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
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
    private val _previewData = MutableStateFlow<PreviewData?>(null)
    val previewData: StateFlow<PreviewData?> = _previewData.asStateFlow()

    data class PreviewData(
        val layoutIndex: Int,
        val includeImages: Boolean,
        val includeMetadata: Boolean,
        val includeWatermark: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
    )

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
            val currentLayout = _selectedLayout.value
            val currentShowImages = _showImages.value
            val currentShowMetadata = _showMetadata.value
            val currentShowWatermark = _showWatermark.value
            delay(500)
            val preview =
                PreviewData(
                    layoutIndex = currentLayout,
                    includeImages = currentShowImages,
                    includeMetadata = currentShowMetadata,
                    includeWatermark = currentShowWatermark,
                )
            _previewData.value = preview
            _previewGenerated.value = true
        }
    }

    fun proceedToSecond(context: Context) {
        launchWithErrorHandling {
            NavigationManager
                .build(RouterConfig.REPORT_PREVIEW_SECOND)
                .navigation(context)
        }
    }
}
