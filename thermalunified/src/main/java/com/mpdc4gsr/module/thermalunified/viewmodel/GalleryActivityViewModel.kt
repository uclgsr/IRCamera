package com.mpdc4gsr.module.thermalunified.viewmodel

import android.Manifest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.launch

class GalleryActivityViewModel : BaseViewModel() {
    // Permission state management
    data class PermissionState(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<String>,
        val targetSdk: Int,
    )

    // ViewPager state management
    sealed class ViewPagerState {
        object Ready : ViewPagerState()

        data class TabSelected(
            val position: Int,
        ) : ViewPagerState()
    }

    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState = _permissionState
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState

    fun initializePermissions(targetSdkVersion: Int) {
        viewModelScope.launch {
            val requiredPermissions = getRequiredPermissions(targetSdkVersion)
            val permissionState =
                PermissionState(
                    hasAllPermissions = false, // Will be checked by permission tool
                    missingPermissions = requiredPermissions,
                    targetSdk = targetSdkVersion,
                )
            _permissionState.value = permissionState
        }
    }

    fun onPermissionsResult(isSuccess: Boolean) {
        viewModelScope.launch {
            if (isSuccess) {
                val currentState = _permissionState.value
                _permissionState.value = currentState?.copy(hasAllPermissions = true)
                _viewPagerState.value = ViewPagerState.Ready
            }
        }
    }

    fun selectTab(position: Int) {
        _viewPagerState.value = ViewPagerState.TabSelected(position)
    }

    private fun getRequiredPermissions(targetSdkVersion: Int): List<String> =
        when {
            targetSdkVersion >= 34 ->
                listOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )

            targetSdkVersion >= 33 ->
                listOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )

            else ->
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
        }
}
