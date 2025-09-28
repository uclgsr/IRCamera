package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.common.SharedManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AutoSave settings
 * Manages the auto save toggle state using SharedManager
 */
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