package mpdc4gsr.core.ui

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*

/**
 * App-specific Base ViewModel with connection state support
 * Extends libunified BaseViewModel and adds app-specific connection state management
 */
open class AppBaseViewModel : BaseViewModel() {

    // Connection state for device/socket handling
    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}
