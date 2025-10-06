package mpdc4gsr.feature.gsr.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import java.io.File

class GSRRawImageViewViewModel(
    context: Context
) : AppBaseViewModel() {
    private val application: Context = context.applicationContext

    data class GSRImageViewState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val imageFiles: List<File> = emptyList(),
        val selectedImage: File? = null
    )

    private val _imageViewState = MutableStateFlow(GSRImageViewState())
    val imageViewState: StateFlow<GSRImageViewState> = _imageViewState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _imageViewState.value = _imageViewState.value.copy(isLoading = true, error = null)
            try {
                val imageFiles = getGSRImageFiles()
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    imageFiles = imageFiles
                )
            } catch (e: Exception) {
                _imageViewState.value = _imageViewState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load images"
                )
            }
        }
    }

    fun openImage(imageFile: File) {
        viewModelScope.launch {
            try {
                val context = application.applicationContext
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        uri, "image
                        fun shareImage(imageFile: File) {
                            viewModelScope.launch {
                                try {
                                    val context = application.applicationContext
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image
                                        fun deleteImage(imageFile: File) {
                                            viewModelScope.launch {
                                                try {
                                                    if (imageFile.delete()) {
                                                        // Reload images after deletion
                                                        loadImages()
                                                    } else {
                                                        _imageViewState.value = _imageViewState.value.copy(
                                                            error = "Failed to delete image"
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    _imageViewState.value = _imageViewState.value.copy(
                                                        error = "Error deleting image: ${e.message}"
                                                    )
                                                }
                                            }
                                        }

                                        private fun getGSRImageFiles(): List<File> {
                                            val imageFiles = mutableListOf<File>()
                                            // Check multiple possible directories
                                            val possibleDirectories = listOf(
                                                // External storage directories
                                                File(Environment.getExternalStorageDirectory(), "GSR/Images"),
                                                File(Environment.getExternalStorageDirectory(), "IRCamera/GSR"),
                                                File(Environment.getExternalStorageDirectory(), "DCIM/GSR"),
                                                // App-specific directories
                                                File(application.externalCacheDir, "gsr_images"),
                                                File(
                                                    application.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                                    "GSR"
                                                ),
                                                File(application.filesDir, "gsr_images")
                                            )
                                            for (directory in possibleDirectories) {
                                                if (directory.exists() && directory.isDirectory) {
                                                    directory.listFiles { file ->
                                                        file.isFile && isImageFile(file.name)
                                                    }?.let { files ->
                                                        imageFiles.addAll(files)
                                                    }
                                                }
                                            }
                                            // Sort by last modified (newest first)
                                            return imageFiles.sortedByDescending { it.lastModified() }
                                        }

                                        private fun isImageFile(fileName: String): Boolean {
                                            val imageExtensions =
                                                listOf(".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp")
                                            return imageExtensions.any { fileName.lowercase().endsWith(it) }
                                        }

                                        fun getImageMetadata(imageFile: File): Map<String, String> {
                                            val metadata = mutableMapOf<String, String>()
                                            try {
                                                metadata["Name"] = imageFile.name
                                                metadata["Size"] = formatFileSize(imageFile.length())
                                                metadata["Modified"] = formatDate(imageFile.lastModified())
                                                metadata["Path"] = imageFile.absolutePath
                                                // Try to get image dimensions
                                                val options = android.graphics.BitmapFactory.Options().apply {
                                                    inJustDecodeBounds = true
                                                }
                                                android.graphics.BitmapFactory.decodeFile(
                                                    imageFile.absolutePath,
                                                    options
                                                )
                                                if (options.outWidth > 0 && options.outHeight > 0) {
                                                    metadata["Dimensions"] =
                                                        "${options.outWidth} x ${options.outHeight}"
                                                    metadata["Type"] = options.outMimeType ?: "Unknown"
                                                }
                                            } catch (e: Exception) {
                                                metadata["Error"] = "Failed to read metadata: ${e.message}"
                                            }
                                            return metadata
                                        }

                                        private fun formatFileSize(bytes: Long): String {
                                            return when {
                                                bytes < 1024 -> "$bytes B"
                                                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                                                bytes < 1024 * 1024 * 1024 -> String.format(
                                                    "%.1f MB",
                                                    bytes / (1024.0 * 1024.0)
                                                )

                                                else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                                            }
                                        }

                                        private fun formatDate(timestamp: Long): String {
                                            return java.text.SimpleDateFormat(
                                                "MMM dd, yyyy HH:mm",
                                                java.util.Locale.getDefault()
                                            )
                                                .format(java.util.Date(timestamp))
                                        }
                                    }