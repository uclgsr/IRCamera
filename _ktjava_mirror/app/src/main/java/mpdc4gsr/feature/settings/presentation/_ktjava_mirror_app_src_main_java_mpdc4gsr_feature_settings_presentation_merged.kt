// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\feature\settings\presentation' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\settings\presentation\app_src_main_java_mpdc4gsr_feature_settings_presentation_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\settings\presentation' subtree
// Files: 7; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\NetworkSettingsViewModel.kt =====

package mpdc4gsr.feature.settings.presentation

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.ShimmerDeviceManager

class NetworkSettingsViewModel(context: Context) : BaseViewModel() {
    private val context: Context = context.applicationContext
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var wifiManager: WifiManager? = null
    private var shimmerDeviceManager: ShimmerDeviceManager? = null
    private val _networkSettings = MutableStateFlow(NetworkSettings())
    val networkSettings: StateFlow<NetworkSettings> = _networkSettings.asStateFlow()
    private val _pairedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<DeviceInfo>> = _pairedDevices.asStateFlow()
    private val _networkInfo = MutableStateFlow(NetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    data class NetworkSettings(
        val wifiEnabled: Boolean = true,
        val bluetoothEnabled: Boolean = true,
        val autoConnect: Boolean = true
    )

    data class DeviceInfo(
        val name: String,
        val address: String,
        val type: DeviceType,
        val isConnected: Boolean
    )

    enum class DeviceType {
        SHIMMER_GSR, TOPDON_THERMAL, UNKNOWN
    }

    data class NetworkInfo(
        val wifiNetwork: String = "Not Connected",
        val ipAddress: String = "N/A",
        val bluetoothStatus: String = "Unknown"
    )

    companion object {
        private const val KEY_WIFI_ENABLED = "network_wifi_enabled"
        private const val KEY_BLUETOOTH_ENABLED = "network_bluetooth_enabled"
        private const val KEY_AUTO_CONNECT = "network_auto_connect"
    }

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        loadSettings()
        updateNetworkInfo()
        loadPairedDevices()
    }

    fun initialize() {
        // Kept for compatibility, but initialization now happens in init block
        loadSettings()
        updateNetworkInfo()
        loadPairedDevices()
    }

    private fun loadSettings() {
        _networkSettings.value = NetworkSettings(
            wifiEnabled = wifiManager?.isWifiEnabled ?: true,
            bluetoothEnabled = bluetoothAdapter?.isEnabled ?: true,
            autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        )
    }

    @Suppress("DEPRECATION")
    private fun updateNetworkInfo() {
        viewModelScope.launch {
            val wifiInfo = wifiManager?.connectionInfo
            val ipAddress = wifiInfo?.ipAddress?.let {
                String.format(
                    "%d.%d.%d.%d",
                    (it and 0xff),
                    (it shr 8 and 0xff),
                    (it shr 16 and 0xff),
                    (it shr 24 and 0xff)
                )
            } ?: "N/A"
            _networkInfo.value = NetworkInfo(
                wifiNetwork = wifiInfo?.ssid?.replace("\"", "") ?: "Not Connected",
                ipAddress = ipAddress,
                bluetoothStatus = if (bluetoothAdapter?.isEnabled == true) "Enabled" else "Disabled"
            )
        }
    }

