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
            emitEvent("auto_enhance")
        }
    }

    fun manualTune() {
        launchWithErrorHandling {
            emitEvent("manual_tune")
        }
    }

    fun aiAnalysis() {
        launchWithErrorHandling {
            emitEvent("ai_analysis")
        }
    }

    fun plusCapture() {
        launchWithErrorHandling {
            emitEvent("plus_capture")
        }
    }

    fun plusRecord() {
        launchWithErrorHandling {
            emitEvent("plus_record")
        }
    }

    fun plusProcess() {
        launchWithErrorHandling {
            emitEvent("plus_process")
        }
    }
}
