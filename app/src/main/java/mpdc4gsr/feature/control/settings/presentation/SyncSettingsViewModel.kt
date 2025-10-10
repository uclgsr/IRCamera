package mpdc4gsr.feature.control.settings.presentation

import android.content.Context
import android.content.SharedPreferences
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
class SyncSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
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

    init {
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

