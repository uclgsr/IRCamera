package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.component.shared.app.common.SharedManager
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoSaveViewModel : BaseViewModel() {
    private val _isAutoSaveEnabled = MutableStateFlow(false)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    init {
        loadAutoSaveState()
    }

    private fun loadAutoSaveState() {
        _isAutoSaveEnabled.value = SharedManager.is04AutoSync
    }

    fun updateAutoSaveState(enabled: Boolean) {
        launchWithErrorHandling {
            SharedManager.is04AutoSync = enabled
            _isAutoSaveEnabled.value = enabled
        }
    }
}


