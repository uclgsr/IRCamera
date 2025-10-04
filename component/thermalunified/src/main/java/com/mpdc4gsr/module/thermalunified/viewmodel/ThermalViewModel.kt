package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalViewModel : BaseViewModel() {

    private val _enhancementLevel = MutableStateFlow(50)
    val enhancementLevel: StateFlow<Int> = _enhancementLevel.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _action = MutableLiveData<PlusAction>()
    val action: LiveData<PlusAction> = _action

    fun yuvArea(
        yuv: ByteArray,
        temp: FloatArray,
        max: Float,
        min: Float,
    ) {
        for (i in temp.indices) {
            if (temp[i] < min) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0x00.toByte()
            }
            if (temp[i] > max) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0xFF.toByte()
            }
        }
    }

    fun autoEnhance() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _enhancementLevel.value = 75
            _action.postValue(PlusAction.ApplyAutoEnhancement(75))
            _uiEvents.emit(UiEvent.ShowMessage("Auto enhancement applied"))
            _isProcessing.value = false
        }
    }

    fun manualTune() {
        launchWithErrorHandling {
            _action.postValue(PlusAction.ShowManualTuneDialog)
        }
    }

    fun aiAnalysis() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _action.postValue(PlusAction.RunAIAnalysis)
            _uiEvents.emit(UiEvent.ShowMessage("Running AI analysis..."))
        }
    }

    fun plusCapture() {
        launchWithErrorHandling {
            val timestamp = System.currentTimeMillis()
            val fileName = "thermal_plus_$timestamp.jpg"
            _action.postValue(PlusAction.CapturePlusImage(fileName, _enhancementLevel.value))
            _uiEvents.emit(UiEvent.ShowMessage("Plus image captured: $fileName"))
        }
    }

    fun plusRecord() {
        launchWithErrorHandling {
            _action.postValue(PlusAction.StartPlusRecording(_enhancementLevel.value))
        }
    }

    fun plusProcess() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _action.postValue(PlusAction.ProcessWithEnhancement(_enhancementLevel.value))
            _uiEvents.emit(UiEvent.ShowMessage("Processing with enhancement level ${_enhancementLevel.value}%"))
        }
    }

    fun setEnhancementLevel(level: Int) {
        _enhancementLevel.value = level.coerceIn(0, 100)
    }

    fun processingComplete() {
        _isProcessing.value = false
    }

    sealed class PlusAction {
        data class ApplyAutoEnhancement(val level: Int) : PlusAction()
        object ShowManualTuneDialog : PlusAction()
        object RunAIAnalysis : PlusAction()
        data class CapturePlusImage(val fileName: String, val enhancementLevel: Int) : PlusAction()
        data class StartPlusRecording(val enhancementLevel: Int) : PlusAction()
        data class ProcessWithEnhancement(val level: Int) : PlusAction()
    }
}
