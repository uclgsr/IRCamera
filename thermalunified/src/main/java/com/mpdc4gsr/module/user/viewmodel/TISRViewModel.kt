package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TISRViewModel : BaseViewModel() {
    private val _isTISREnabled = MutableStateFlow(false)
    val isTISREnabled: StateFlow<Boolean> = _isTISREnabled.asStateFlow()

    init {
        loadTISRState()
    }

    private fun loadTISRState() {
        launchWithErrorHandling {
            // Original TS004Repository functionality was removed - use SharedManager default
            _isTISREnabled.value = SharedManager.is04TISR
        }
    }

    fun updateTISRState(enabled: Boolean) {
        launchWithErrorHandling {
            SharedManager.is04TISR = enabled
            _isTISREnabled.value = enabled
            // Note: Original socket communication removed as per original activity changes
        }
    }
}
