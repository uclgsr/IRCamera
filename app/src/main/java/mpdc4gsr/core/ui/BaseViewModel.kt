package mpdc4gsr.core.ui

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel as LibUnifiedBaseViewModel
import kotlinx.coroutines.flow.*

/**
 * Base ViewModel for the app module with connection state support
 * Extends libunified BaseViewModel to maintain consistency
 */
open class BaseViewModel : LibUnifiedBaseViewModel() {

    // Connection state for device/socket handling
    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}
