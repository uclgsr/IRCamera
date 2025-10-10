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
                    if (hasLoadPage == 0) {
                        ArrayList(pageList.size)
                    } else {
                        sourceListLD.value
                            ?: ArrayList(pageList.size)
                    }
                val showList =
                    if (hasLoadPage == 0) {
                        ArrayList(pageList.size)
                    } else {
                        showListLD.value
                            ?: ArrayList(pageList.size)
                    }
                if (pageList.isNotEmpty()) {
                    hasLoadPage++
                }
                var beforeTime =
                    if (sourceList.isEmpty()) {
                        0
                    } else {
                        TimeTools.timeToMinute(
                            sourceList.last().timeMillis,
                            4,
                        )
                    }
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

    private fun calculateFileSize(path: String): Long =
        try {
            val file = File(path)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }

    fun getCachedFileSize(path: String): Long = fileSizeCache[path] ?: 0L

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
