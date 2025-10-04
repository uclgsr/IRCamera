package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalVideoViewModel : BaseViewModel() {

    private val _currentFramePosition = MutableStateFlow(0L)
    val currentFramePosition: StateFlow<Long> = _currentFramePosition.asStateFlow()

    private val _action = MutableLiveData<VideoAction>()
    val action: LiveData<VideoAction> = _action

    fun shareVideo() {
        launchWithErrorHandling {
            _action.postValue(VideoAction.ShareVideo)
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _action.postValue(VideoAction.ShowMoreOptions)
        }
    }

    fun previousFrame() {
        launchWithErrorHandling {
            val newPosition = (_currentFramePosition.value - 33).coerceAtLeast(0L)
            _currentFramePosition.value = newPosition
            _action.postValue(VideoAction.SeekToFrame(newPosition))
        }
    }

    fun nextFrame() {
        launchWithErrorHandling {
            val newPosition = _currentFramePosition.value + 33
            _currentFramePosition.value = newPosition
            _action.postValue(VideoAction.SeekToFrame(newPosition))
        }
    }

    fun exportFrame() {
        launchWithErrorHandling {
            val timestamp = System.currentTimeMillis()
            val fileName = "thermal_frame_$timestamp.jpg"
            _action.postValue(VideoAction.ExportFrame(fileName, _currentFramePosition.value))
            _uiEvents.emit(UiEvent.ShowMessage("Exporting frame: $fileName"))
        }
    }

    fun analyzeVideo() {
        launchWithErrorHandling {
            _action.postValue(VideoAction.AnalyzeVideo)
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            _action.postValue(VideoAction.NavigateToSettings)
        }
    }

    fun updateFramePosition(position: Long) {
        _currentFramePosition.value = position
    }

    sealed class VideoAction {
        object ShareVideo : VideoAction()
        object ShowMoreOptions : VideoAction()
        data class SeekToFrame(val position: Long) : VideoAction()
        data class ExportFrame(val fileName: String, val position: Long) : VideoAction()
        object AnalyzeVideo : VideoAction()
        object NavigateToSettings : VideoAction()
    }
}
