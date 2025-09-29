package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for More (Settings Hub) Activity
 * Manages the settings menu options and navigation
 */
class MoreViewModel : BaseViewModel() {

    data class SettingsItem(
        val title: String,
        val subtitle: String,
        val action: SettingsAction
    )

    enum class SettingsAction {
        DEVICE_INFORMATION,
        TISR,
        STORAGE_SPACE,
        AUTO_SAVE,
        UNIT,
        VERSION,
        DISCONNECT,
        RESET
    }

    private val _settingsItems = MutableStateFlow<List<SettingsItem>>(emptyList())
    val settingsItems: StateFlow<List<SettingsItem>> = _settingsItems.asStateFlow()

    private val _isUpgradeAvailable = MutableStateFlow(false)
    val isUpgradeAvailable: StateFlow<Boolean> = _isUpgradeAvailable.asStateFlow()

    init {
        loadSettingsItems()
    }

    private fun loadSettingsItems() {
        launchWithErrorHandling {
            val items = listOf(
                SettingsItem(
                    title = "Device Information",
                    subtitle = "View device details and specifications",
                    action = SettingsAction.DEVICE_INFORMATION
                ),
                SettingsItem(
                    title = "TISR",
                    subtitle = "Temperature Image Super Resolution settings",
                    action = SettingsAction.TISR
                ),
                SettingsItem(
                    title = "Storage Space",
                    subtitle = "Manage device storage and format options",
                    action = SettingsAction.STORAGE_SPACE
                ),
                SettingsItem(
                    title = "Auto Save",
                    subtitle = "Automatically save images to device",
                    action = SettingsAction.AUTO_SAVE
                ),
                SettingsItem(
                    title = "Temperature Unit",
                    subtitle = "Choose Celsius or Fahrenheit",
                    action = SettingsAction.UNIT
                ),
                SettingsItem(
                    title = "Version Information",
                    subtitle = "App version and update information",
                    action = SettingsAction.VERSION
                ),
                SettingsItem(
                    title = "Disconnect Device",
                    subtitle = "Disconnect from thermal camera",
                    action = SettingsAction.DISCONNECT
                ),
                SettingsItem(
                    title = "Factory Reset",
                    subtitle = "Reset device to factory settings",
                    action = SettingsAction.RESET
                )
            )
            _settingsItems.value = items
        }
    }

    fun checkForUpdates() {
        launchWithErrorHandling {
            // Mock update check - in real implementation would check for firmware updates
            _isUpgradeAvailable.value = false
        }
    }
}