package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class ImageColorViewModel : BaseViewModel() {
    private val _timestamp = MutableStateFlow("")
    val timestamp: StateFlow<String> = _timestamp.asStateFlow()
    private val _showData = MutableStateFlow(false)
    val showData: StateFlow<Boolean> = _showData.asStateFlow()
    private val _leftImagePath = MutableStateFlow("")
    val leftImagePath: StateFlow<String> = _leftImagePath.asStateFlow()
    private val _rightImagePath = MutableStateFlow("")
    val rightImagePath: StateFlow<String> = _rightImagePath.asStateFlow()
    private val _comparisonResult = MutableStateFlow("")
    val comparisonResult: StateFlow<String> = _comparisonResult.asStateFlow()

    init {
        updateTimestamp()
    }

    fun toggleDataDisplay() {
        launchWithErrorHandling {
            _showData.value = !_showData.value
        }
    }

    fun loadImages(
        leftImagePath: String,
        rightImagePath: String,
    ) {
        launchWithLoading {
            _leftImagePath.value = leftImagePath
            _rightImagePath.value = rightImagePath
        }
    }

    fun compareImages() {
        launchWithErrorHandling {
            _comparisonResult.value = "Images compared successfully"
        }
    }

    private fun updateTimestamp() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        _timestamp.value = dateFormat.format(Date())
    }
}
