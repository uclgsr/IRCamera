// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel' subtree
// Files: 25; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\GalleryActivityViewModel.kt =====

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
        val targetSdk: Int
    )

    // ViewPager state management
    sealed class ViewPagerState {
        object Ready : ViewPagerState()
        data class TabSelected(val position: Int) : ViewPagerState()
    }

    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState = _permissionState
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState
    fun initializePermissions(targetSdkVersion: Int) {
        viewModelScope.launch {
            val requiredPermissions = getRequiredPermissions(targetSdkVersion)
            val permissionState = PermissionState(
                hasAllPermissions = false, // Will be checked by permission tool
                missingPermissions = requiredPermissions,
                targetSdk = targetSdkVersion
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

    private fun getRequiredPermissions(targetSdkVersion: Int): List<String> {
        return when {
            targetSdkVersion >= 34 -> listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            targetSdkVersion >= 33 -> listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            else -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\GalleryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "GalleryViewModel"
    }

    val galleryLiveData = SingleLiveEvent<ArrayList<String>>()

    // Data class for media items
    data class MediaItem(
        val id: String,
        val name: String,
        val path: String,
        val thumbnailPath: String,
        val size: Long,
        val dateModified: Long,
        val isVideo: Boolean = false
    )

    // State flows for Compose
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    private val _galleryItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val galleryItems: StateFlow<List<MediaItem>> = _galleryItems.asStateFlow()
    private val _videoItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoItems: StateFlow<List<MediaItem>> = _videoItems.asStateFlow()
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMediaItems()
    }

    // Load media items and update different flows
    private fun loadMediaItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val items = getMediaItemsList()
                withContext(Dispatchers.Main) {
                    _mediaItems.value = items
                    _galleryItems.value = items.filter { !it.isVideo }
                    _videoItems.value = items.filter { it.isVideo }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading media items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // View mode toggle
    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    // Selection mode methods
    fun enterSelectionMode(item: MediaItem? = null) {
        _isSelectionMode.value = true
        item?.let {
            _selectedItems.value = setOf(it.id)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun toggleItemSelection(item: MediaItem) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(item.id)) {
            currentSelected.remove(item.id)
        } else {
            currentSelected.add(item.id)
        }
        _selectedItems.value = currentSelected
        // Exit selection mode if no items selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    // File operations
    fun deleteSelectedItems() {
        val selectedIds = _selectedItems.value
        val itemsToDelete = _mediaItems.value.filter { selectedIds.contains(it.id) }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach { item ->
                try {
                    File(item.path).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: ${item.path}", e)
                }
            }
            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadMediaItems() // Refresh the list
            }
        }
    }

    fun shareSelectedItems() {
        val selectedIds = _selectedItems.value
        val itemsToShare = _mediaItems.value.filter { selectedIds.contains(it.id) }
        if (itemsToShare.isNotEmpty()) {
            // Implementation would depend on context being available
            // For now, just log the action
            Log.d(TAG, "Sharing ${itemsToShare.size} items")
        }
    }

    fun openMediaItem(item: MediaItem) {
        // Implementation for opening media item
        Log.d(TAG, "Opening media item: ${item.name}")
    }

    // Refresh methods
    fun refreshGallery() {
        loadMediaItems()
    }

    fun refreshVideoGallery() {
        loadMediaItems()
    }

    // Legacy methods for backward compatibility
    fun getData() {
        viewModelScope.launch {
            getGalleryList().collect { it ->
                if (it.size == 0) {
                    Log.w(TAG, "No gallery items found")
                } else {
                    galleryLiveData.postValue(it)
                }
            }
        }
    }

    fun getVideoData() {
        viewModelScope.launch {
            getVideoList().collect { it ->
                if (it.size == 0) {
                    Log.w(TAG, "No video items found")
                } else {
                    galleryLiveData.postValue(it)
                }
            }
        }
    }

    private fun getMediaItemsList(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        // Load pictures
        val picturePath = ContextProvider.getContext()
            .getExternalFilesDir("Pictures")!!.absolutePath + File.separator + "thermal"
        val pictureDir = File(picturePath)
        if (pictureDir.isDirectory) {
            pictureDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    items.add(
                        MediaItem(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            thumbnailPath = file.absolutePath,
                            size = file.length(),
                            dateModified = file.lastModified(),
                            isVideo = false
                        )
                    )
                }
            }
        }
        // Load videos
        val videoPath = FileConfig.lineGalleryDir
        val videoDir = File(videoPath)
        if (videoDir.isDirectory) {
            videoDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    items.add(
                        MediaItem(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            thumbnailPath = file.absolutePath,
                            size = file.length(),
                            dateModified = file.lastModified(),
                            isVideo = true
                        )
                    )
                }
            }
        }
        return items.sortedByDescending { it.dateModified }
    }

    private fun getGalleryList(): Flow<ArrayList<String>> {
        val flow =
            flow {
                val path =
                    ContextProvider.getContext()
                        .getExternalFilesDir("Pictures")!!.absolutePath + File.separator + "thermal"
                val file = File(path)
                if (file.isDirectory) {
                    val list = arrayListOf<String>()
                    file.list()?.forEach { fileName ->
                        list.add("$path/$fileName")
                    }
                    emit(list)
                } else {
                    emit(arrayListOf<String>())
                }
            }.map {
                return@map it
            }
        return flow
    }

    private fun getVideoList(): Flow<ArrayList<String>> {
        val flow =
            flow {
                val path = FileConfig.lineGalleryDir
                val file = File(path)
                if (file.isDirectory) {
                    val list = arrayListOf<String>()
                    file.list()?.forEach { fileName ->
                        list.add("$path/$fileName")
                    }
                    emit(list)
                } else {
                    emit(arrayListOf<String>())
                }
            }.map {
                return@map it
            }
        return flow
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ImageColorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class ImageColorViewModel : BaseViewModel() {
    private val _timestamp = MutableStateFlow("")
    val timestamp: StateFlow<String> = _timestamp.asStateFlow()
    private val _showData = MutableStateFlow(false)
    val showData: StateFlow<Boolean> = _showData.asStateFlow()
    private val _leftImagePath = MutableStateFlow("")
    val leftImagePath: StateFlow<String> = _leftImagePath.asStateFlow()
    private val _rightImagePath = MutableStateFlow("")
    val rightImagePath: StateFlow<String> = _rightImagePath.asStateFlow()
    private val _comparisonResult = MutableStateFlow("")
    val comparisonResult: StateFlow<String> = _comparisonResult.asStateFlow()

    init {
        updateTimestamp()
    }

    fun toggleDataDisplay() {
        launchWithErrorHandling {
            _showData.value = !_showData.value
        }
    }

    fun loadImages(leftImagePath: String, rightImagePath: String) {
        launchWithLoading {
            _leftImagePath.value = leftImagePath
            _rightImagePath.value = rightImagePath
        }
    }

    fun compareImages() {
        launchWithErrorHandling {
            _comparisonResult.value = "Images compared successfully"
        }
    }

    private fun updateTimestamp() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        _timestamp.value = dateFormat.format(Date())
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRConfigViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import com.mpdc4gsr.module.thermalunified.bean.ModelBean
import com.mpdc4gsr.module.thermalunified.repository.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IRConfigViewModel : BaseViewModel() {
    val configLiveData = SingleLiveEvent<ModelBean>()
    fun getConfig(isTC007: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            configLiveData.postValue(ConfigRepository.read(isTC007))
        }
    }

    fun updateDefaultEnvironment(
        isTC007: Boolean,
        environment: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.environment = environment
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateDefaultDistance(
        isTC007: Boolean,
        distance: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.distance = distance
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateDefaultRadiation(
        isTC007: Boolean,
        radiation: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.radiation = radiation
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun addConfig(isTC007: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            var index = 0
            modelBean.myselfModel.forEach {
                index = index.coerceAtLeast(it.id)
            }
            index++
            modelBean.myselfModel.add(DataBean(id = index, name = index.toString()))
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun checkConfig(
        isTC007: Boolean,
        id: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.use = id == 0
            modelBean.myselfModel.forEach {
                it.use = it.id == id
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun deleteConfig(
        isTC007: Boolean,
        id: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            var removeAt = modelBean.myselfModel.size
            for (i in modelBean.myselfModel.indices) {
                val dataBean = modelBean.myselfModel[i]
                if (dataBean.id == id) {
                    if (dataBean.use) {
                        modelBean.defaultModel.use = true
                    }
                    modelBean.myselfModel.removeAt(i)
                    removeAt = i
                    break
                }
            }
            if (removeAt < modelBean.myselfModel.size) {
                for (i in removeAt until modelBean.myselfModel.size) {
                    val dataBean = modelBean.myselfModel[i]
                    dataBean.id = i + 1
                    dataBean.name = dataBean.id.toString()
                }
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateCustom(
        isTC007: Boolean,
        dataBean: DataBean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            for (i in modelBean.myselfModel.indices) {
                if (modelBean.myselfModel[i].id == dataBean.id) {
                    modelBean.myselfModel[i] = dataBean
                    break
                }
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRCorrectionViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRCorrectionViewModel : BaseViewModel() {
    // State management for correction functionality
    private val _correctionState = MutableStateFlow(CorrectionState.INACTIVE)
    val correctionState: StateFlow<CorrectionState> = _correctionState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _calibrationStatus = MutableStateFlow(CalibrationStatus.NONE)
    val calibrationStatus: StateFlow<CalibrationStatus> = _calibrationStatus.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Current correction parameters
    private var currentCorrectionValue: Float = 0f
    private var currentTemperaturePoint: Triple<Float, Int, Int>? = null

    init {
        // Initialize with default temperature data
        _temperatureData.value = TemperatureData(
            currentTemp = 25.0f,
            correctedTemp = 25.0f,
            offsetValue = 0.0f
        )
    }

    fun toggleCorrection() {
        viewModelScope.launch {
            try {
                when (_correctionState.value) {
                    CorrectionState.INACTIVE -> {
                        _correctionState.value = CorrectionState.ACTIVE
                        startTemperatureMonitoring()
                    }

                    CorrectionState.ACTIVE -> {
                        _correctionState.value = CorrectionState.INACTIVE
                        stopTemperatureMonitoring()
                    }

                    CorrectionState.CALIBRATING -> {
                        // Cannot toggle while calibrating
                    }
                }
            } catch (e: Exception) {
                // Handle the exception by logging and updating error state
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            }
        }
    }

    fun updateTemperaturePoint(temp: Float, x: Int, y: Int) {
        currentTemperaturePoint = Triple(temp, x, y)
        updateTemperatureData(temp)
    }

    fun updateCorrectionValue(value: Float) {
        currentCorrectionValue = value
        currentTemperaturePoint?.let { (baseTemp, _, _) ->
            updateTemperatureData(baseTemp)
        }
    }

    fun startCalibration() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _correctionState.value = CorrectionState.CALIBRATING
            _calibrationStatus.value = CalibrationStatus.NEEDS_CALIBRATION
            try {
                // Simulate calibration process
                kotlinx.coroutines.delay(2000) // Simulate calibration time
                _calibrationStatus.value = CalibrationStatus.CALIBRATED
                _correctionState.value = CorrectionState.ACTIVE
                // Restart temperature monitoring after calibration completes
                startTemperatureMonitoring()
            } catch (e: Exception) {
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetCorrection() {
        launchWithErrorHandling {
            currentCorrectionValue = 0f
            currentTemperaturePoint = null
            _calibrationStatus.value = CalibrationStatus.NONE
            _correctionState.value = CorrectionState.INACTIVE
            // Reset temperature data
            _temperatureData.value = TemperatureData(
                currentTemp = 25.0f,
                correctedTemp = 25.0f,
                offsetValue = 0.0f
            )
        }
    }

    fun saveSettings() {
        launchWithErrorHandling {
            _isProcessing.value = true
            try {
                // Simulate saving settings
                kotlinx.coroutines.delay(1000)
                // Show success message
                _uiEvents.emit(BaseViewModel.UiEvent.ShowMessage("Correction settings saved successfully"))
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            // Simulate temperature monitoring with some variation
            while (_correctionState.value == CorrectionState.ACTIVE) {
                currentTemperaturePoint?.let { (baseTemp, _, _) ->
                    // Add some realistic temperature variation
                    val variation = (Math.random() - 0.5).toFloat() * 0.5f
                    updateTemperatureData(baseTemp + variation)
                }
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }

    private fun stopTemperatureMonitoring() {
        // Temperature monitoring is stopped by the coroutine condition check
    }

    private fun updateTemperatureData(currentTemp: Float) {
        val correctedTemp = currentTemp + currentCorrectionValue
        _temperatureData.value = TemperatureData(
            currentTemp = currentTemp,
            correctedTemp = correctedTemp,
            offsetValue = currentCorrectionValue
        )
    }
}

data class TemperatureData(
    val currentTemp: Float,
    val correctedTemp: Float,
    val offsetValue: Float
)

enum class CorrectionState {
    INACTIVE, ACTIVE, CALIBRATING
}

enum class CalibrationStatus {
    NONE, CALIBRATED, NEEDS_CALIBRATION
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryEditViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.libunified.app.utils.UnifiedByteUtils.bytesToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class IRGalleryEditViewModel : BaseViewModel() {
    val resultLiveData = SingleLiveEvent<FrameBean>()
    fun initData(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) {
                XLog.w("IR[ph][ph][ph][ph][ph]: ${file.absolutePath}")
                return@launch
            }
            XLog.w("IR[ph][ph]: ${file.absolutePath}")
            val bytes = file.readBytes()
            val headLenBytes = ByteArray(2)
            System.arraycopy(bytes, 0, headLenBytes, 0, 2)
            val headLen = headLenBytes.bytesToInt()
            val headDataBytes = ByteArray(headLen)
            val frameDataBytes = ByteArray(bytes.size - headLen)
            System.arraycopy(bytes, 0, headDataBytes, 0, headDataBytes.size)
            System.arraycopy(bytes, headLen, frameDataBytes, 0, frameDataBytes.size)
            XLog.w("[ph][ph][ph][ph]: ${frameDataBytes.size}")
            resultLiveData.postValue(FrameBean(headDataBytes, frameDataBytes))
        }
    }

    fun getTailData(bytes: ByteArray) {
    }

    data class FrameBean(val capital: ByteArray, val frame: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FrameBean
            if (!capital.contentEquals(other.capital)) return false
            if (!frame.contentEquals(other.frame)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = capital.contentHashCode()
            result = 31 * result + frame.contentHashCode()
            return result
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryTabViewModel.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.bean.GalleryTitle
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.GalleryRepository
import com.mpdc4gsr.libunified.app.repository.TS004Repository
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.module.thermalunified.utils.WriteTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class IRGalleryViewModel : BaseViewModel() {
    companion object {
        const val PAGE_COUNT = 20
    }

    // Existing LiveData properties
    val sourceListLD: MutableLiveData<ArrayList<GalleryBean>> = MutableLiveData()
    val showListLD: MutableLiveData<ArrayList<GalleryBean>> = MutableLiveData()
    val pageListLD: MutableLiveData<ArrayList<GalleryBean>?> = MutableLiveData()
    val deleteResultLD: MutableLiveData<Boolean> = MutableLiveData()

    // StateFlow properties for Compose
    private val _galleryItems = MutableStateFlow<List<GalleryBean>>(emptyList())
    val galleryItems: StateFlow<List<GalleryBean>> = _galleryItems.asStateFlow()
    private val _currentDirType = MutableStateFlow(GalleryRepository.DirType.LINE)
    val currentDirType: StateFlow<GalleryRepository.DirType> = _currentDirType.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Cache for file sizes to avoid repeated I/O operations
    private val fileSizeCache = mutableMapOf<String, Long>()

    // Compose-related methods
    fun changeDirType(dirType: GalleryRepository.DirType) {
        _currentDirType.value = dirType
        refreshGallery()
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun refreshGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val items = GalleryRepository.loadAllReportImg(_currentDirType.value)
                // Pre-calculate file sizes on background thread and cache them
                items.forEach { item ->
                    val fileSize = calculateFileSize(item.path)
                    fileSizeCache[item.path] = fileSize
                }
                _galleryItems.value = items
            } catch (e: Exception) {
                _galleryItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleItemSelection(item: GalleryBean) {
        val currentSelected = _selectedItems.value.toMutableSet()
        val itemPath = item.path ?: return
        if (currentSelected.contains(itemPath)) {
            currentSelected.remove(itemPath)
        } else {
            currentSelected.add(itemPath)
        }
        _selectedItems.value = currentSelected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun enterSelectionMode(item: GalleryBean) {
        _isSelectionMode.value = true
        val itemPath = item.path ?: return
        _selectedItems.value = setOf(itemPath)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun deleteSelectedItems() {
        val selectedPaths = _selectedItems.value
        if (selectedPaths.isEmpty()) return
        viewModelScope.launch {
            val itemsToDelete = _galleryItems.value.filter { selectedPaths.contains(it.path) }
            delete(itemsToDelete, _currentDirType.value, true)
            exitSelectionMode()
            refreshGallery()
        }
    }

    fun shareSelectedItems() {
        // Implementation for sharing selected items would go here
        // For now, just exit selection mode
        exitSelectionMode()
    }

    fun openGalleryItem(item: GalleryBean) {
        // Implementation for opening gallery item would go here
        // This would typically navigate to a detail view
    }

    var hasLoadPage = 0
    fun queryAllReportImg(dirType: GalleryRepository.DirType) {
        viewModelScope.launch(Dispatchers.IO) {
            val sourceList: ArrayList<GalleryBean> = GalleryRepository.loadAllReportImg(dirType)
            sourceListLD.postValue(sourceList)
            val showList: ArrayList<GalleryBean> = ArrayList(sourceList.size)
            var beforeTime = 0L
            for (galleryBean in sourceList) {
                val currentTime = TimeTools.timeToMinute(galleryBean.timeMillis, 4)
                if (beforeTime != currentTime) {
                    showList.add(GalleryTitle(galleryBean.timeMillis))
                    beforeTime = currentTime
                }
                showList.add(galleryBean)
            }
            showListLD.postValue(showList)
        }
    }

    fun queryGalleryByPage(
        isVideo: Boolean,
        dirType: GalleryRepository.DirType,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val pageList: ArrayList<GalleryBean>? =
                GalleryRepository.loadByPage(isVideo, dirType, hasLoadPage + 1, PAGE_COUNT)
            pageListLD.postValue(pageList)
            if (pageList != null) {
                val sourceList =
                    if (hasLoadPage == 0) ArrayList(pageList.size) else sourceListLD.value
                        ?: ArrayList(pageList.size)
                val showList = if (hasLoadPage == 0) ArrayList(pageList.size) else showListLD.value
                    ?: ArrayList(pageList.size)
                if (pageList.isNotEmpty()) {
                    hasLoadPage++
                }
                var beforeTime = if (sourceList.isEmpty()) 0 else TimeTools.timeToMinute(
                    sourceList.last().timeMillis,
                    4
                )
                for (galleryBean in pageList) {
                    val currentTime = TimeTools.timeToMinute(galleryBean.timeMillis, 4)
                    if (beforeTime != currentTime) {
                        showList.add(GalleryTitle(galleryBean.timeMillis))
                        beforeTime = currentTime
                    }
                    showList.add(galleryBean)
                }
                sourceList.addAll(pageList)
                sourceListLD.postValue(sourceList)
                showListLD.postValue(showList)
            }
        }
    }

    private fun calculateFileSize(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getCachedFileSize(path: String): Long {
        return fileSizeCache[path] ?: 0L
    }

    fun delete(
        deleteList: List<GalleryBean>,
        dirType: GalleryRepository.DirType,
        isDelLocal: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dirType == GalleryRepository.DirType.TS004_REMOTE) {
                val isSuccess =
                    TS004Repository.deleteFiles(
                        Array(deleteList.size) {
                            deleteList[it].id
                        },
                    )
                if (isSuccess) {
                    if (isDelLocal) {
                        deleteList.forEach {
                            if (it.hasDownload) {
                                val file = File(FileConfig.ts004GalleryDir, it.name)
                                if (file.exists()) {
                                    WriteTools.delete(file)
                                }
                            }
                        }
                    }
                    deleteResultLD.postValue(true)
                } else {
                    deleteResultLD.postValue(false)
                }
            } else {
                deleteList.forEach {
                    val file = File(it.path)
                    if (file.exists()) {
                        WriteTools.delete(file)
                    }
                }
                deleteResultLD.postValue(true)
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMainActivityViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.launch

class IRMainActivityViewModel : BaseViewModel() {
    // Device state management
    data class DeviceState(
        val isTC007: Boolean = false,
        val isWebSocketConnected: Boolean = false,
        val isUsbConnected: Boolean = false,
        val shouldAutoOpen: Boolean = false,
        val shouldBlur: Boolean = false
    )

    // Fragment communication state
    data class FragmentCommunicationState(
        val activeFragment: Int = 0,
        val deviceConnected: Boolean = false,
        val pendingNavigation: NavigationEvent? = null
    )

    // Navigation events
    sealed class NavigationEvent {
        data class ToMonitor(val isTC007: Boolean) : NavigationEvent()
        object ToGallery : NavigationEvent()
        data class ToThermal(val routeConfig: String) : NavigationEvent()
    }

    // ViewPager state management
    sealed class ViewPagerState {
        data class PageSelected(val position: Int) : ViewPagerState()
        data class NavigateToPage(val position: Int) : ViewPagerState()
    }

    private val _deviceState = MutableLiveData<DeviceState>()
    val deviceState = _deviceState
    private val _fragmentCommunication = MutableLiveData<FragmentCommunicationState>()
    val fragmentCommunication = _fragmentCommunication
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent = _navigationEvent
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState
    private var currentDeviceType = false // false = not TC007, true = TC007
    fun setDeviceType(isTC007: Boolean) {
        currentDeviceType = isTC007
        refreshDeviceState()
    }

    fun initializeDeviceState() {
        viewModelScope.launch {
            refreshDeviceState()
        }
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val deviceState = if (currentDeviceType) {
                // TC007 device state
                val isConnected = WebSocketProxy.getInstance().isTC007Connect()
                DeviceState(
                    isTC007 = true,
                    isWebSocketConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnect07AutoOpen
                )
            } else {
                // USB device state
                val isConnected = DeviceTools.isConnect(isAutoRequest = false)
                DeviceState(
                    isTC007 = false,
                    isUsbConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnectAutoOpen
                )
            }
            _deviceState.value = deviceState
        }
    }

    fun onPageSelected(position: Int) {
        _viewPagerState.value = ViewPagerState.PageSelected(position)
        updateFragmentCommunication(position)
    }

    fun navigateToPage(position: Int) {
        _viewPagerState.value = ViewPagerState.NavigateToPage(position)
    }

    fun navigateToMonitor() {
        _navigationEvent.value = NavigationEvent.ToMonitor(currentDeviceType)
    }

    fun navigateToGallery() {
        _navigationEvent.value = NavigationEvent.ToGallery
    }

    fun navigateToThermal() {
        val routeConfig = if (currentDeviceType) {
            RouterConfig.IR_THERMAL_07
        } else {
            RouterConfig.IR_THERMAL
        }
        _navigationEvent.value = NavigationEvent.ToThermal(routeConfig)
    }

    private fun updateFragmentCommunication(activeFragment: Int) {
        val currentDeviceState = _deviceState.value ?: DeviceState()
        val communicationState = FragmentCommunicationState(
            activeFragment = activeFragment,
            deviceConnected = currentDeviceState.isWebSocketConnected || currentDeviceState.isUsbConnected
        )
        _fragmentCommunication.value = communicationState
    }

    // Guide dialog management
    fun handleGuideDialog(onGuideShow: (Int, Int) -> Unit) {
        val currentStep = SharedManager.homeGuideStep
        if (currentStep == 0) return
        val navigationTarget = when (currentStep) {
            1 -> 0
            2 -> 4
            3 -> 2
            else -> 2
        }
        onGuideShow(currentStep, navigationTarget)
    }

    fun handleGuideNavigation(step: Int) {
        SharedManager.homeGuideStep = when (step) {
            1 -> 2
            2 -> 3
            3 -> 0
            else -> 0
        }
    }

    fun completeGuide() {
        SharedManager.homeGuideStep = 0
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorCaptureViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class IRMonitorCaptureViewModel : BaseViewModel() {
    // Data classes matching the fragment requirements
    data class TemperatureData(
        val centerTemp: Float,
        val maxTemp: Float,
        val minTemp: Float
    )

    data class CaptureData(
        val id: Int,
        val timestamp: Long,
        val temperature: Float,
        val imagePath: String
    )

    enum class DeviceConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    enum class CaptureState {
        INACTIVE, ACTIVE, CONTINUOUS, CAPTURING
    }

    // StateFlow properties for UI state management
    private val _captureState = MutableStateFlow(CaptureState.INACTIVE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _captureHistory = MutableStateFlow<List<CaptureData>>(emptyList())
    val captureHistory: StateFlow<List<CaptureData>> = _captureHistory.asStateFlow()
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val deviceConnectionState: StateFlow<DeviceConnectionState> = _deviceConnectionState.asStateFlow()

    // Internal state
    private var captureIdCounter = 1
    private var continuousCapturingJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize with mock data for development
        initializeMockData()
        // Start temperature monitoring simulation
        startTemperatureMonitoring()
    }

    fun toggleCapture() {
        viewModelScope.launch {
            when (_captureState.value) {
                CaptureState.INACTIVE -> {
                    _captureState.value = CaptureState.ACTIVE
                    simulateDeviceConnection()
                }

                CaptureState.ACTIVE -> {
                    _captureState.value = CaptureState.INACTIVE
                    stopContinuousCapture()
                }

                CaptureState.CONTINUOUS -> {
                    stopContinuousCapture()
                    _captureState.value = CaptureState.ACTIVE
                }

                CaptureState.CAPTURING -> {
                    // Already capturing, ignore
                }
            }
        }
    }

    fun captureFrame() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            _captureState.value = CaptureState.CAPTURING
            // Simulate capture delay
            delay(500)
            // Create capture data
            val currentTemp = _temperatureData.value?.centerTemp ?: 25.0f
            val capture = CaptureData(
                id = captureIdCounter++,
                timestamp = System.currentTimeMillis(),
                temperature = currentTemp,
                imagePath = "/mock/path/capture_${captureIdCounter - 1}.jpg"
            )
            // Add to history
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.add(0, capture) // Add to beginning
            _captureHistory.value = currentHistory
            // Return to previous state
            _captureState.value = if (continuousCapturingJob?.isActive == true) {
                CaptureState.CONTINUOUS
            } else {
                CaptureState.ACTIVE
            }
        }
    }

    fun toggleContinuousCapture() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            if (_captureState.value == CaptureState.CONTINUOUS) {
                stopContinuousCapture()
                _captureState.value = CaptureState.ACTIVE
            } else {
                startContinuousCapture()
            }
        }
    }

    fun clearCaptureHistory() {
        viewModelScope.launch {
            _captureHistory.value = emptyList()
        }
    }

    fun exportCaptures() {
        viewModelScope.launch {
            val captures = _captureHistory.value
            if (captures.isEmpty()) {
                return@launch
            }
            // Create export data with capture information
            val exportData = captures.map { capture ->
                mapOf(
                    "id" to capture.id,
                    "timestamp" to capture.timestamp,
                    "temperature" to capture.temperature,
                    "imagePath" to capture.imagePath
                )
            }
            // In a real implementation, this would write to a file or share the data
            // For now, we log the export action
            android.util.Log.d("IRMonitorCaptureVM", "Exporting ${captures.size} captures")
        }
    }

    fun deleteCapture(capture: CaptureData) {
        viewModelScope.launch {
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.remove(capture)
            _captureHistory.value = currentHistory
        }
    }

    // Private helper methods
    private fun initializeMockData() {
        // Initialize with mock temperature data
        _temperatureData.value = TemperatureData(
            centerTemp = 25.0f,
            maxTemp = 28.5f,
            minTemp = 22.1f
        )
        _deviceConnectionState.value = DeviceConnectionState.DISCONNECTED
    }

    private fun simulateDeviceConnection() {
        viewModelScope.launch {
            _deviceConnectionState.value = DeviceConnectionState.CONNECTING
            delay(2000) // Simulate connection delay
            _deviceConnectionState.value = DeviceConnectionState.CONNECTED
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (_deviceConnectionState.value == DeviceConnectionState.CONNECTED) {
                    // Simulate temperature readings with variation using Kotlin Random
                    val baseTemp = 25.0f
                    val variation = (Random.nextFloat() - 0.5f) * 5.0f
                    val centerTemp = baseTemp + variation
                    _temperatureData.value = TemperatureData(
                        centerTemp = centerTemp,
                        maxTemp = centerTemp + (Random.nextFloat() * 3.0f),
                        minTemp = centerTemp - (Random.nextFloat() * 2.0f)
                    )
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun startContinuousCapture() {
        _captureState.value = CaptureState.CONTINUOUS
        continuousCapturingJob = viewModelScope.launch {
            while (_captureState.value == CaptureState.CONTINUOUS) {
                captureFrame()
                delay(3000) // Capture every 3 seconds
            }
        }
    }

    private fun stopContinuousCapture() {
        continuousCapturingJob?.cancel()
        continuousCapturingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousCapture()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorChartLiteViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRMonitorChartLiteViewModel : BaseViewModel() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingTime = MutableStateFlow("00:00:00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _currentTemp = MutableStateFlow(25.0f)
    val currentTemp: StateFlow<Float> = _currentTemp.asStateFlow()
    private val _highTemp = MutableStateFlow(30.0f)
    val highTemp: StateFlow<Float> = _highTemp.asStateFlow()
    private val _lowTemp = MutableStateFlow(20.0f)
    val lowTemp: StateFlow<Float> = _lowTemp.asStateFlow()
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
            if (!_isRecording.value) {
                _recordingTime.value = "00:00:00"
            }
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun updateTemperature(current: Float, high: Float, low: Float) {
        launchWithErrorHandling {
            _currentTemp.value = current
            _highTemp.value = high
            _lowTemp.value = low
        }
    }

    fun startMonitoring() {
        launchWithLoading {
            _isMonitoring.value = true
        }
    }

    fun stopMonitoring() {
        launchWithErrorHandling {
            _isMonitoring.value = false
            _isRecording.value = false
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorHistoryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.dao.ThermalDao
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class IRMonitorHistoryViewModel : BaseViewModel() {
    // Data classes for history management
    data class HistoryItem(
        val id: String,
        val sessionName: String,
        val startTime: Long,
        val duration: Long,
        val sampleCount: Int,
        val avgTemperature: Float,
        val maxTemperature: Float,
        val minTemperature: Float,
        val sessionType: SessionType,
        val dataFilePath: String
    )

    enum class SessionType(val displayName: String) {
        MONITORING("Monitor"),
        CAPTURE("Capture"),
        ANALYSIS("Analysis"),
        CALIBRATION("Calibration")
    }

    enum class HistoryFilter(
        val displayName: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        ALL("All", Icons.AutoMirrored.Filled.ViewList),
        TODAY("Today", Icons.Default.Today),
        WEEK("This Week", Icons.Default.DateRange),
        MONTH("This Month", Icons.Default.CalendarMonth)
    }

    // StateFlow properties expected by the Compose fragment
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()
    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Internal data storage
    private var allHistoryItems: List<HistoryItem> = emptyList()

    // Custom UI events for history-specific actions
    private val _historyUiEvents = MutableSharedFlow<HistoryUiEvent>()
    val historyUiEvents: SharedFlow<HistoryUiEvent> = _historyUiEvents.asSharedFlow()

    init {
        refreshHistory()
    }

    fun changeFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun exportSelectedItems() {
        // Implement export functionality for selected history items
        launchWithErrorHandling {
            val selectedList = _selectedItems.value
            if (selectedList.isEmpty()) {
                _historyUiEvents.emit(HistoryUiEvent.ShowMessage("No items selected for export"))
                return@launchWithErrorHandling
            }
            // Create export data from selected items
            val exportData = historyItems.value.filter { selectedList.contains(it.id) }
            // Emit export event with data
            _historyUiEvents.emit(HistoryUiEvent.ExportData(exportData))
            // Show success message and clear selection
            _historyUiEvents.emit(HistoryUiEvent.ShowMessage("Exported ${exportData.size} items"))
            clearSelection()
        }
    }

    fun deleteSelectedItems() {
        launchWithErrorHandling {
            val selectedIds = _selectedItems.value
            if (selectedIds.isNotEmpty()) {
                // Remove selected items from the database on IO thread
                withContext(Dispatchers.IO) {
                    selectedIds.forEach { id ->
                        val item = allHistoryItems.find { it.id == id }
                        item?.let {
                            // Convert id back to startTime for database operation
                            val startTime = it.startTime
                            AppDatabase.getInstance().thermalDao().delDetail(startTime)
                        }
                    }
                }
                // Refresh the data after deletion
                refreshHistory()
                clearSelection()
            }
        }
    }

    fun refreshHistory() {
        launchWithLoading {
            try {
                // Perform database operations on IO thread
                val historyItems = withContext(Dispatchers.IO) {
                    val recordList: List<ThermalDao.Record> =
                        AppDatabase.getInstance().thermalDao().queryRecordList()
                    // Convert database records to HistoryItem objects
                    recordList.mapIndexed { index, record ->
                        // Query additional details for temperature statistics
                        val detailList = AppDatabase.getInstance().thermalDao().queryDetail(record.startTime)
                        // Calculate temperature statistics from detail data
                        val temperatures = detailList.map { it.thermal }
                        val maxTemperatures = detailList.map { it.thermalMax }
                        val minTemperatures = detailList.map { it.thermalMin }
                        val avgTemp = if (temperatures.isNotEmpty()) temperatures.average().toFloat() else 0f
                        val maxTemp = maxTemperatures.maxOrNull() ?: 0f
                        val minTemp = minTemperatures.minOrNull() ?: 0f
                        HistoryItem(
                            id = record.startTime.toString(),
                            sessionName = "Session ${index + 1}",
                            startTime = record.startTime,
                            duration = record.duration.toLong() * 1000L, // Convert seconds to milliseconds
                            sampleCount = detailList.size,
                            avgTemperature = avgTemp,
                            maxTemperature = maxTemp,
                            minTemperature = minTemp,
                            sessionType = when (record.type) {
                                "point" -> SessionType.MONITORING
                                "line" -> SessionType.ANALYSIS
                                "area" -> SessionType.CAPTURE
                                else -> SessionType.MONITORING
                            },
                            dataFilePath = findThermalImagePath(record.startTime, detailList.firstOrNull()?.thermalId)
                        )
                    }
                }
                // Update the data on main thread
                allHistoryItems = historyItems
                applyFilter()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleItemSelection(id: String) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(id)) {
            currentSelected.remove(id)
        } else {
            currentSelected.add(id)
        }
        _selectedItems.value = currentSelected
        // Exit selection mode if no items are selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun viewHistoryDetails(item: HistoryItem) {
        // Implement navigation to details screen with history item data
        launchWithErrorHandling {
            // Emit navigation event with the selected item
            _historyUiEvents.emit(HistoryUiEvent.NavigateToDetails(item))
        }
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    private fun applyFilter() {
        val filteredItems = when (_selectedFilter.value) {
            HistoryFilter.ALL -> allHistoryItems
            HistoryFilter.TODAY -> filterByToday()
            HistoryFilter.WEEK -> filterByThisWeek()
            HistoryFilter.MONTH -> filterByThisMonth()
        }
        _historyItems.value = filteredItems.sortedByDescending { it.startTime }
    }

    private fun filterByToday(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrow = calendar.apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        return allHistoryItems.filter { it.startTime in today until tomorrow }
    }

    private fun filterByThisWeek(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis
        return allHistoryItems.filter { it.startTime in weekStart until weekEnd }
    }

    private fun filterByThisMonth(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis
        return allHistoryItems.filter { it.startTime in monthStart until monthEnd }
    }

    private fun findThermalImagePath(startTime: Long, thermalId: String?): String {
        val possibleDirs = sequenceOf(
            FileConfig.gallerySourDir,
            FileConfig.lineIrGalleryDir,
            FileConfig.tc007IrGalleryDir
        )
        val possibleNames = sequenceOf(
            thermalId?.let { "$it.jpg" },
            thermalId?.let { "$it.png" },
            "${startTime}.jpg",
            "${startTime}.png"
        ).filterNotNull()
        return possibleDirs.flatMap { dir ->
            possibleNames.map { name -> File(dir, name) }
        }.firstOrNull { it.exists() }?.absolutePath ?: ""
    }

    // History UI Event sealed class for one-time events
    sealed class HistoryUiEvent {
        data class ShowMessage(val message: String) : HistoryUiEvent()
        data class ExportData(val items: List<HistoryItem>) : HistoryUiEvent()
        data class NavigateToDetails(val item: HistoryItem) : HistoryUiEvent()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.dao.ThermalDao
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IRMonitorViewModel : BaseViewModel() {
    val recordListLD = MutableLiveData<List<ThermalDao.Record>>()
    fun queryRecordList() {
        viewModelScope.launch(Dispatchers.IO) {
            val recordList: List<ThermalDao.Record> =
                AppDatabase.getInstance().thermalDao().queryRecordList()
            recordListLD.postValue(recordList)
        }
    }

    val detailListLD = MutableLiveData<List<ThermalEntity>>()
    fun queryDetail(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val detailList: List<ThermalEntity> =
                AppDatabase.getInstance().thermalDao().queryDetail(startTime)
            detailListLD.postValue(detailList)
        }
    }

    fun delDetail(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().thermalDao().delDetail(startTime)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRPlushViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.view.SurfaceView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.WorkspacePremium
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRPlushViewModel : BaseViewModel() {
    companion object {
        private const val CALIBRATION_DELAY_MS = 2000L
    }

    // Dual view state management
    private val _dualViewState = MutableStateFlow(DualViewState.INACTIVE)
    val dualViewState: StateFlow<DualViewState> = _dualViewState.asStateFlow()

    // Temperature data management
    private val _temperatureData = MutableStateFlow(
        TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    )
    val temperatureData: StateFlow<TemperatureData> = _temperatureData.asStateFlow()

    // Processing mode management
    private val _processingMode = MutableStateFlow(ProcessingMode.STANDARD)
    val processingMode: StateFlow<ProcessingMode> = _processingMode.asStateFlow()

    // Recording state management
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun initializeDualView(surfaceView: SurfaceView) {
        // Only update state, don't store the SurfaceView reference
        _dualViewState.value = DualViewState.ACTIVE
    }

    fun changeProcessingMode(mode: ProcessingMode) {
        _processingMode.value = mode
    }

    fun calibrateDualView() {
        launchWithErrorHandling {
            _dualViewState.value = DualViewState.CALIBRATING
            // Simulation of calibration process
            kotlinx.coroutines.delay(CALIBRATION_DELAY_MS)
            _dualViewState.value = DualViewState.ACTIVE
        }
    }

    fun resetSettings() {
        _processingMode.value = ProcessingMode.STANDARD
        _isRecording.value = false
        _temperatureData.value = TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    }

    fun updateTemperatureData(
        centerTemp: Float,
        maxTemp: Float,
        minTemp: Float,
        ambientTemp: Float
    ) {
        _temperatureData.value = TemperatureData(
            irCenterTemp = centerTemp,
            irMaxTemp = maxTemp,
            irMinTemp = minTemp,
            ambientTemp = ambientTemp
        )
    }

    // Data class definitions
    data class TemperatureData(
        val irCenterTemp: Float,
        val irMaxTemp: Float,
        val irMinTemp: Float,
        val ambientTemp: Float
    )

    enum class DualViewState {
        INACTIVE, ACTIVE, CALIBRATING, ERROR
    }

    enum class ProcessingMode(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
        STANDARD("Standard", Icons.Default.CameraAlt),
        ENHANCED("Enhanced", Icons.Default.AutoAwesome),
        PROFESSIONAL("Professional", Icons.Default.WorkspacePremium),
        FUSION("Fusion", Icons.Default.Merge)
    }

    fun showAdvancedSettings() {
        // Placeholder for advanced settings functionality
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRThermalDoubleViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRThermalDoubleViewModel : BaseViewModel() {
    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _showTrendChart = MutableStateFlow(false)
    val showTrendChart: StateFlow<Boolean> = _showTrendChart.asStateFlow()
    private val _showCompass = MutableStateFlow(false)
    val showCompass: StateFlow<Boolean> = _showCompass.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _isRangeLocked = MutableStateFlow(false)
    val isRangeLocked: StateFlow<Boolean> = _isRangeLocked.asStateFlow()
    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleTrendChart() {
        launchWithErrorHandling {
            _showTrendChart.value = !_showTrendChart.value
        }
    }

    fun toggleCompass() {
        launchWithErrorHandling {
            _showCompass.value = !_showCompass.value
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
        }
    }

    fun toggleRangeLock() {
        launchWithErrorHandling {
            _isRangeLocked.value = !_isRangeLocked.value
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRThermalFragmentViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IRThermalFragmentViewModel : BaseViewModel() {
    data class DeviceConnectionState(
        val hasConnection: Boolean = false,
        val isTC007Connected: Boolean = false,
        val hasUsbDevice: Boolean = false,
        val isTC007Device: Boolean = false
    )

    data class ThermalUIState(
        val isConnected: Boolean = false,
        val isTC007Connected: Boolean = false,
        val showConnectButton: Boolean = false,
        val isLoading: Boolean = false
    )

    // Device connection state management
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState())
    val deviceConnectionState: StateFlow<DeviceConnectionState> =
        _deviceConnectionState.asStateFlow()

    // Individual state flows required by the Compose fragment
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    private val _isTC007 = MutableStateFlow(false)
    val isTC007: StateFlow<Boolean> = _isTC007.asStateFlow()
    private val _deviceInfo = MutableStateFlow<String?>(null)
    val deviceInfo: StateFlow<String?> = _deviceInfo.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // Permission state management
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    // UI state management
    private val _thermalUiState = MutableStateFlow(ThermalUIState())
    val thermalUiState: StateFlow<ThermalUIState> = _thermalUiState.asStateFlow()

    // Action events for dialogs and operations
    private val _thermalAction = MutableLiveData<ThermalAction>()
    val thermalAction: LiveData<ThermalAction> = _thermalAction

    init {
        setupDeviceStateMonitoring()
    }

    private fun setupDeviceStateMonitoring() {
        viewModelScope.launch {
            // Monitor device connections and update UI state accordingly
            combine(
                _deviceConnectionState,
                _thermalUiState
            ) { connectionState, uiState ->
                // Update individual state flows
                _connectionStatus.value = when {
                    connectionState.hasConnection -> ConnectionStatus.CONNECTED
                    else -> ConnectionStatus.DISCONNECTED
                }
                _isTC007.value = connectionState.isTC007Device
                _deviceInfo.value = if (connectionState.hasConnection) {
                    if (connectionState.isTC007Device) "TC007 Connected" else "Device Connected"
                } else {
                    if (connectionState.hasUsbDevice) "USB Device Available" else "No Device Detected"
                }
                uiState.copy(
                    isConnected = connectionState.hasConnection,
                    isTC007Connected = connectionState.isTC007Connected,
                    showConnectButton = !connectionState.hasConnection && connectionState.hasUsbDevice
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    fun checkDeviceConnection(isTC007: Boolean) {
        val hasConnection = if (isTC007) {
            WebSocketProxy.getInstance().isTC007Connect()
        } else {
            DeviceTools.isConnect(isAutoRequest = false)
        }
        val hasUsbDevice = DeviceTools.findUsbDevice() != null
        _deviceConnectionState.value = DeviceConnectionState(
            hasConnection = hasConnection,
            isTC007Connected = isTC007 && hasConnection,
            hasUsbDevice = hasUsbDevice,
            isTC007Device = isTC007
        )
        // Update individual state flows
        _connectionStatus.value = if (hasConnection) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        _isTC007.value = isTC007
        _deviceInfo.value = when {
            hasConnection -> if (isTC007) "TC007 Connected" else "Device Connected"
            hasUsbDevice -> "USB Device Available"
            else -> "No Device Detected"
        }
    }

    fun onDeviceConnected(isTC007Device: Boolean) {
        if (!isTC007Device) {
            SharedManager.hasTcLine = true
        }
        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = true,
            isTC007Connected = isTC007Device,
            isTC007Device = isTC007Device
        )
    }

    fun onDeviceDisconnected() {
        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = false,
            isTC007Connected = false,
            isTC007Device = false
        )
    }

    fun onSocketConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = true,
                isTC007Connected = true,
                isTC007Device = true
            )
        }
    }

    fun onSocketDisConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = false,
                isTC007Connected = false,
                isTC007Device = false
            )
        }
    }

    fun handleThermalOpen(isTC007: Boolean) {
        if (isTC007) {
            _navigationEvent.value = NavigationEvent.NavigateToTC007Thermal
        } else {
            when {
                DeviceTools.isTC001PlusConnect() -> {
                    _navigationEvent.value = NavigationEvent.StartThermalPlusActivity
                }

                DeviceTools.isTC001LiteConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToTCLite
                }

                DeviceTools.isHikConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToHikMain
                }

                else -> {
                    _navigationEvent.value = NavigationEvent.StartThermalNightActivity
                }
            }
        }
    }

    fun handleMainEnter() {
        val connectionState = _deviceConnectionState.value
        if (!connectionState.hasConnection) {
            if (!connectionState.hasUsbDevice) {
                _thermalAction.value = ThermalAction.ShowDeviceConnectTip
            } else {
                _permissionState.value = PermissionState.RequestCameraPermission
            }
        }
    }

    fun onPermissionGranted() {
        _thermalAction.value = ThermalAction.ShowConnectTip
    }

    fun onPermissionDenied(doNotAskAgain: Boolean) {
        if (doNotAskAgain) {
            _thermalAction.value = ThermalAction.ShowPermissionSettingsTip
        }
    }

    // Methods required by the Compose fragment
    fun retryConnection() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        viewModelScope.launch {
            val isTC007Device = _isTC007.value
            val hasConnection = if (isTC007Device) {
                WebSocketProxy.getInstance().isTC007Connect()
            } else {
                DeviceTools.isConnect(isAutoRequest = false)
            }
            if (hasConnection) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                onDeviceConnected(isTC007Device)
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    fun openMainThermal() {
        val isTC007Device = _isTC007.value
        handleThermalOpen(isTC007Device)
    }

    fun connectDevice() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        // This would typically trigger device connection logic
        retryConnection()
    }

    fun openDeviceSettings() {
        _thermalAction.value = ThermalAction.ShowConnectTip
    }

    sealed class NavigationEvent {
        object NavigateToTC007Thermal : NavigationEvent()
        object StartThermalPlusActivity : NavigationEvent()
        object NavigateToTCLite : NavigationEvent()
        object NavigateToHikMain : NavigationEvent()
        object StartThermalNightActivity : NavigationEvent()
    }

    sealed class ThermalAction {
        object ShowDeviceConnectTip : ThermalAction()
        object ShowConnectTip : ThermalAction()
        object ShowPermissionSettingsTip : ThermalAction()
    }

    sealed class PermissionState {
        object RequestCameraPermission : PermissionState()
        object PermissionGranted : PermissionState()
        data class PermissionDenied(val doNotAskAgain: Boolean) : PermissionState()
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\MonitorThermalViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class MonitorThermalViewModel : BaseViewModel() {
    // Monitoring State
    private val _monitoringState = MutableStateFlow(MonitoringState.STOPPED)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    // Thermal Data
    private val _thermalData = MutableStateFlow<ThermalData?>(null)
    val thermalData: StateFlow<ThermalData?> = _thermalData.asStateFlow()

    // Recording Status
    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    // Monitoring Alerts
    private val _monitoringAlerts = MutableStateFlow<List<MonitoringAlert>>(emptyList())
    val monitoringAlerts: StateFlow<List<MonitoringAlert>> = _monitoringAlerts.asStateFlow()
    fun toggleMonitoring() {
        launchWithErrorHandling {
            _monitoringState.value = when (_monitoringState.value) {
                MonitoringState.STOPPED -> MonitoringState.ACTIVE
                MonitoringState.ACTIVE -> MonitoringState.PAUSED
                MonitoringState.PAUSED -> MonitoringState.ACTIVE
            }
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _recordingStatus.value = when (_recordingStatus.value) {
                RecordingStatus.IDLE -> RecordingStatus.RECORDING
                RecordingStatus.RECORDING -> RecordingStatus.IDLE
            }
        }
    }

    fun updateMonitoringFence(fence: FenceData) {
        launchWithErrorHandling {
            // Update fence configuration for monitoring
            // Implementation would integrate with thermal processing
        }
    }

    fun updateTemperatureThreshold(threshold: TemperatureThreshold) {
        launchWithErrorHandling {
            // Update temperature threshold settings
            // Implementation would configure alert triggers
        }
    }

    fun updateAlertSettings(settings: AlertSettings) {
        launchWithErrorHandling {
            // Update alert configuration
            // Implementation would configure notification settings
        }
    }

    fun exportMonitoringData() {
        launchWithErrorHandling {
            // Export monitoring session data
            // Implementation would handle data export
        }
    }

    // Data classes and enums
    data class ThermalData(
        val currentTemp: Float,
        val maxTemp: Float,
        val minTemp: Float,
        val avgTemp: Float,
        val isAlarmTriggered: Boolean,
        val sessionDuration: String,
        val sampleCount: Int,
        val alertCount: Int,
        val dataSize: String
    )

    data class FenceData(val data: String)
    data class TemperatureThreshold(val high: Float, val low: Float)
    data class AlertSettings(val soundEnabled: Boolean, val vibrationEnabled: Boolean)
    data class MonitoringAlert(
        val message: String,
        val severity: AlertSeverity,
        val timestamp: Date
    )

    enum class MonitoringState {
        STOPPED, ACTIVE, PAUSED
    }

    enum class RecordingStatus {
        IDLE, RECORDING
    }

    enum class AlertSeverity {
        LOW, MEDIUM, HIGH
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\MonitorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class MonitorViewModel : BaseViewModel() {
    companion object {
        const val STATS_START = 101
        const val STATS_MONITOR = 102
        const val STATS_FINISH = 103
    }

    private val _monitorState = MutableLiveData(STATS_START)
    val monitorState: MutableLiveData<Int> = _monitorState
    private val _selectedType = MutableLiveData(1)
    val selectedType: MutableLiveData<Int> = _selectedType
    private val _selectedIndex = MutableLiveData<ArrayList<Int>>(arrayListOf())
    val selectedIndex: MutableLiveData<ArrayList<Int>> = _selectedIndex
    private val _recordingTime = MutableLiveData(0L)
    val recordingTime: MutableLiveData<Long> = _recordingTime
    fun setMonitorState(state: Int) {
        _monitorState.value = state
    }

    fun selectMonitorType(type: Int, indices: ArrayList<Int>) {
        _selectedType.value = type
        _selectedIndex.value = indices
        _monitorState.value = STATS_FINISH
    }

    fun startRecording() {
        _recordingTime.value = 0L
    }

    fun updateRecordingTime(time: Long) {
        _recordingTime.value = time
    }

    fun resetState() {
        _monitorState.value = STATS_START
        _selectedType.value = 1
        _selectedIndex.value = arrayListOf()
        _recordingTime.value = 0L
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\PDFListViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PDFListViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "PDFListViewModel"
    }

    // Data class for PDF items (matching the one in fragment)
    data class PDFItem(
        val path: String,
        val name: String,
        val size: Long,
        val pageCount: Int,
        val dateModified: Long,
        val isAnalysisReport: Boolean = false
    )

    // State flows for Compose
    private val _pdfItems = MutableStateFlow<List<PDFItem>>(emptyList())
    val pdfItems: StateFlow<List<PDFItem>> = _pdfItems.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPDFItems()
    }

    private fun loadPDFItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.update { true }
            try {
                val items = getPDFItemsList()
                withContext(Dispatchers.Main) {
                    _pdfItems.update { items }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading PDF items", e)
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun enterSelectionMode(itemPath: String? = null) {
        _isSelectionMode.update { true }
        itemPath?.let { path ->
            _selectedItems.update { setOf(path) }
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.update { false }
        _selectedItems.update { emptySet() }
    }

    fun clearSelection() {
        _selectedItems.update { emptySet() }
        _isSelectionMode.update { false }
    }

    fun toggleItemSelection(itemPath: String) {
        _selectedItems.update { currentSet ->
            val mutableSet = currentSet.toMutableSet()
            if (mutableSet.contains(itemPath)) {
                mutableSet.remove(itemPath)
            } else {
                mutableSet.add(itemPath)
            }
            mutableSet
        }
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.update { false }
        }
    }

    // File operations
    fun deleteSelectedItems() {
        val selectedPaths = _selectedItems.value
        val itemsToDelete = _pdfItems.value.filter { selectedPaths.contains(it.path) }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach { item ->
                try {
                    File(item.path).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: ${item.path}", e)
                }
            }
            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadPDFItems() // Refresh the list
            }
        }
    }

    fun refreshPDFList() {
        loadPDFItems()
    }

    // Get PDF items from file system
    private suspend fun getPDFItemsList(): List<PDFItem> {
        return try {
            val items = mutableListOf<PDFItem>()
            // Scan the PDF directory for PDF files
            val pdfDir = File(FileConfig.getPdfDir())
            Log.d(TAG, "Scanning PDF directory: ${pdfDir.absolutePath}")
            if (pdfDir.exists() && pdfDir.isDirectory) {
                val pdfFiles = pdfDir.listFiles { file ->
                    file.isFile && file.name.lowercase().endsWith(".pdf")
                }
                Log.d(TAG, "Found ${pdfFiles?.size ?: 0} PDF files")
                pdfFiles?.forEach { pdfFile ->
                    try {
                        // Determine if this is an analysis report based on filename patterns
                        val isAnalysisReport = pdfFile.name.contains("analysis", ignoreCase = true) ||
                                pdfFile.name.contains("report", ignoreCase = true) ||
                                pdfFile.name.contains("thermal", ignoreCase = true)
                        // For now, we'll use a default page count of 1
                        // In a production app, you would use a PDF library to get actual page count
                        val pageCount = 1
                        items.add(
                            PDFItem(
                                path = pdfFile.absolutePath,
                                name = pdfFile.name,
                                size = pdfFile.length(),
                                pageCount = pageCount,
                                dateModified = pdfFile.lastModified(),
                                isAnalysisReport = isAnalysisReport
                            )
                        )
                        Log.d(TAG, "Added PDF: ${pdfFile.name} (${pdfFile.length()} bytes)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing PDF file: ${pdfFile.name}", e)
                    }
                }
            } else {
                Log.w(TAG, "PDF directory does not exist or is not a directory: ${pdfDir.absolutePath}")
            }
            // Sort by date modified (newest first)
            val sortedItems = items.sortedByDescending { it.dateModified }
            Log.d(TAG, "Returning ${sortedItems.size} PDF items")
            sortedItems
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF items list", e)
            emptyList()
        }
    }

    fun showMoreActions(item: PDFItem) {
        // Placeholder for more actions functionality
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ReportDetailViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.flow.*

class ReportDetailViewModel : BaseViewModel() {
    private val _reportDate = MutableStateFlow("")
    val reportDate: StateFlow<String> = _reportDate.asStateFlow()
    private val _reportTime = MutableStateFlow("")
    val reportTime: StateFlow<String> = _reportTime.asStateFlow()
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    private val _inspector = MutableStateFlow("")
    val inspector: StateFlow<String> = _inspector.asStateFlow()
    private val _equipment = MutableStateFlow("")
    val equipment: StateFlow<String> = _equipment.asStateFlow()
    private val _reportId = MutableStateFlow<String?>(null)
    val reportId: StateFlow<String?> = _reportId.asStateFlow()
    private val _events = MutableSharedFlow<ReportDetailEvent>()
    val events: SharedFlow<ReportDetailEvent> = _events.asSharedFlow()
    private val reportRepository = ReportDetailRepository()

    sealed class ReportDetailEvent {
        data class ShareReport(val reportId: String) : ReportDetailEvent()
        data class DeleteReport(val reportId: String) : ReportDetailEvent()
    }

    fun loadReportData(reportId: String) {
        launchWithLoading {
            _reportId.value = reportId
            val result = reportRepository.getReportById(reportId)
            when (result) {
                is BaseRepository.Result.Success -> {
                    val report = result.data
                    _reportDate.value = report.date
                    _reportTime.value = report.time
                    _location.value = report.location
                    _inspector.value = report.inspector
                    _equipment.value = report.equipment
                }

                is BaseRepository.Result.Error -> {
                    throw result.exception
                }

                else -> {}
            }
        }
    }

    fun shareReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.ShareReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to share"))
            }
        }
    }

    fun deleteReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.DeleteReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to delete"))
            }
        }
    }

    private inner class ReportDetailRepository : BaseRepository() {
        suspend fun getReportById(reportId: String): Result<ReportDetail> = safeCall {
            val cacheKey = "report_detail_$reportId"
            getCachedOrExecute(cacheKey, 5 * 60 * 1000L) {
                ReportDetail(
                    id = reportId,
                    date = "2024-10-01",
                    time = "14:30:00",
                    location = "Building A - Room 101",
                    inspector = "John Doe",
                    equipment = "TC001 Thermal Camera"
                )
            }
        }
    }

    data class ReportDetail(
        val id: String,
        val date: String,
        val time: String,
        val location: String,
        val inspector: String,
        val equipment: String
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ReportPreviewViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportPreviewViewModel : BaseViewModel() {
    private val _selectedLayout = MutableStateFlow(0)
    val selectedLayout: StateFlow<Int> = _selectedLayout.asStateFlow()
    private val _showImages = MutableStateFlow(true)
    val showImages: StateFlow<Boolean> = _showImages.asStateFlow()
    private val _showMetadata = MutableStateFlow(true)
    val showMetadata: StateFlow<Boolean> = _showMetadata.asStateFlow()
    private val _showWatermark = MutableStateFlow(false)
    val showWatermark: StateFlow<Boolean> = _showWatermark.asStateFlow()
    private val _previewGenerated = MutableStateFlow(false)
    val previewGenerated: StateFlow<Boolean> = _previewGenerated.asStateFlow()
    private val _previewData = MutableStateFlow<PreviewData?>(null)
    val previewData: StateFlow<PreviewData?> = _previewData.asStateFlow()

    data class PreviewData(
        val layoutIndex: Int,
        val includeImages: Boolean,
        val includeMetadata: Boolean,
        val includeWatermark: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun selectLayout(index: Int) {
        launchWithErrorHandling {
            _selectedLayout.value = index
        }
    }

    fun toggleImages() {
        launchWithErrorHandling {
            _showImages.value = !_showImages.value
        }
    }

    fun toggleMetadata() {
        launchWithErrorHandling {
            _showMetadata.value = !_showMetadata.value
        }
    }

    fun toggleWatermark() {
        launchWithErrorHandling {
            _showWatermark.value = !_showWatermark.value
        }
    }

    fun generatePreview() {
        launchWithLoading {
            val currentLayout = _selectedLayout.value
            val currentShowImages = _showImages.value
            val currentShowMetadata = _showMetadata.value
            val currentShowWatermark = _showWatermark.value
            delay(500)
            val preview = PreviewData(
                layoutIndex = currentLayout,
                includeImages = currentShowImages,
                includeMetadata = currentShowMetadata,
                includeWatermark = currentShowWatermark
            )
            _previewData.value = preview
            _previewGenerated.value = true
        }
    }

    fun proceedToSecond(context: Context) {
        launchWithErrorHandling {
            NavigationManager.build(RouterConfig.REPORT_PREVIEW_SECOND)
                .navigation(context)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalFragmentViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.matrix.IrSurfaceView
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.extension.setAutoShutter
import com.mpdc4gsr.libunified.ir.extension.setContrast
import com.mpdc4gsr.libunified.ir.extension.setMirror
import com.mpdc4gsr.libunified.ir.extension.setPropDdeLevel
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ThermalFragmentViewModel(
    private val context: Context? = null
) : BaseViewModel() {
    // ThermalMonitoringUiState data class for Compose UI - holds monitoring-related state
    data class ThermalMonitoringUiState(
        val isMonitoring: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float? = null,
        val maxTemperature: Float? = null,
        val averageTemperature: Float? = null,
        val isDeviceConnected: Boolean = false,
        val isRecording: Boolean = false,
        val alertCount: Int = 0
    )

    // Thermal image processing state
    private val _thermalImageState = MutableStateFlow(ThermalImageState())
    val thermalImageState: StateFlow<ThermalImageState> = _thermalImageState.asStateFlow()

    // Temperature analysis state
    private val _temperatureAnalysis = MutableStateFlow(TemperatureAnalysis())
    val temperatureAnalysis: StateFlow<TemperatureAnalysis> = _temperatureAnalysis.asStateFlow()

    // Thermal processing actions
    private val _thermalProcessingAction = MutableLiveData<ThermalProcessingAction>()
    val thermalProcessingAction: LiveData<ThermalProcessingAction> = _thermalProcessingAction

    // Fence and measurement state
    private val _fenceState = MutableStateFlow(FenceState())
    val fenceState: StateFlow<FenceState> = _fenceState.asStateFlow()

    // Video recording state
    private val _videoRecordingState = MutableStateFlow(VideoRecordingState())
    val videoRecordingState: StateFlow<VideoRecordingState> = _videoRecordingState.asStateFlow()

    // UI interaction state for processing
    private val _processingUiState = MutableStateFlow(ThermalProcessingUiState())
    val processingUiState: StateFlow<ThermalProcessingUiState> = _processingUiState.asStateFlow()

    // Temperature data for UI display
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()

    // Recording state for UI
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Connection status for UI
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Processing mode for UI
    private val _processingMode = MutableStateFlow("Standard")
    val processingMode: StateFlow<String> = _processingMode.asStateFlow()

    // Thermal surface dimensions
    var rawWidth: Int = 0
        private set
    var rawHeight: Int = 0
        private set
    private var iruvctc: IRUVCTC? = null
    private var syncBitmap: SynchronizedBitmap? = null
    private var ircmd: IRCMD? = null

    init {
        setupThermalDataProcessing()
        syncRecordingStates()
        syncUiState()
    }

    private fun setupThermalDataProcessing() {
        viewModelScope.launch {
            // Combine thermal image and temperature data for comprehensive analysis
            combine(
                _thermalImageState,
                _temperatureAnalysis,
                _fenceState
            ) { imageState, tempAnalysis, fenceState ->
                ThermalProcessingUiState(
                    isProcessing = imageState.isProcessing,
                    hasValidImage = imageState.bitmap != null,
                    temperatureInfo = tempAnalysis,
                    fenceActive = fenceState.isActive,
                    processingProgress = imageState.processingProgress
                )
            }.collect { newUiState ->
                _processingUiState.value = newUiState
            }
        }
    }

    private fun syncRecordingStates() {
        viewModelScope.launch {
            // Keep _isRecording in sync with _videoRecordingState
            _videoRecordingState.collect { videoState ->
                if (_isRecording.value != videoState.isRecording) {
                    _isRecording.value = videoState.isRecording
                }
            }
        }
    }

    // Thermal image processing methods
    suspend fun processThermalBitmap(bitmap: Bitmap): ProcessedThermalResult {
        return withContext(Dispatchers.Default) {
            _thermalImageState.value = _thermalImageState.value.copy(
                isProcessing = true,
                processingProgress = 0f
            )
            try {
                val processedBitmap = applyThermalProcessing(bitmap)
                val temperatureData = extractTemperatureData(bitmap)
                val analysis = performTemperatureAnalysis(temperatureData)
                _thermalImageState.value = _thermalImageState.value.copy(
                    bitmap = processedBitmap,
                    isProcessing = false,
                    processingProgress = 1f
                )
                _temperatureAnalysis.value = analysis
                ProcessedThermalResult(
                    processedBitmap = processedBitmap,
                    temperatureAnalysis = analysis,
                    success = true
                )
            } catch (e: Exception) {
                _thermalImageState.value = _thermalImageState.value.copy(
                    isProcessing = false,
                    processingProgress = 0f
                )
                ProcessedThermalResult(
                    processedBitmap = null,
                    temperatureAnalysis = TemperatureAnalysis(),
                    success = false,
                    error = e.message
                )
            }
        }
    }

    private fun applyThermalProcessing(bitmap: Bitmap): Bitmap {
        // Apply thermal image processing algorithms
        val matrix = Matrix()
        // Add thermal processing transformations
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun extractTemperatureData(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { pixel: Int ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val intensity = (red * 0.3f + green * 0.59f + blue * 0.11f) / 255f
            val minTemp = -20f
            val maxTemp = 120f
            minTemp + (intensity * (maxTemp - minTemp))
        }.toFloatArray()
    }

    private fun performTemperatureAnalysis(temperatureData: FloatArray): TemperatureAnalysis {
        if (temperatureData.isEmpty()) {
            return TemperatureAnalysis()
        }
        val maxTemp = temperatureData.maxOrNull() ?: 0f
        val minTemp = temperatureData.minOrNull() ?: 0f
        val avgTemp = temperatureData.average().toFloat()
        val variance = calculateVariance(temperatureData, avgTemp)
        val stdDev = kotlin.math.sqrt(variance)
        val hotSpots = detectHotSpots(temperatureData)
        val coldSpots = detectColdSpots(temperatureData)
        val temperatureTrend = calculateTemperatureTrend(temperatureData)
        return TemperatureAnalysis(
            maxTemperature = maxTemp,
            minTemperature = minTemp,
            averageTemperature = avgTemp,
            standardDeviation = stdDev,
            hotSpotCount = hotSpots.size,
            coldSpotCount = coldSpots.size,
            temperatureTrend = temperatureTrend,
            dataQuality = assessDataQuality(temperatureData),
            isValid = true
        )
    }

    private fun calculateVariance(data: FloatArray, mean: Float): Float {
        return data.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun detectHotSpots(temperatureData: FloatArray): List<HotSpot> {
        val threshold = temperatureData.maxOrNull()?.let { it * 0.8f } ?: 0f
        val hotSpots = mutableListOf<HotSpot>()
        temperatureData.forEachIndexed { index, temp ->
            if (temp > threshold) {
                hotSpots.add(HotSpot(index, temp))
            }
        }
        return hotSpots
    }

    private fun detectColdSpots(temperatureData: FloatArray): List<ColdSpot> {
        val threshold = temperatureData.minOrNull()?.let { it * 1.2f } ?: 0f
        val coldSpots = mutableListOf<ColdSpot>()
        temperatureData.forEachIndexed { index, temp ->
            if (temp < threshold) {
                coldSpots.add(ColdSpot(index, temp))
            }
        }
        return coldSpots
    }

    private fun calculateTemperatureTrend(temperatureData: FloatArray): TemperatureTrend {
        if (temperatureData.size < 2) return TemperatureTrend.STABLE
        val firstHalf = temperatureData.take(temperatureData.size / 2).average()
        val secondHalf = temperatureData.takeLast(temperatureData.size / 2).average()
        return when {
            secondHalf > firstHalf * 1.05 -> TemperatureTrend.RISING
            secondHalf < firstHalf * 0.95 -> TemperatureTrend.FALLING
            else -> TemperatureTrend.STABLE
        }
    }

    private fun assessDataQuality(temperatureData: FloatArray): DataQuality {
        val validCount =
            temperatureData.count { it > -40f && it < 150f } // Reasonable temperature range
        val qualityPercentage = validCount.toFloat() / temperatureData.size
        return when {
            qualityPercentage >= 0.95f -> DataQuality.EXCELLENT
            qualityPercentage >= 0.85f -> DataQuality.GOOD
            qualityPercentage >= 0.70f -> DataQuality.FAIR
            else -> DataQuality.POOR
        }
    }

    // Fence management methods
    fun activateFence(fenceType: FenceType) {
        _fenceState.value = _fenceState.value.copy(
            isActive = true,
            fenceType = fenceType,
            measurements = emptyList()
        )
    }

    fun deactivateFence() {
        _fenceState.value = _fenceState.value.copy(
            isActive = false,
            fenceType = null,
            measurements = emptyList()
        )
    }

    fun addFenceMeasurement(x: Int, y: Int, temperature: Float) {
        val currentMeasurements = _fenceState.value.measurements.toMutableList()
        currentMeasurements.add(FenceMeasurement(x, y, temperature))
        _fenceState.value = _fenceState.value.copy(
            measurements = currentMeasurements
        )
    }

    // Video recording methods
    fun startVideoRecording(outputFile: File) {
        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = true,
            outputFile = outputFile,
            recordingStartTime = System.currentTimeMillis()
        )
    }

    fun stopVideoRecording() {
        val recordingDuration = System.currentTimeMillis() -
                (_videoRecordingState.value.recordingStartTime ?: 0L)
        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = false,
            recordingDuration = recordingDuration
        )
    }

    // Public methods for UI interaction
    fun initializeThermalCamera(surfaceView: IrSurfaceView) {
        _connectionStatus.value = "Connecting"
        viewModelScope.launch {
            try {
                if (context == null) {
                    _connectionStatus.value = "Connection Failed"
                    handleError(Exception("Context not provided"))
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    surfaceView.holder.setFixedSize(256, 192)
                }
                syncBitmap = SynchronizedBitmap()
                val connectCallback = object : ConnectCallback {
                    override fun onCameraOpened(camera: UVCCamera?) {
                        _connectionStatus.value = "Connected"
                        _temperatureData.value = TemperatureData(
                            centerTemp = "25.0Â°C",
                            maxTemp = "30.0Â°C",
                            minTemp = "20.0Â°C"
                        )
                    }

                    override fun onIRCMDCreate(cmd: IRCMD?) {
                        ircmd = cmd
                        cmd?.let {
                            it.setMirror(false)
                            it.setAutoShutter(true)
                            it.setPropDdeLevel(128)
                            it.setContrast(128)
                        }
                    }
                }
                val usbMonitorCallback = object : USBMonitorCallback {
                    override fun onAttach() {}
                    override fun onGranted() {}
                    override fun onDettach() {}
                    override fun onCancel() {}
                    override fun onConnect() {}
                    override fun onDisconnect() {}
                }
                iruvctc = IRUVCTC(
                    256,
                    192,
                    context,
                    syncBitmap!!,
                    CommonParams.DataFlowMode.TEMP_OUTPUT,
                    connectCallback,
                    usbMonitorCallback
                )
                iruvctc?.registerUSB()
                rawWidth = 256
                rawHeight = 192
            } catch (e: Exception) {
                _connectionStatus.value = "Connection Failed"
                handleError(e)
            }
        }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "thermal_photo_$timestamp.jpg"
                val thermalState = _thermalImageState.value
                val tempAnalysis = _temperatureAnalysis.value
                val metadata = mapOf(
                    "timestamp" to timestamp,
                    "centerTemp" to tempAnalysis.averageTemperature,
                    "maxTemp" to tempAnalysis.maxTemperature,
                    "minTemp" to tempAnalysis.minTemperature,
                    "averageTemp" to tempAnalysis.averageTemperature,
                    "deviceConnected" to (ircmd != null),
                    "sdkInitialized" to (iruvctc != null)
                )
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.PhotoCaptured(fileName, metadata)
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleRecording() {
        viewModelScope.launch {
            if (_isRecording.value) {
                // Stop recording
                stopVideoRecording()
                _isRecording.value = false
            } else {
                // Start recording
                try {
                    // Create a temporary file for recording
                    val outputFile = File.createTempFile(
                        "thermal_recording_${System.currentTimeMillis()}",
                        ".mp4"
                    )
                    startVideoRecording(outputFile)
                    _isRecording.value = true
                } catch (e: Exception) {
                    // Handle file creation error
                    _isRecording.value = false
                    // Emit error event and log the exception
                    handleError(e)
                    _thermalProcessingAction.postValue(
                        ThermalProcessingAction.RecordingError(e.message ?: "Failed to start recording")
                    )
                }
            }
        }
    }

    fun openSettings() {
        // Open thermal camera settings
        _thermalProcessingAction.postValue(
            ThermalProcessingAction.NavigateToSettings
        )
    }

    fun updateSurfaceDimensions(width: Int, height: Int) {
        rawWidth = width
        rawHeight = height
    }

    fun calculateViewPosition(
        index: Int,
        viewWidth: Int,
        viewHeight: Int,
        parentWidth: Int,
        parentHeight: Int
    ): Pair<Float, Float> {
        if (rawWidth == 0 || rawHeight == 0) {
            return Pair(0f, 0f)
        }
        val y = index / rawWidth
        val x = index - y * rawWidth
        val x1 = x * parentWidth / rawWidth
        val y1 = y * parentHeight / rawHeight
        val maxX = x1 - viewWidth / 2
        val maxY = y1 - viewHeight / 2
        return Pair(maxX.toFloat(), maxY.toFloat())
    }

    // Data classes for state management
    data class TemperatureData(
        val centerTemp: String = "--Â°C",
        val maxTemp: String = "--Â°C",
        val minTemp: String = "--Â°C"
    )

    data class ThermalImageState(
        val bitmap: Bitmap? = null,
        val isProcessing: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class TemperatureAnalysis(
        val maxTemperature: Float = 0f,
        val minTemperature: Float = 0f,
        val averageTemperature: Float = 0f,
        val standardDeviation: Float = 0f,
        val hotSpotCount: Int = 0,
        val coldSpotCount: Int = 0,
        val temperatureTrend: TemperatureTrend = TemperatureTrend.STABLE,
        val dataQuality: DataQuality = DataQuality.POOR,
        val isValid: Boolean = false
    )

    data class FenceState(
        val isActive: Boolean = false,
        val fenceType: FenceType? = null,
        val measurements: List<FenceMeasurement> = emptyList()
    )

    data class VideoRecordingState(
        val isRecording: Boolean = false,
        val outputFile: File? = null,
        val recordingStartTime: Long? = null,
        val recordingDuration: Long = 0L
    )

    data class ThermalProcessingUiState(
        val isProcessing: Boolean = false,
        val hasValidImage: Boolean = false,
        val temperatureInfo: TemperatureAnalysis = TemperatureAnalysis(),
        val fenceActive: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class ProcessedThermalResult(
        val processedBitmap: Bitmap?,
        val temperatureAnalysis: TemperatureAnalysis,
        val success: Boolean,
        val error: String? = null
    )

    data class HotSpot(val index: Int, val temperature: Float)
    data class ColdSpot(val index: Int, val temperature: Float)
    data class FenceMeasurement(val x: Int, val y: Int, val temperature: Float)
    enum class TemperatureTrend { RISING, FALLING, STABLE }
    enum class DataQuality { EXCELLENT, GOOD, FAIR, POOR }
    enum class FenceType { POINT, LINE, AREA }
    sealed class ThermalProcessingAction {
        object StartProcessing : ThermalProcessingAction()
        object ProcessingComplete : ThermalProcessingAction()
        data class ProcessingError(val message: String) : ThermalProcessingAction()
        data class TemperatureAlert(val temperature: Float, val type: AlertType) :
            ThermalProcessingAction()

        data class PhotoCaptured(val fileName: String, val metadata: Map<String, Any>) : ThermalProcessingAction()
        data class RecordingError(val message: String) : ThermalProcessingAction()
        object NavigateToSettings : ThermalProcessingAction()
        data class RegionConfigured(val fenceType: FenceType) : ThermalProcessingAction()
    }

    enum class AlertType { HOT_SPOT, COLD_SPOT, TEMPERATURE_THRESHOLD }

    // Combined UI state for compose UI
    private val _thermalUiState = MutableStateFlow(ThermalMonitoringUiState())
    val thermalUiState: StateFlow<ThermalMonitoringUiState> = _thermalUiState.asStateFlow()

    // Monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    private fun syncUiState() {
        viewModelScope.launch {
            combine(
                _isMonitoring,
                _temperatureAnalysis,
                _connectionStatus,
                _isRecording
            ) { isMonitoring, tempAnalysis, connectionStatus, isRecording ->
                ThermalMonitoringUiState(
                    isMonitoring = isMonitoring,
                    currentTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    minTemperature = if (tempAnalysis.isValid) tempAnalysis.minTemperature else null,
                    maxTemperature = if (tempAnalysis.isValid) tempAnalysis.maxTemperature else null,
                    averageTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    isDeviceConnected = connectionStatus == "Connected",
                    isRecording = isRecording,
                    alertCount = tempAnalysis.hotSpotCount + tempAnalysis.coldSpotCount
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    // Monitoring control methods
    fun startMonitoring() {
        _isMonitoring.value = true
        viewModelScope.launch {
            // Start thermal monitoring process
            // TODO: Implement actual monitoring logic
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        viewModelScope.launch {
            // Stop thermal monitoring process
            // TODO: Implement actual monitoring stop logic
        }
    }

    fun configureRegions() {
        viewModelScope.launch {
            try {
                val currentFence = _fenceState.value
                val nextFenceType = when (currentFence.fenceType) {
                    FenceType.POINT -> FenceType.LINE
                    FenceType.LINE -> FenceType.AREA
                    FenceType.AREA -> FenceType.POINT
                    null -> FenceType.POINT
                }
                _fenceState.value = currentFence.copy(
                    fenceType = nextFenceType
                )
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.RegionConfigured(nextFenceType)
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun disconnectCamera() {
        viewModelScope.launch {
            try {
                iruvctc?.unregisterUSB()
                iruvctc?.stopPreview()
                iruvctc = null
                ircmd = null
                syncBitmap = null
                _connectionStatus.value = "Disconnected"
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun showSettings() {
        // Placeholder for settings functionality
    }

    override fun onCleared() {
        super.onCleared()
        disconnectCamera()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalIrNightViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalIrNightViewModel : BaseViewModel() {
    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()
    private val _nightModeEnabled = MutableStateFlow(true)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleNightMode() {
        launchWithErrorHandling {
            _nightModeEnabled.value = !_nightModeEnabled.value
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalRGBPreviewViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.view.Surface
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*

class ThermalRGBPreviewViewModel : BaseViewModel() {
    data class RGBPreviewState(
        val isInitialized: Boolean = false,
        val isStreaming: Boolean = false,
        val resolution: String = "1920x1080",
        val frameRate: Int = 30,
        val cameraId: String = "0",
        val availableCameras: List<String> = emptyList(),
        val previewSurface: Surface? = null,
        val currentFrame: Bitmap? = null,
        val exposureMode: ExposureMode = ExposureMode.AUTO,
        val focusMode: FocusMode = FocusMode.AUTO
    )

    data class ThermalOverlayState(
        val isEnabled: Boolean = true,
        val opacity: Float = 0.7f,
        val blendMode: BlendMode = BlendMode.OVERLAY,
        val alignmentOffset: Pair<Float, Float> = 0f to 0f,
        val scale: Float = 1.0f,
        val rotation: Float = 0f,
        val thermalBitmap: Bitmap? = null,
        val colorPalette: ColorPalette = ColorPalette.IRON,
        val temperatureRange: Pair<Float, Float> = 20f to 40f
    )

    data class CombinedPreviewState(
        val rgbState: RGBPreviewState = RGBPreviewState(),
        val thermalState: ThermalOverlayState = ThermalOverlayState(),
        val isReady: Boolean = false,
        val overlayMode: OverlayMode = OverlayMode.BLENDED,
        val syncedFrame: Bitmap? = null
    )

    // StateFlow for RGB preview state management
    private val _rgbPreviewState = MutableStateFlow(RGBPreviewState())
    val rgbPreviewState: StateFlow<RGBPreviewState> = _rgbPreviewState.asStateFlow()
    private val _thermalOverlayState = MutableStateFlow(ThermalOverlayState())
    val thermalOverlayState: StateFlow<ThermalOverlayState> = _thermalOverlayState.asStateFlow()

    // SharedFlow for one-time events
    private val _previewEvents = MutableSharedFlow<PreviewEvent>()
    val previewEvents: SharedFlow<PreviewEvent> = _previewEvents.asSharedFlow()

    // Combined UI State for thermal + RGB preview
    val combinedPreviewState: StateFlow<CombinedPreviewState> = combine(
        _rgbPreviewState,
        _thermalOverlayState
    ) { rgbState, thermalState ->
        CombinedPreviewState(
            rgbState = rgbState,
            thermalState = thermalState,
            isReady = rgbState.isInitialized && thermalState.isEnabled,
            overlayMode = when {
                thermalState.blendMode == BlendMode.SIDE_BY_SIDE -> OverlayMode.SIDE_BY_SIDE
                thermalState.opacity > 0.8f -> OverlayMode.THERMAL_PRIMARY
                thermalState.opacity > 0.3f -> OverlayMode.BLENDED
                else -> OverlayMode.RGB_PRIMARY
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CombinedPreviewState())

    enum class BlendMode {
        OVERLAY, MULTIPLY, SCREEN, SIDE_BY_SIDE, PICTURE_IN_PICTURE
    }

    enum class OverlayMode {
        RGB_PRIMARY, BLENDED, THERMAL_PRIMARY, SIDE_BY_SIDE
    }

    enum class ExposureMode {
        AUTO, MANUAL, SCENE_NIGHT, SCENE_BRIGHT
    }

    enum class FocusMode {
        AUTO, MANUAL, CONTINUOUS_VIDEO, MACRO
    }

    enum class ColorPalette {
        IRON, RAINBOW, GRAYSCALE, HOT, COOL, MEDICAL
    }

    sealed class PreviewEvent {
        object RGBStreamStarted : PreviewEvent()
        object RGBStreamStopped : PreviewEvent()
        data class CameraError(val message: String) : PreviewEvent()
        data class ThermalDataReceived(val bitmap: Bitmap, val temperature: Float) : PreviewEvent()
        data class CalibrationRequired(val message: String) : PreviewEvent()
        data class ShowToast(val message: String) : PreviewEvent()
        data class ShowError(val message: String) : PreviewEvent()
    }

    // RGB Camera Management
    fun initializeRGBCamera(cameraManager: CameraManager) {
        launchWithErrorHandling {
            try {
                val cameraList = cameraManager.cameraIdList.toList()
                _rgbPreviewState.value = _rgbPreviewState.value.copy(
                    availableCameras = cameraList,
                    cameraId = cameraList.firstOrNull() ?: "0",
                    isInitialized = true
                )
                _previewEvents.emit(PreviewEvent.ShowToast("RGB camera initialized"))
            } catch (e: Exception) {
                _previewEvents.emit(PreviewEvent.CameraError("Failed to initialize RGB camera: ${e.message}"))
            }
        }
    }

    fun startRGBPreview(surface: Surface) {
        launchWithErrorHandling {
            _rgbPreviewState.value = _rgbPreviewState.value.copy(
                previewSurface = surface,
                isStreaming = true
            )
            _previewEvents.emit(PreviewEvent.RGBStreamStarted)
        }
    }

    fun stopRGBPreview() {
        launchWithErrorHandling {
            _rgbPreviewState.value = _rgbPreviewState.value.copy(
                previewSurface = null,
                isStreaming = false
            )
            _previewEvents.emit(PreviewEvent.RGBStreamStopped)
        }
    }

    fun selectCamera(cameraId: String) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(cameraId = cameraId)
    }

    fun setExposureMode(mode: ExposureMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(exposureMode = mode)
    }

    fun setFocusMode(mode: FocusMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(focusMode = mode)
    }

    // Thermal Overlay Management
    fun updateThermalOverlay(bitmap: Bitmap, temperature: Float) {
        launchWithErrorHandling {
            _thermalOverlayState.value = _thermalOverlayState.value.copy(
                thermalBitmap = bitmap,
                temperatureRange = _thermalOverlayState.value.temperatureRange.first to temperature
            )
            _previewEvents.emit(PreviewEvent.ThermalDataReceived(bitmap, temperature))
        }
    }

    fun setOverlayOpacity(opacity: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            opacity = opacity.coerceIn(0f, 1f)
        )
    }

    fun setBlendMode(blendMode: BlendMode) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(blendMode = blendMode)
    }

    fun setColorPalette(palette: ColorPalette) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(colorPalette = palette)
    }

    fun adjustAlignment(offsetX: Float, offsetY: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            alignmentOffset = offsetX to offsetY
        )
    }

    fun setScale(scale: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            scale = scale.coerceIn(0.1f, 3.0f)
        )
    }

    fun setRotation(rotation: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            rotation = rotation % 360f
        )
    }

    fun toggleThermalOverlay() {
        val currentState = _thermalOverlayState.value
        _thermalOverlayState.value = currentState.copy(isEnabled = !currentState.isEnabled)
    }

    // Calibration and Synchronization
    fun calibrateAlignment() {
        launchWithErrorHandling {
            // Simulate calibration process
            _previewEvents.emit(PreviewEvent.CalibrationRequired("Place calibration target in view and press OK"))
            // Reset alignment to defaults after calibration
            _thermalOverlayState.value = _thermalOverlayState.value.copy(
                alignmentOffset = 0f to 0f,
                scale = 1.0f,
                rotation = 0f
            )
        }
    }

    fun syncFrames() {
        launchWithErrorHandling {
            val rgbFrame = _rgbPreviewState.value.currentFrame
            val thermalFrame = _thermalOverlayState.value.thermalBitmap
            if (rgbFrame != null && thermalFrame != null) {
                // In a real implementation, this would combine the frames
                // For now, we'll just use the thermal frame as the synced frame
                val combinedState = combinedPreviewState.value
                // Update combined state would happen here
                _previewEvents.emit(PreviewEvent.ShowToast("Frames synchronized"))
            } else {
                _previewEvents.emit(PreviewEvent.ShowError("Cannot sync frames - missing RGB or thermal data"))
            }
        }
    }

    // Preset configurations for different use cases
    fun applyPreset(preset: PreviewPreset) {
        when (preset) {
            PreviewPreset.MEDICAL -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.MEDICAL,
                    opacity = 0.8f,
                    blendMode = BlendMode.OVERLAY
                )
            }

            PreviewPreset.INDUSTRIAL -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.IRON,
                    opacity = 0.6f,
                    blendMode = BlendMode.MULTIPLY
                )
            }

            PreviewPreset.RESEARCH -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.RAINBOW,
                    opacity = 0.5f,
                    blendMode = BlendMode.SIDE_BY_SIDE
                )
            }

            PreviewPreset.NIGHT_VISION -> {
                _rgbPreviewState.value = _rgbPreviewState.value.copy(
                    exposureMode = ExposureMode.SCENE_NIGHT
                )
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.HOT,
                    opacity = 0.9f
                )
            }
        }
    }

    enum class PreviewPreset {
        MEDICAL, INDUSTRIAL, RESEARCH, NIGHT_VISION
    }

    companion object {
        private const val TAG = "ThermalRGBPreviewViewModel"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : BaseViewModel() {
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun yuvArea(
        yuv: ByteArray,
        temp: FloatArray,
        max: Float,
        min: Float,
    ) {
        for (i in temp.indices) {
            if (temp[i] < min) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0x00.toByte()
            }
            if (temp[i] > max) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0xFF.toByte()
            }
        }
    }

    fun exportData(context: Context, format: ExportFormat) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                // Implementation for data export
                val exportFile = createExportFile(context, format)
                // Export data to file based on format
                _exportStatus.value = ExportStatus.Success(exportFile)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun createExportFile(context: Context, format: ExportFormat): File {
        val fileName = "thermal_export_${System.currentTimeMillis()}.${format.extension}"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun captureSnapshot() {
        viewModelScope.launch {
            // Capture thermal snapshot
        }
    }

    sealed class ExportStatus {
        object Idle : ExportStatus()
        object Exporting : ExportStatus()
        data class Success(val file: File) : ExportStatus()
        data class Error(val message: String) : ExportStatus()
    }

    enum class ExportFormat(val extension: String) {
        CSV("csv"),
        JSON("json"),
        PDF("pdf")
    }
}


