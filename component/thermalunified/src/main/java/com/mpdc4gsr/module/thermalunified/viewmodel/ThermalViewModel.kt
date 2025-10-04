package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalViewModel : BaseViewModel() {

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
            _uiEvents.emit(UiEvent.ShowMessage("Auto Enhance"))
        }
    }

    fun manualTune() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Manual Tune"))
        }
    }

    fun aiAnalysis() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("AI Analysis"))
        }
    }

    fun plusCapture() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Plus Capture"))
        }
    }

    fun plusRecord() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Plus Record"))
        }
    }

    fun plusProcess() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Plus Process"))
        }
    }
}
