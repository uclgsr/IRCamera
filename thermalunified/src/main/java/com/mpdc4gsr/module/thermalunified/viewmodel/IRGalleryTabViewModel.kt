package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRGalleryTabViewModel : BaseViewModel() {
    val isEditModeLD: MutableLiveData<Boolean> = MutableLiveData(false)
    val selectSizeLD: MutableLiveData<Int> = MutableLiveData(0)
    val selectAllIndex: MutableLiveData<Int> = MutableLiveData(0)

    // StateFlow properties for Compose
    private val _currentDirType = MutableStateFlow(DirType.LINE)
    val currentDirType: StateFlow<DirType> = _currentDirType.asStateFlow()
    private val _canSwitchDir = MutableStateFlow(true)
    val canSwitchDir: StateFlow<Boolean> = _canSwitchDir.asStateFlow()
    private val _hasBackIcon = MutableStateFlow(false)
    val hasBackIcon: StateFlow<Boolean> = _hasBackIcon.asStateFlow()

    // Methods for Compose fragment
    fun changeDirType(dirType: DirType) {
        _currentDirType.value = dirType
    }

    fun setCanSwitchDir(canSwitch: Boolean) {
        _canSwitchDir.value = canSwitch
    }

    fun setHasBackIcon(hasIcon: Boolean) {
        _hasBackIcon.value = hasIcon
    }

    fun navigateBack() {
        // Emit navigation back event to be handled by the fragment/activity
        // The fragment should observe uiEvents and finish the activity when NavigateBack is received
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.NavigateBack)
        }
    }

    fun showSearch() {
        // Placeholder for search functionality
    }

    fun showMoreOptions() {
        // Placeholder for more options functionality
    }
}
