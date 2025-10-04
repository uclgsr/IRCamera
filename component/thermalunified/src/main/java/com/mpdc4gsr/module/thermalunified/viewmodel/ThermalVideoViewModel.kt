package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalVideoViewModel : BaseViewModel() {

    fun shareVideo() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Share Video"))
        }
    }

    fun showMoreOptions() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Show More Options"))
        }
    }

    fun previousFrame() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Previous Frame"))
        }
    }

    fun nextFrame() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Next Frame"))
        }
    }

    fun exportFrame() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Export Frame"))
        }
    }

    fun analyzeVideo() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Analyze Video"))
        }
    }

    fun openSettings() {
        launchWithErrorHandling {
            _uiEvents.emit(UiEvent.ShowMessage("Open Settings"))
        }
    }
}
