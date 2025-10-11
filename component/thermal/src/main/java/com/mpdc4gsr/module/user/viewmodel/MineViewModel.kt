package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.component.shared.app.common.UserInfoManager
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineViewModel : BaseViewModel() {
    data class UserInfo(
        val name: String = "User",
        val email: String = "user@example.com",
        val avatarUrl: String? = null,
        val isLoggedIn: Boolean = false,
    )

    data class DeviceInfo(
        val hasLineConnection: Boolean = false,
        val hasTC007: Boolean = false,
        val hasTC007Connection: Boolean = false,
        val tc007Battery: Int? = null,
        val hasTS004: Boolean = false,
        val hasTS004Connection: Boolean = false,
    )

    data class AppInfo(
        val version: String = "1.0.0",
        val buildNumber: String = "1000",
        val cacheSize: String = "0 MB",
        val lastUpdated: String = "Never",
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
            _userInfo.value =
                UserInfo(
                    name = if (isLoggedIn) "User" else "Guest",
                    email = if (isLoggedIn) "user@example.com" else "guest@example.com",
                    avatarUrl = null,
                    isLoggedIn = isLoggedIn,
                )
        }
    }

    private fun loadDeviceInfo() {
        launchWithErrorHandling {
            // Load device connection information
            _deviceInfo.value =
                DeviceInfo(
                    hasLineConnection = false,
                    hasTC007 = false,
                    hasTC007Connection = false,
                    tc007Battery = null,
                    hasTS004 = false,
                    hasTS004Connection = false,
                )
        }
    }

    private fun loadAppInfo() {
        launchWithErrorHandling {
            // Load app information
            _appInfo.value =
                AppInfo(
                    version = "1.10.000",
                    buildNumber = "1100",
                    cacheSize = "0 MB",
                    lastUpdated = "Never",
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