    private fun loadPairedDevices() {
        viewModelScope.launch {
            val devices = mutableListOf<DeviceInfo>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter?.bondedDevices?.forEach { device ->
                    // Identify device type by checking for known device name patterns
                    // Note: This is a heuristic approach. For more robust detection,
                    // consider scanning for specific Bluetooth service UUIDs if available
                    val deviceType = when {
                        device.name?.contains("Shimmer", ignoreCase = true) == true -> DeviceType.SHIMMER_GSR
                        device.name?.contains("Topdon", ignoreCase = true) == true ||
                                device.name?.contains("TC001", ignoreCase = true) == true -> DeviceType.TOPDON_THERMAL

                        else -> DeviceType.UNKNOWN
                    }
                    devices.add(
                        DeviceInfo(
                            name = device.name ?: "Unknown Device",
                            address = device.address,
                            type = deviceType,
                            isConnected = false
                        )
                    )
                }
            }
            _pairedDevices.value = devices
        }
    }

    fun updateAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
            _networkSettings.value = _networkSettings.value.copy(autoConnect = enabled)
        }
    }

    fun scanForDevices() {
        viewModelScope.launch {
            loadPairedDevices()
            updateNetworkInfo()
        }
    }

    fun refreshWifiInfo() {
        updateNetworkInfo()
    }

    fun refreshBluetoothInfo() {
        updateNetworkInfo()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\NetworkSettingsViewModelFactory.kt =====

package mpdc4gsr.feature.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NetworkSettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkSettingsViewModel::class.java)) {
            return NetworkSettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\ProfileEditViewModel.kt =====

package mpdc4gsr.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

data class ProfileData(
    val userName: String = "Research Participant",
    val userId: String = "RP-2025-001",
    val email: String = "participant@research.edu",
    val institution: String = "University Research Lab",
    val researchArea: String = "Physiological Computing",
    val bio: String = "Conducting multi-modal sensor research",
    val profilePhotoUrl: String? = null,
    val isProfileVisible: Boolean = true,
    val allowDataSharing: Boolean = false
)

class ProfileEditViewModel : AppBaseViewModel() {
    private val _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    fun updateUserName(value: String) {
        _profileData.value = _profileData.value.copy(userName = value)
    }

    fun updateUserId(value: String) {
        _profileData.value = _profileData.value.copy(userId = value)
    }

    fun updateEmail(value: String) {
        _profileData.value = _profileData.value.copy(email = value)
    }

    fun updateInstitution(value: String) {
        _profileData.value = _profileData.value.copy(institution = value)
    }

    fun updateResearchArea(value: String) {
        _profileData.value = _profileData.value.copy(researchArea = value)
    }

    fun updateBio(value: String) {
        _profileData.value = _profileData.value.copy(bio = value)
    }

    fun updateProfileVisibility(visible: Boolean) {
        _profileData.value = _profileData.value.copy(isProfileVisible = visible)
    }

    fun updateDataSharing(enabled: Boolean) {
        _profileData.value = _profileData.value.copy(allowDataSharing = enabled)
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // TODO: Save to repository or SharedPreferences
                // For now, just simulate save delay
                kotlinx.coroutines.delay(500)
                onSuccess()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\RecordingSettingsViewModel.kt =====

package mpdc4gsr.feature.settings.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository

class RecordingSettingsViewModel : AppBaseViewModel() {
    private lateinit var repository: RecordingSettingsRepository
    private val _recordingSettings = MutableStateFlow(RecordingSettings())
    val recordingSettings: StateFlow<RecordingSettings> = _recordingSettings.asStateFlow()

    data class RecordingSettings(
        val autoRecording: Boolean = false,
        val recordingQuality: String = "High",
        val videoFrameRate: Int = 30,
        val audioEnabled: Boolean = true,
        val simultaneousRecording: Boolean = true,
        val timestampSync: Boolean = true,
        val videoFormat: String = "MP4 (H.264)",
        val audioFormat: String = "AAC",
        val sensorDataFormat: String = "CSV"
    )

    fun initialize(context: Context) {
        repository = RecordingSettingsRepository.getInstance(context)
        loadSettings()
        viewModelScope.launch {
            repository.settings.collect { repoSettings ->
                _recordingSettings.value = RecordingSettings(
                    autoRecording = repoSettings.autoRecording,
                    recordingQuality = repoSettings.recordingQuality,
                    videoFrameRate = repoSettings.videoFrameRate,
                    audioEnabled = repoSettings.audioEnabled,
                    simultaneousRecording = repoSettings.simultaneousRecording,
                    timestampSync = repoSettings.timestampSync,
                    videoFormat = repoSettings.videoFormat,
                    audioFormat = repoSettings.audioFormat,
                    sensorDataFormat = repoSettings.sensorDataFormat
                )
            }
        }
    }

    private fun loadSettings() {
        if (::repository.isInitialized) {
            val settings = repository.getSettings()
            _recordingSettings.value = RecordingSettings(
                autoRecording = settings.autoRecording,
                recordingQuality = settings.recordingQuality,
                videoFrameRate = settings.videoFrameRate,
                audioEnabled = settings.audioEnabled,
                simultaneousRecording = settings.simultaneousRecording,
                timestampSync = settings.timestampSync,
                videoFormat = settings.videoFormat,
                audioFormat = settings.audioFormat,
                sensorDataFormat = settings.sensorDataFormat
            )
        }
    }

    fun updateAutoRecording(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoRecording(enabled)
        }
    }

    fun updateRecordingQuality(quality: String) {
        viewModelScope.launch {
            repository.updateRecordingQuality(quality)
        }
    }

    fun updateVideoFrameRate(frameRate: Int) {
        viewModelScope.launch {
            repository.updateVideoFrameRate(frameRate)
        }
    }

    fun updateAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudioEnabled(enabled)
        }
    }

    fun updateSimultaneousRecording(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSimultaneousRecording(enabled)
        }
    }

    fun updateTimestampSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateTimestampSync(enabled)
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\StorageSettingsViewModel.kt =====

