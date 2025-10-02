package mpdc4gsr.core.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base ViewModel for the app module with connection state support
 * Extends libunified BaseViewModel for compatibility with BaseComposeActivity
 */
open class BaseViewModel : com.mpdc4gsr.libunified.app.ktbase.BaseViewModel() {

    // Connection state for device/socket handling (app-specific addition)
    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}
