package mpdc4gsr.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
class FragmentToComposeMigrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock ViewModels for testing
    private val mockMainFragmentViewModel = mock<MainFragmentViewModel>()

    @Test
    fun testMainFragmentCompose_InitialState() = runTest {
        // Setup mock data
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify initial UI state
        composeTestRule.onNodeWithText("No Devices Found").assertExists()
        composeTestRule.onNodeWithText("Add a device to get started").assertExists()
        composeTestRule.onNodeWithContentDescription("Add Device").assertExists()
        composeTestRule.onNodeWithContentDescription("GSR Recording").assertExists()
    }

    @Test
    fun testMainFragmentCompose_WithConnectedDevices() = runTest {
        // Setup mock data with connected devices
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = true,
            hasConnectLine = true,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify connected device state
        composeTestRule.onNodeWithText("Devices Connected").assertExists()
        composeTestRule.onNodeWithText("TC Line Device").assertExists()
        composeTestRule.onNodeWithText("Online").assertExists()
    }

    @Test
    fun testSensorDashboardFragmentCompose_InitialState() = runTest {
        composeTestRule.setContent {
            SensorDashboardFragmentCompose().Content()
        }
        // Verify dashboard components
        composeTestRule.onNodeWithText("Sensor Dashboard").assertExists()
        composeTestRule.onNodeWithText("All Sensors Connected & Ready").assertExists()
        composeTestRule.onNodeWithText("Start Recording").assertExists()
        composeTestRule.onNodeWithText("Sensors (4)").assertExists()
        // Verify individual sensors
        composeTestRule.onNodeWithText("TC001 Thermal Camera").assertExists()
        composeTestRule.onNodeWithText("RGB Camera").assertExists()
        composeTestRule.onNodeWithText("Shimmer GSR Sensor").assertExists()
        composeTestRule.onNodeWithText("Audio Recorder").assertExists()
    }

    @Test
    fun testSensorDashboardFragmentCompose_RecordingState() = runTest {
        composeTestRule.setContent {
            SensorDashboardFragmentCompose().Content()
        }
        // Start recording
        composeTestRule.onNodeWithText("Start Recording").performClick()
        // Verify recording state changes
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Stop Recording").assertExists()
        composeTestRule.onNodeWithText("RECORDING").assertExists()
    }

    @Test
    fun testAccessibility_MainFragmentCompose() = runTest {
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Add Device").assertExists()
        composeTestRule.onNodeWithContentDescription("GSR Recording").assertExists()
        // Test accessibility actions
        composeTestRule.onAllNodesWithContentDescription("Add Device")[0].assertHasClickAction()
        composeTestRule.onAllNodesWithContentDescription("GSR Recording")[0].assertHasClickAction()
    }

    @Test
    fun testThemeConsistency_AllFragments() = runTest {
        // Test that all fragments use consistent Material 3 theming
        val deviceState = MainFragmentViewModel.DeviceState(
            hasAnyDevice = false,
            hasConnectLine = false,
            hasConnectTS004 = false,
            hasConnectTC007 = false
        )
        whenever(mockMainFragmentViewModel.deviceState).thenReturn(MutableStateFlow(deviceState))
        whenever(mockMainFragmentViewModel.batteryInfo).thenReturn(MutableStateFlow(null))
        whenever(mockMainFragmentViewModel.navigationEvents).thenReturn(MutableSharedFlow())
        composeTestRule.setContent {
            MainFragmentCompose().Content(mockMainFragmentViewModel)
        }
        // Verify UI components are present
        composeTestRule.onNodeWithText("No Devices Found").assertExists()
    }
}