package mpdc4gsr.feature.settings.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageSettingsViewModel(context: Context) : BaseViewModel() {
    private val context: Context = context.applicationContext
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

    fun initialize() {
        // Kept for compatibility, but initialization now happens in init block
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
            try {
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
            } catch (e: Exception) {
                _storageInfo.value = StorageInfo(
                    availableSpace = "Error",
                    usedSpace = "Error",
                    totalSpace = "Error"
                )
            }
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


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\StorageSettingsViewModelFactory.kt =====

package mpdc4gsr.feature.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StorageSettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StorageSettingsViewModel::class.java)) {
            return StorageSettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\settings\presentation\SyncSettingsViewModel.kt =====

package mpdc4gsr.feature.settings.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

class SyncSettingsViewModel : AppBaseViewModel() {
    private lateinit var prefs: SharedPreferences
    private val _syncSettings = MutableStateFlow(SyncSettings())
    val syncSettings: StateFlow<SyncSettings> = _syncSettings.asStateFlow()

    data class SyncSettings(
        val ntpSync: Boolean = true,
        val syncMethod: String = "NTP",
        val syncInterval: Int = 60,
        val autoAlignment: Boolean = true,
        val timestampCorrection: Boolean = true,
        val lastSync: String = "Never"
    )

    companion object {
        private const val KEY_NTP_SYNC = "sync_ntp_enabled"
        private const val KEY_SYNC_METHOD = "sync_method"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_AUTO_ALIGNMENT = "sync_auto_alignment"
        private const val KEY_TIMESTAMP_CORRECTION = "sync_timestamp_correction"
        private const val KEY_LAST_SYNC = "sync_last_sync"
    }

    fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        loadSettings()
    }

    private fun loadSettings() {
        _syncSettings.value = SyncSettings(
            ntpSync = prefs.getBoolean(KEY_NTP_SYNC, true),
            syncMethod = prefs.getString(KEY_SYNC_METHOD, "NTP") ?: "NTP",
            syncInterval = prefs.getInt(KEY_SYNC_INTERVAL, 60),
            autoAlignment = prefs.getBoolean(KEY_AUTO_ALIGNMENT, true),
            timestampCorrection = prefs.getBoolean(KEY_TIMESTAMP_CORRECTION, true),
            lastSync = prefs.getString(KEY_LAST_SYNC, "Never") ?: "Never"
        )
    }

    fun updateNtpSync(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_NTP_SYNC, enabled).apply()
            _syncSettings.value = _syncSettings.value.copy(ntpSync = enabled)
        }
    }

    fun updateSyncMethod(method: String) {
        viewModelScope.launch {
            prefs.edit().putString(KEY_SYNC_METHOD, method).apply()
            _syncSettings.value = _syncSettings.value.copy(syncMethod = method)
        }
    }

    fun updateSyncInterval(interval: Int) {
        viewModelScope.launch {
            prefs.edit().putInt(KEY_SYNC_INTERVAL, interval).apply()
            _syncSettings.value = _syncSettings.value.copy(syncInterval = interval)
        }
    }

    fun updateAutoAlignment(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_ALIGNMENT, enabled).apply()
            _syncSettings.value = _syncSettings.value.copy(autoAlignment = enabled)
        }
    }

    fun updateTimestampCorrection(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_TIMESTAMP_CORRECTION, enabled).apply()
            _syncSettings.value = _syncSettings.value.copy(timestampCorrection = enabled)
        }
    }
}