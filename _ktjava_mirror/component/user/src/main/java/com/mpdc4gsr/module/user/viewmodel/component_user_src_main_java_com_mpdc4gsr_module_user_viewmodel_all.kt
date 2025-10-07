// Merged .kt under 'component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel' subtree
// Files: 12; Generated 2025-10-07 23:07:45


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\AutoSaveViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
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


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\DeviceDetailsViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceDetailsViewModel : BaseViewModel() {
    companion object {
        private const val TC007_DEMO_SERIAL = "TC007-DEMO-SN"
        private const val TC007_MODEL = "TC007"
        private const val TS004_DEMO_SERIAL = "TS004-DEMO-SN"
        private const val TS004_MODEL = "TS004"
        private const val ERROR_SERIAL = "Error loading SN"
        private const val ERROR_MODEL = "Error loading model"
    }

    private val _serialNumber = MutableStateFlow("N/A")
    val serialNumber: StateFlow<String> = _serialNumber.asStateFlow()
    private val _deviceModel = MutableStateFlow("N/A")
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _copyMessage = MutableStateFlow("")
    val copyMessage: StateFlow<String> = _copyMessage.asStateFlow()
    fun loadDeviceDetails(isTC007: Boolean) {
        launchWithErrorHandling {
            _isLoading.value = true
            try {
                // Note: Original TS004Repository functionality was removed
                // Setting default values as per the original implementation
                if (isTC007) {
                    _serialNumber.value = TC007_DEMO_SERIAL
                    _deviceModel.value = TC007_MODEL
                } else {
                    _serialNumber.value = TS004_DEMO_SERIAL
                    _deviceModel.value = TS004_MODEL
                }
            } catch (e: Exception) {
                _serialNumber.value = ERROR_SERIAL
                _deviceModel.value = ERROR_MODEL
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCopyText(): String {
        return "SN:${_serialNumber.value}  Device Model:${_deviceModel.value}"
    }

    fun setCopyMessage(message: String) {
        _copyMessage.value = message
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\ElectronicManualViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ElectronicManualViewModel : BaseViewModel() {
    data class ManualOption(
        val name: String,
        val isTS001: Boolean
    )

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    private val _options = MutableStateFlow<List<ManualOption>>(emptyList())
    val options: StateFlow<List<ManualOption>> = _options.asStateFlow()
    private val _productType = MutableStateFlow(0)
    val productType: StateFlow<Int> = _productType.asStateFlow()
    fun loadManualOptions(productType: Int) {
        launchWithErrorHandling {
            _productType.value = productType
            val isFAQ = productType != Constants.SETTING_BOOK
            val optionsList = mutableListOf<ManualOption>()
            if (isFAQ) {
                optionsList.add(ManualOption("TS001", true))
            }
            optionsList.add(ManualOption("TS004", false))
            _options.value = optionsList
            _title.value = if (productType == Constants.SETTING_BOOK) {
                "Electronic Manual"
            } else {
                "Questions"
            }
        }
    }

    fun isBookMode(): Boolean = _productType.value == Constants.SETTING_BOOK
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MineFragmentViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineFragmentViewModel : BaseViewModel() {
    data class UserProfileState(
        val username: String = "Guest",
        val avatarUrl: String? = null,
        val isLoggedIn: Boolean = false
    )

    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()
    private val _showWinterPoint = MutableStateFlow(false)
    val showWinterPoint: StateFlow<Boolean> = _showWinterPoint.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        launchWithErrorHandling {
            val userInfoManager = UserInfoManager.getInstance()
            val isLoggedIn = userInfoManager.isLogin()
            _userProfile.value = UserProfileState(
                username = if (isLoggedIn) "User" else "Guest",
                avatarUrl = null,
                isLoggedIn = isLoggedIn
            )
        }
    }

    fun refreshUserProfile() {
        loadUserProfile()
    }

    fun clearCache() {
        launchWithErrorHandling {
            // Clear cache implementation
            // Original implementation used CleanUtils.cleanInternalCache()
        }
    }

    fun onWinterEggClick() {
        _showWinterPoint.value = !_showWinterPoint.value
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MineViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineViewModel : BaseViewModel() {
    data class UserInfo(
        val name: String = "User",
        val email: String = "user@example.com",
        val avatarUrl: String? = null,
        val isLoggedIn: Boolean = false
    )

    data class DeviceInfo(
        val hasLineConnection: Boolean = false,
        val hasTC007: Boolean = false,
        val hasTC007Connection: Boolean = false,
        val tc007Battery: Int? = null,
        val hasTS004: Boolean = false,
        val hasTS004Connection: Boolean = false
    )

    data class AppInfo(
        val version: String = "1.0.0",
        val buildNumber: String = "1000",
        val cacheSize: String = "0 MB",
        val lastUpdated: String = "Never"
    )

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()
    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()
    private val _appInfo = MutableStateFlow(AppInfo())
    val appInfo: StateFlow<AppInfo> = _appInfo.asStateFlow()

    init {
        loadUserInfo()
        loadDeviceInfo()
        loadAppInfo()
    }

    private fun loadUserInfo() {
        launchWithErrorHandling {
            val userInfoManager = UserInfoManager.getInstance()
            val isLoggedIn = userInfoManager.isLogin()
            _userInfo.value = UserInfo(
                name = if (isLoggedIn) "User" else "Guest",
                email = if (isLoggedIn) "user@example.com" else "guest@example.com",
                avatarUrl = null,
                isLoggedIn = isLoggedIn
            )
        }
    }

    private fun loadDeviceInfo() {
        launchWithErrorHandling {
            // Load device connection information
            _deviceInfo.value = DeviceInfo(
                hasLineConnection = false,
                hasTC007 = false,
                hasTC007Connection = false,
                tc007Battery = null,
                hasTS004 = false,
                hasTS004Connection = false
            )
        }
    }

    private fun loadAppInfo() {
        launchWithErrorHandling {
            // Load app information
            _appInfo.value = AppInfo(
                version = "1.10.000",
                buildNumber = "1100",
                cacheSize = "0 MB",
                lastUpdated = "Never"
            )
        }
    }

    fun editUserProfile() {
        launchWithErrorHandling {
            // Navigate to user profile editing
        }
    }

    fun changeAvatar() {
        launchWithErrorHandling {
            // Handle avatar change
        }
    }

    fun openDeviceSettings() {
        launchWithErrorHandling {
            // Navigate to device settings
        }
    }

    fun viewAppLogs() {
        launchWithErrorHandling {
            // Navigate to app logs
        }
    }

    fun clearAppCache() {
        launchWithErrorHandling {
            // Clear app cache
        }
    }

    fun checkForUpdates() {
        launchWithErrorHandling {
            // Check for app updates
        }
    }

    fun refreshData() {
        loadUserInfo()
        loadDeviceInfo()
        loadAppInfo()
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MoreFragmentComposeViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.common.WifiSaveSettingUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
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
        val versionText: String = ""
    )

    private val _deviceSettings = MutableStateFlow(DeviceSettingsState())
    val deviceSettings: StateFlow<DeviceSettingsState> = _deviceSettings.asStateFlow()
    fun initialize(isTC007: Boolean) {
        launchWithErrorHandling {
            val isSaveEnabled = if (isTC007) {
                WifiSaveSettingUtils.isSaveSetting
            } else {
                SaveSettingUtils.isSaveSetting
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
                WifiSaveSettingUtils.isSaveSetting = enabled
            } else {
                SaveSettingUtils.isSaveSetting = enabled
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


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\MoreViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MoreViewModel : BaseViewModel() {
    data class SettingsItem(
        val title: String,
        val subtitle: String,
        val action: SettingsAction
    )

    data class QuickAction(
        val title: String,
        val subtitle: String,
        val actionType: String
    )

    data class HelpResource(
        val title: String,
        val subtitle: String,
        val resourceType: String
    )

    data class CommunityLink(
        val title: String,
        val subtitle: String,
        val linkType: String
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
    private val _quickActions = MutableStateFlow<List<QuickAction>>(emptyList())
    val quickActions: StateFlow<List<QuickAction>> = _quickActions.asStateFlow()
    private val _helpResources = MutableStateFlow<List<HelpResource>>(emptyList())
    val helpResources: StateFlow<List<HelpResource>> = _helpResources.asStateFlow()
    private val _communityLinks = MutableStateFlow<List<CommunityLink>>(emptyList())
    val communityLinks: StateFlow<List<CommunityLink>> = _communityLinks.asStateFlow()
    private val _isUpgradeAvailable = MutableStateFlow(false)
    val isUpgradeAvailable: StateFlow<Boolean> = _isUpgradeAvailable.asStateFlow()

    init {
        loadSettingsItems()
        loadQuickActions()
        loadHelpResources()
        loadCommunityLinks()
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
            // Check for firmware/app updates
            // In a real implementation, this would call an update service or check a remote version API
            // For now, simulate the check and set to false (no updates available)
            kotlinx.coroutines.delay(500) // Simulate network delay
            _isUpgradeAvailable.value = false
        }
    }

    private fun loadQuickActions() {
        launchWithErrorHandling {
            val actions = listOf(
                QuickAction(
                    title = "Quick Calibration",
                    subtitle = "Start thermal camera calibration",
                    actionType = "startQuickCalibration"
                ),
                QuickAction(
                    title = "Export Data",
                    subtitle = "Export thermal images and logs",
                    actionType = "exportData"
                ),
                QuickAction(
                    title = "Share Analysis",
                    subtitle = "Share thermal analysis results",
                    actionType = "shareAnalysis"
                )
            )
            _quickActions.value = actions
        }
    }

    private fun loadHelpResources() {
        launchWithErrorHandling {
            val resources = listOf(
                HelpResource(
                    title = "User Guide",
                    subtitle = "Complete user manual and tutorials",
                    resourceType = "USER_GUIDE"
                ),
                HelpResource(
                    title = "FAQ",
                    subtitle = "Frequently asked questions",
                    resourceType = "FAQ"
                ),
                HelpResource(
                    title = "Troubleshooting",
                    subtitle = "Common issues and solutions",
                    resourceType = "TROUBLESHOOTING"
                )
            )
            _helpResources.value = resources
        }
    }

    private fun loadCommunityLinks() {
        launchWithErrorHandling {
            val links = listOf(
                CommunityLink(
                    title = "Advanced Analysis",
                    subtitle = "Access advanced thermal analysis tools",
                    linkType = "openAdvancedAnalysis"
                ),
                CommunityLink(
                    title = "Batch Processing",
                    subtitle = "Process multiple thermal images",
                    linkType = "openBatchProcessing"
                ),
                CommunityLink(
                    title = "AI Detection",
                    subtitle = "Use AI for thermal anomaly detection",
                    linkType = "openAIDetection"
                )
            )
            _communityLinks.value = links
        }
    }

    fun startQuickCalibration() {
        launchWithErrorHandling {
            // Start quick calibration process
        }
    }

    fun exportData() {
        launchWithErrorHandling {
            // Export thermal data
        }
    }

    fun shareAnalysis() {
        launchWithErrorHandling {
            // Share analysis results
        }
    }

    fun openAdvancedAnalysis() {
        launchWithErrorHandling {
            // Open advanced analysis tools
        }
    }

    fun openBatchProcessing() {
        launchWithErrorHandling {
            // Open batch processing interface
        }
    }

    fun openAIDetection() {
        launchWithErrorHandling {
            // Open AI detection interface
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\QuestionDetailsViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestionDetailsViewModel : BaseViewModel() {
    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()
    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()
    fun loadQuestionDetails(question: String?, answer: String?) {
        _question.value = question ?: ""
        _answer.value = answer ?: ""
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\QuestionViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.module.user.model.FaqRepository
import com.mpdc4gsr.module.user.model.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestionViewModel : BaseViewModel() {
    private val _questions = MutableStateFlow<List<QuestionData>>(emptyList())
    val questions: StateFlow<List<QuestionData>> = _questions.asStateFlow()
    fun loadQuestions(isTS001: Boolean) {
        launchWithErrorHandling {
            val questionList = FaqRepository.getQuestionList(isTS001)
            _questions.value = questionList
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\StorageSpaceViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat

class StorageSpaceViewModel : BaseViewModel() {
    companion object {
        private const val MOCK_TOTAL_SPACE = 32_000_000_000L
        private const val MOCK_USED_SPACE = 12_000_000_000L
        private const val MOCK_FREE_SPACE = 20_000_000_000L
        private const val MOCK_PHOTO_SPACE = 5_000_000_000L
        private const val MOCK_VIDEO_SPACE = 6_000_000_000L
        private const val MOCK_SYSTEM_SPACE = 1_000_000_000L
    }

    data class StorageInfo(
        val totalSpace: Long = 0L,
        val usedSpace: Long = 0L,
        val freeSpace: Long = 0L,
        val photoSpace: Long = 0L,
        val videoSpace: Long = 0L,
        val systemSpace: Long = 0L
    )

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()
    fun loadStorageInfo() {
        launchWithErrorHandling {
            // Original TS004Repository functionality removed - use mock data
            val mockStorageInfo = StorageInfo(
                totalSpace = MOCK_TOTAL_SPACE,
                usedSpace = MOCK_USED_SPACE,
                freeSpace = MOCK_FREE_SPACE,
                photoSpace = MOCK_PHOTO_SPACE,
                videoSpace = MOCK_VIDEO_SPACE,
                systemSpace = MOCK_SYSTEM_SPACE
            )
            _storageInfo.value = mockStorageInfo
        }
    }

    fun getUsagePercentage(): Float {
        val info = _storageInfo.value
        return if (info.totalSpace > 0) {
            (info.usedSpace.toFloat() / info.totalSpace.toFloat())
        } else {
            0f
        }
    }

    fun formatFileSize(fileSize: Long): String {
        return when {
            fileSize == 0L -> "0 B"
            fileSize < 1024 -> DecimalFormat("#.0").format(fileSize.toDouble()) + " B"
            fileSize < 1048576 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1024) + " KB"
            fileSize < 1073741824 -> DecimalFormat("#.0").format(fileSize.toDouble() / 1048576) + " MB"
            else -> DecimalFormat("#.0").format(fileSize.toDouble() / 1073741824) + " GB"
        }
    }

    fun formatStorage() {
        launchWithErrorHandling {
            // Original format storage operation removed - just show confirmation
            // In real implementation, this would format the storage
        }
    }
}


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\TISRViewModel.kt =====

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


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\viewmodel\UnitViewModel.kt =====

package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnitViewModel : BaseViewModel() {
    companion object {
        const val CELSIUS = 1
        const val FAHRENHEIT = 0
    }

    private val _selectedUnit = MutableStateFlow(CELSIUS)
    val selectedUnit: StateFlow<Int> = _selectedUnit.asStateFlow()

    init {
        loadTemperatureUnit()
    }

    private fun loadTemperatureUnit() {
        _selectedUnit.value = SharedManager.getTemperature()
    }

    fun selectUnit(unit: Int) {
        launchWithErrorHandling {
            _selectedUnit.value = unit
            // Don't save immediately, wait for user to confirm with save button
        }
    }

    fun saveTemperatureUnit() {
        launchWithErrorHandling {
            SharedManager.setTemperature(_selectedUnit.value)
        }
    }

    fun isCelsiusSelected(): Boolean = _selectedUnit.value == CELSIUS
    fun isFahrenheitSelected(): Boolean = _selectedUnit.value == FAHRENHEIT
}


