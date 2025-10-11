package com.mpdc4gsr.component.thermal.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.component.shared.app.config.FileConfig
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import com.mpdc4gsr.component.shared.app.utils.SingleLiveEvent
import com.mpdc4gsr.component.thermal.compat.ContextProvider
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
        val isVideo: Boolean = false,
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
        }
    }

    fun openMediaItem(item: MediaItem) {
        // Implementation for opening media item
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
                } else {
                    galleryLiveData.postValue(it)
                }
            }
        }
    }

    private fun getMediaItemsList(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        // Load pictures
        val picturePath =
            ContextProvider
                .getContext()
                .getExternalFilesDir("Pictures")!!
                .absolutePath + File.separator + "thermal"
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
                            isVideo = false,
                        ),
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
                            isVideo = true,
                        ),
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
                    ContextProvider
                        .getContext()
                        .getExternalFilesDir("Pictures")!!
                        .absolutePath + File.separator + "thermal"
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



