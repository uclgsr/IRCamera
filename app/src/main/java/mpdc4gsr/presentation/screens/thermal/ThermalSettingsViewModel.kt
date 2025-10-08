package mpdc4gsr.presentation.screens.thermal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.feature.thermal.data.ThermalSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ThermalSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val repository: ThermalSettingsRepository = ThermalSettingsRepository.getInstance(context)
    private val _thermalSettings = MutableStateFlow(ThermalSettingsRepository.ThermalSettings())
    val thermalSettings: StateFlow<ThermalSettingsRepository.ThermalSettings> = _thermalSettings.asStateFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            repository.thermalSettings.collect { repoSettings ->
                _thermalSettings.value = repoSettings
            }
        }
    }

    private fun loadSettings() {
        _thermalSettings.value = repository.getSettings()
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
