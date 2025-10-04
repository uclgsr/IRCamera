package mpdc4gsr.tests

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class MainFragmentViewModel {
    data class DeviceState(
        val hasAnyDevice: Boolean,
        val hasConnectLine: Boolean,
        val hasConnectTS004: Boolean,
        val hasConnectTC007: Boolean
    )

    val deviceState: MutableStateFlow<DeviceState> = MutableStateFlow(
        DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
    )

    val batteryInfo: MutableStateFlow<String?> = MutableStateFlow(null)
    val navigationEvents: MutableSharedFlow<String> = MutableSharedFlow()
}

class MainFragmentCompose {
    @Composable
    fun Content(viewModel: MainFragmentViewModel) {
    }
}

class SensorDashboardFragmentCompose {
    @Composable
    fun Content() {
    }
}
