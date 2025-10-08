package mpdc4gsr.presentation.screens.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _storageSettings = MutableStateFlow(StorageSettings())
    val storageSettings: StateFlow<StorageSettings> = _storageSettings.asStateFlow()
    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    data class StorageSettings(
        val autoExport: Boolean = false,
        val exportFormat: String = "CSV",
        val storageLocation: String = "Internal Storage",
        val autoBackup: Boolean = false,
        val compressionEnabled: Boolean = true,
        val deleteAfterExport: Boolean = false
    )

    data class StorageInfo(
        val availableSpace: String = "Calculating...",
        val usedSpace: String = "Calculating...",
        val totalSpace: String = "Calculating..."
    )

    companion object {
        private const val KEY_AUTO_EXPORT = "storage_auto_export"
        private const val KEY_EXPORT_FORMAT = "storage_export_format"
        private const val KEY_STORAGE_LOCATION = "storage_location"
        private const val KEY_AUTO_BACKUP = "storage_auto_backup"
        private const val KEY_COMPRESSION = "storage_compression"
        private const val KEY_DELETE_AFTER_EXPORT = "storage_delete_after_export"
    }

    init {
        loadSettings()
        updateStorageInfo()
    }

    private fun loadSettings() {
        _storageSettings.value = StorageSettings(
            autoExport = prefs.getBoolean(KEY_AUTO_EXPORT, false),
            exportFormat = prefs.getString(KEY_EXPORT_FORMAT, "CSV") ?: "CSV",
            storageLocation = prefs.getString(KEY_STORAGE_LOCATION, "Internal Storage") ?: "Internal Storage",
            autoBackup = prefs.getBoolean(KEY_AUTO_BACKUP, false),
            compressionEnabled = prefs.getBoolean(KEY_COMPRESSION, true),
            deleteAfterExport = prefs.getBoolean(KEY_DELETE_AFTER_EXPORT, false)
        )
    }

    private fun updateStorageInfo() {
        viewModelScope.launch {
            val currentLocation = _storageSettings.value.storageLocation
            val path = when (currentLocation) {
                "SD Card" -> {
                    val externalStorage = Environment.getExternalStorageDirectory()
                    if (externalStorage != null && externalStorage.exists()) {
                        externalStorage
                    } else {
                        Environment.getDataDirectory()
                    }
                }

                "External USB" -> {
                    // For External USB, would need to scan removable storage
                    // Falling back to internal for now
                    Environment.getDataDirectory()
                }

                else -> Environment.getDataDirectory()
            }
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            val usedBlocks = totalBlocks - availableBlocks
            val available = (availableBlocks * blockSize) / (1024.0 * 1024 * 1024)
            val used = (usedBlocks * blockSize) / (1024.0 * 1024 * 1024)
            val total = (totalBlocks * blockSize) / (1024.0 * 1024 * 1024)
            _storageInfo.value = StorageInfo(
                availableSpace = "%.1f GB".format(available),
                usedSpace = "%.1f GB".format(used),
                totalSpace = "%.1f GB".format(total)
            )
        }
    }

    fun updateAutoExport(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_EXPORT, enabled).apply()
            _storageSettings.value = _storageSettings.value.copy(autoExport = enabled)
        }
    }

    fun updateExportFormat(format: String) {
        viewModelScope.launch {
            prefs.edit().putString(KEY_EXPORT_FORMAT, format).apply()
            _storageSettings.value = _storageSettings.value.copy(exportFormat = format)
        }
    }

    fun updateStorageLocation(location: String) {
        viewModelScope.launch {
            prefs.edit().putString(KEY_STORAGE_LOCATION, location).apply()
            _storageSettings.value = _storageSettings.value.copy(storageLocation = location)
            updateStorageInfo()
        }
    }

    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
            _storageSettings.value = _storageSettings.value.copy(autoBackup = enabled)
        }
    }

    fun updateCompression(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_COMPRESSION, enabled).apply()
            _storageSettings.value = _storageSettings.value.copy(compressionEnabled = enabled)
        }
    }

    fun updateDeleteAfterExport(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_DELETE_AFTER_EXPORT, enabled).apply()
            _storageSettings.value = _storageSettings.value.copy(deleteAfterExport = enabled)
        }
    }
}
