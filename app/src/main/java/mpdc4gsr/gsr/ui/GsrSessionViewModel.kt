package mpdc4gsr.gsr.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mpdc4gsr.gsr.model.RecorderKind

@HiltViewModel
class GsrSessionViewModel @Inject constructor() : ViewModel() {

    private val _sessionConfiguration = MutableStateFlow(SessionConfiguration())
    val sessionConfiguration: StateFlow<SessionConfiguration> = _sessionConfiguration.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    fun setSessionLabel(label: String) {
        _sessionConfiguration.update { it.copy(label = label) }
    }

    fun setSensorEnabled(kind: RecorderKind, enabled: Boolean) {
        _sessionConfiguration.update { config ->
            val updated =
                when (kind) {
                    RecorderKind.GSR -> config.copy(useGsr = enabled)
                    RecorderKind.RGB_VIDEO -> config.copy(useRgb = enabled)
                    RecorderKind.THERMAL_VIDEO -> config.copy(useIr = enabled)
                    RecorderKind.AUDIO -> config
                }
            if (updated.useGsr || updated.useRgb || updated.useIr) updated else updated.copy(useGsr = true)
        }
    }

    fun completeOnboarding() {
        _onboardingCompleted.value = true
    }
}
