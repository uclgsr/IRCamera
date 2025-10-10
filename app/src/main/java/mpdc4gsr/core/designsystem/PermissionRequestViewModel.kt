package mpdc4gsr.core.designsystem

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PermissionRequestViewModel : BaseViewModel() {
    // StateFlow for permission states
    private val _permissionStates = MutableStateFlow(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()
    private val _logMessages = MutableStateFlow<List<LogMessage>>(emptyList())
    val logMessages: StateFlow<List<LogMessage>> = _logMessages.asStateFlow()
    private val _screenState = MutableStateFlow(ScreenState())
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<PermissionEvent>()
    val events: SharedFlow<PermissionEvent> = _events.asSharedFlow()
    private lateinit var permissionController: PermissionController
    private lateinit var permissionManager: PermissionManager

    data class PermissionStates(
        val camera: PermissionStatus = PermissionStatus.UNKNOWN,
        val bluetooth: PermissionStatus = PermissionStatus.UNKNOWN,
        val location: PermissionStatus = PermissionStatus.UNKNOWN,
        val storage: PermissionStatus = PermissionStatus.UNKNOWN,
        val usb: PermissionStatus = PermissionStatus.UNKNOWN,
    )

    data class ScreenState(
        val canStartRecording: Boolean = false,
        val isRequestingPermissions: Boolean = false,
        val statusMessage: String = "Checking permissions...",
    )

    data class LogMessage(
        val timestamp: String,
        val message: String,
        val id: Long = System.currentTimeMillis(),
    )

    enum class PermissionStatus {
        UNKNOWN,
        GRANTED,
        DENIED,
        NOT_AVAILABLE,
    }

    sealed class PermissionEvent {
        data class ShowError(
            val message: String,
        ) : PermissionEvent()

        data class ShowSuccess(
            val message: String,
        ) : PermissionEvent()

        object NavigateToRecording : PermissionEvent()
    }

    init {
        // Setup combined state management
        viewModelScope.launch {
            combine(
                _permissionStates,
                _logMessages,
            ) { permissionStates, _ ->
                val canStartRecording = checkCanStartRecording(permissionStates)
                val statusMessage = generateStatusMessage(permissionStates)
                ScreenState(
                    canStartRecording = canStartRecording,
                    isRequestingPermissions = false,
                    statusMessage = statusMessage,
                )
            }.collect { newState ->
                _screenState.value = newState
            }
        }
    }

    fun initialize(activity: androidx.fragment.app.FragmentActivity) {
        launchWithErrorHandling {
            permissionController = PermissionController(activity)
            permissionManager = PermissionManager(activity, permissionController)
            addLog("Permission System initialized.")
            updatePermissionStatus()
        }
    }

    fun updatePermissionStatus() {
        launchWithErrorHandling {
            val newStates =
                PermissionStates(
                    camera = if (permissionController.hasCameraPermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    bluetooth = if (permissionController.hasBluetoothPermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    location = if (permissionController.hasLocationPermission()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    storage = if (permissionController.hasStoragePermissions()) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    usb = if (permissionController.hasUsbPermissions()) PermissionStatus.GRANTED else PermissionStatus.NOT_AVAILABLE,
                )
            _permissionStates.value = newStates
            addLog("Permission status updated.")
        }
    }

    fun requestCameraPermissions() {
        launchWithLoading {
            addLog("Requesting camera permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestCameraPermissions()
                addLog(if (granted) "Camera permissions granted" else "Camera permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Camera permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Camera permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestBluetoothPermissions() {
        launchWithLoading {
            addLog("Requesting Bluetooth permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestBluetoothPermissions()
                addLog(if (granted) "Bluetooth permissions granted" else "Bluetooth permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Bluetooth permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Bluetooth permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestAllPermissions() {
        launchWithLoading {
            addLog("Starting comprehensive permission request...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestAllCriticalPermissions()
                addLog(if (granted) "Critical permissions granted" else "Some permissions were denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("All critical permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Some permissions were denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestLocationPermissions() {
        launchWithLoading {
            addLog("Requesting location permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestBluetoothPermissions() // Bluetooth requires location
                addLog(if (granted) "Location permissions granted" else "Location permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Location permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Location permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun requestStoragePermissions() {
        launchWithLoading {
            addLog("Requesting storage permissions...")
            _screenState.value = _screenState.value.copy(isRequestingPermissions = true)
            try {
                val granted = permissionManager.requestAllCriticalPermissions()
                addLog(if (granted) "Storage permissions granted" else "Storage permissions denied")
                updatePermissionStatus()
                if (granted) {
                    _events.emit(PermissionEvent.ShowSuccess("Storage permissions granted"))
                } else {
                    _events.emit(PermissionEvent.ShowError("Storage permissions denied"))
                }
            } finally {
                _screenState.value = _screenState.value.copy(isRequestingPermissions = false)
            }
        }
    }

    fun testRecordingCapabilities() {
        launchWithErrorHandling {
            addLog("Testing recording capabilities...")
            if (::permissionController.isInitialized) {
                addLog("Status: ${permissionController.getPermissionStatusMessage()}")
            } else {
                addLog("Permission controller not initialized")
            }
        }
    }

    fun startRecordingSession() {
        launchWithErrorHandling {
            val canStart = _screenState.value.canStartRecording
            if (canStart) {
                addLog("Starting recording session...")
                _events.emit(PermissionEvent.NavigateToRecording)
            } else {
                addLog("Cannot start recording - missing required permissions")
                _events.emit(PermissionEvent.ShowError("Missing required permissions"))
            }
        }
    }

    private fun addLog(message: String) {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = LogMessage(timestamp, message)
            val currentLogs = _logMessages.value.toMutableList()
            currentLogs.add(logMessage)
            // Keep only last 100 log messages to prevent memory issues
            if (currentLogs.size > 100) {
                currentLogs.removeAt(0)
            }
            _logMessages.value = currentLogs
        }
    }

    private fun checkCanStartRecording(states: PermissionStates): Boolean =
        if (::permissionController.isInitialized) {
            permissionController.canStartRecording() && permissionController.canConnectToShimmer()
        } else {
            false
        }

    private fun generateStatusMessage(states: PermissionStates): String {
        val grantedCount =
            listOf(
                states.camera,
                states.bluetooth,
                states.location,
                states.storage,
            ).count { it == PermissionStatus.GRANTED }
        return when {
            grantedCount == 4 -> "All critical permissions granted"
            grantedCount > 0 -> "Some permissions granted ($grantedCount/4)"
            else -> "No permissions granted"
        }
    }

    companion object {
        private const val TAG = "PermissionRequestViewModel"
    }
}
