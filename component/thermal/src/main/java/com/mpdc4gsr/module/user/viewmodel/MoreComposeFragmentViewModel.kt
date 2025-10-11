package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.component.shared.app.common.SaveSettingUtils
import com.mpdc4gsr.component.shared.app.common.WifiSaveSettingUtils
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MoreComposeFragmentViewModel : BaseViewModel() {
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
        private const val DEFAULT_UPGRADE_AVAILABLE = false
    }

    data class DeviceSettingsState(
        val isTC007: Boolean = false,
        val isSaveSettingEnabled: Boolean = false,
        val hasUpgrade: Boolean = false,
        val versionText: String = "",
    )

    private val _deviceSettings = MutableStateFlow(DeviceSettingsState())
    val deviceSettings: StateFlow<DeviceSettingsState> = _deviceSettings.asStateFlow()

    fun initialize(isTC007: Boolean) {
        launchWithErrorHandling {
            val isSaveEnabled =
                if (isTC007) {
                    WifiSaveSettingUtils.isSaveSetting
                } else {
                    SaveSettingUtils.isSaveSetting
                }
            _deviceSettings.value =
                DeviceSettingsState(
                    isTC007 = isTC007,
                    isSaveSettingEnabled = isSaveEnabled,
                    hasUpgrade = DEFAULT_UPGRADE_AVAILABLE,
                    versionText = DEFAULT_VERSION,
                )
        }
    }

    fun updateSaveSetting(enabled: Boolean) {
        launchWithErrorHandling {
            val currentState = _deviceSettings.value
            if (currentState.isTC007) {
                WifiSaveSettingUtils.isSaveSetting = enabled
            } else {
                SaveSettingUtils.isSaveSetting = enabled
            }
            _deviceSettings.value =
                currentState.copy(
                    isSaveSettingEnabled = enabled,
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


