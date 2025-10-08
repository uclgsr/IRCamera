package mpdc4gsr.feature.device.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Deprecated(
    message = "Use Hilt @HiltViewModel instead. DiagnosticsViewModel now uses constructor injection via Hilt.",
    replaceWith = ReplaceWith("hiltViewModel()", "androidx.hilt.navigation.compose.hiltViewModel"),
    level = DeprecationLevel.ERROR
)
class DiagnosticsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw UnsupportedOperationException(
            "DiagnosticsViewModelFactory is deprecated. Use hiltViewModel() composable function instead."
        )
    }
}
