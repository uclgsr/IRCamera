package mpdc4gsr.feature.device.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Deprecated(
    message = "Use Hilt @HiltViewModel instead. DiagnosticsViewModel now uses constructor injection.",
    replaceWith = ReplaceWith("hiltViewModel()", "androidx.hilt.navigation.compose.hiltViewModel"),
    level = DeprecationLevel.WARNING
)
class DiagnosticsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticsViewModel::class.java)) {
            return DiagnosticsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
