package mpdc4gsr.feature.thermal.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.thermal.data.ThermalSettingsRepository

class ThermalSettingsViewModel : AppBaseViewModel() {
    private lateinit var repository: ThermalSettingsRepository
    private val _thermalSettings = MutableStateFlow(ThermalSettingsRepository.ThermalSettings())
    val thermalSettings: StateFlow<ThermalSettingsRepository.ThermalSettings> = _thermalSettings.asStateFlow()
    fun initialize(context: Context) {
        repository = ThermalSettingsRepository.getInstance(context)
        loadSettings()
        viewModelScope.launch {
            repository.thermalSettings.collect { repoSettings ->
                _thermalSettings.value = repoSettings
            }
        }
    }

    private fun loadSettings() {
        if (::repository.isInitialized) {
            _thermalSettings.value = repository.getSettings()
        }
    }

    fun updateFrameRate(frameRate: Int) {
        viewModelScope.launch {
            repository.updateFrameRate(frameRate)
        }
    }

    fun updateSaveRawImages(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSaveRawImages(enabled)
        }
    }

    fun updatePalette(palette: String) {
        viewModelScope.launch {
            repository.updatePalette(palette)
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch {
            repository.updateTemperatureUnit(unit)
        }
    }

    fun updateEmissivity(emissivity: Float) {
        viewModelScope.launch {
            repository.updateEmissivity(emissivity)
        }
    }

    fun updateAutoScale(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoScale(enabled)
        }
    }

    fun updateShowCrosshair(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowCrosshair(enabled)
        }
    }

    fun updateTemperatureRange(range: String) {
        viewModelScope.launch {
            repository.updateTemperatureRange(range)
        }
    }
}
