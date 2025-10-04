package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalVideoViewModel : BaseViewModel() {

    fun shareVideo() {
        launchWithErrorHandling {
            emitEvent("share_video")
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            emitEvent("show_more_options")
        }
    }

    fun previousFrame() {
        launchWithErrorHandling {
            emitEvent("previous_frame")
        }
    }

    fun nextFrame() {
        launchWithErrorHandling {
            emitEvent("next_frame")
        }
    }

    fun exportFrame() {
        launchWithErrorHandling {
            emitEvent("export_frame")
        }
    }

    fun analyzeVideo() {
        launchWithErrorHandling {
            emitEvent("analyze_video")
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            emitEvent("open_settings")
        }
    }
}
