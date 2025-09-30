package mpdc4gsr.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-level BaseViewModel that extends the library BaseViewModel
 * and adds app-specific state management like connection state.
 */
open class BaseViewModel : com.mpdc4gsr.libunified.app.ktbase.BaseViewModel() {
    
    // Connection state management
    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
}

