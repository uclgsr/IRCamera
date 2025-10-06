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
