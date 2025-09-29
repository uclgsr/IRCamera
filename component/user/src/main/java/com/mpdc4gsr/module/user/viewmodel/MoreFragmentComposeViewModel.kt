package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.common.SaveSettingUtil
import com.mpdc4gsr.libunified.app.common.WifiSaveSettingUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for More Fragment Compose
 * Manages device settings and configuration options
 */
class MoreFragmentComposeViewModel : BaseViewModel() {

    companion object {
        // Mock data constants for improved maintainability
        private const val DEFAULT_VERSION = "1.0.0"
        private const val DEFAULT_UPGRADE_AVAILABLE = false
    }

    data class DeviceSettingsState(
        val isTC007: Boolean = false,
        val isSaveSettingEnabled: Boolean = false,
        val hasUpgrade: Boolean = false,
        val versionText: String = ""
    )

    private val _deviceSettings = MutableStateFlow(DeviceSettingsState())
    val deviceSettings: StateFlow<DeviceSettingsState> = _deviceSettings.asStateFlow()

    fun initialize(isTC007: Boolean) {
        launchWithErrorHandling {
            val isSaveEnabled = if (isTC007) {
                WifiSaveSettingUtil.isSaveSetting
            } else {
                SaveSettingUtil.isSaveSetting
            }

            _deviceSettings.value = DeviceSettingsState(
                isTC007 = isTC007,
                isSaveSettingEnabled = isSaveEnabled,
                hasUpgrade = DEFAULT_UPGRADE_AVAILABLE,
                versionText = DEFAULT_VERSION
            )
        }
    }

    fun updateSaveSetting(enabled: Boolean) {
        launchWithErrorHandling {
            val currentState = _deviceSettings.value
            if (currentState.isTC007) {
                WifiSaveSettingUtil.isSaveSetting = enabled
            } else {
                SaveSettingUtil.isSaveSetting = enabled
            }

            _deviceSettings.value = currentState.copy(
                isSaveSettingEnabled = enabled
            )
        }
    }

    fun performFactoryReset() {
        launchWithErrorHandling {
            // Factory reset implementation
            // Original TS004Repository functionality was removed
        }
    }
}
