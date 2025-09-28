package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.common.UserInfoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Mine Fragment Compose
 * Manages user profile and settings navigation
 */
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
            val userInfo = UserInfoManager.getUserInfo()
            _userProfile.value = UserProfileState(
                username = userInfo?.username ?: "Guest",
                avatarUrl = userInfo?.avatarUrl,
                isLoggedIn = userInfo != null
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
