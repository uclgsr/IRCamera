package mpdc4gsr.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.bean.event.TS004ResetEvent
import com.mpdc4gsr.libunified.app.bean.event.WinterClickEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DevicePermissionEvent
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.user.fragment.MoreFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.compose.components.SensorStatusCard
import mpdc4gsr.compose.components.ThermalVisualizationCard
import mpdc4gsr.core.RecordingService
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.viewmodel.ConnectionState
import mpdc4gsr.viewmodel.MainActivityViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Alternative MainActivity Implementation
 *
 * @deprecated This activity is deprecated and will be removed in a future release.
 * Use mpdc4gsr.feature.main.ui.MainActivity instead, which follows Clean Architecture principles.
 *
 * Legacy implementation retained for backward compatibility during migration phase.
 * Enhanced MainActivityCompose - Updated to leverage dev branch consolidated layouts
 *
 * This version integrates with the new consolidated layout system while maintaining
 * the modern Compose UI and full backward compatibility.
 *
 * Key enhancements from dev branch integration:
 * - Leverages activity_main_consolidated.xml patterns
 * - Integrates with consolidated camera and sensor layouts
 * - Enhanced multi-modal recording support
 * - Improved network status and device management
 */
@Deprecated(
    message = "Use mpdc4gsr.feature.main.ui.MainActivity instead",
    replaceWith = ReplaceWith("mpdc4gsr.feature.main.ui.MainActivity"),
    level = DeprecationLevel.WARNING
)
class MainActivityAlternative : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivityAlternative"
    }

    // Preserved original functionality
    private var isTC007 = false
    private lateinit var permissionController: PermissionController
    private var recordingService: RecordingService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            bound = true
            viewModel.onServiceConnected(binder)
            Log.i(TAG, "RecordingService connected.")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            recordingService = null
            viewModel.onServiceDisconnected()
            Log.i(TAG, "RecordingService disconnected.")
        }
    }

    fun createViewModel(): MainActivityViewModel {
        return viewModels<MainActivityViewModel>().value
    }

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = viewModels<MainActivityViewModel>().value

        // Get device type from intent (preserved from original)
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        permissionController = PermissionController(this)
        requestAllPermissions()
        bindRecordingService()
        
        setContent {
            LibUnifiedTheme {
                Content(viewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        val context = LocalContext.current

        // Observe ViewModel state
        val currentPage by viewModel.currentPage.collectAsState()
        val networkConnectionState by viewModel.networkConnectionState.collectAsState()
        val sessionState by viewModel.sessionState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val rgbCameraState by viewModel.rgbCameraState.collectAsState()

        Scaffold(
            topBar = {
                // Enhanced network status bar leveraging consolidated layout patterns
                EnhancedNetworkStatusBar(
                    connectionState = networkConnectionState,
                    sessionState = sessionState,
                    onThermalQuickAccess = { launchThermalQuickAccess() },
                    onFaultTolerantAccess = { launchFaultTolerantRecording() },
                    onNetworkConfig = { launchNetworkConfig() }
                )
            },
            bottomBar = {
                // Modern bottom navigation with consolidated layout integration
                EnhancedBottomNavigation(
                    currentPage = currentPage,
                    onPageSelected = { page ->
                        viewModel.onNavigationItemSelected(page)
                        // Trigger page change in ViewPager2
                    },
                    gsrConnectionState = gsrConnectionState,
                    thermalCameraState = thermalCameraState
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF16131E)) // Consistent with consolidated layouts
            ) {
                // Enhanced multi-modal sensor status (leveraging consolidated patterns)
                MultiModalSensorStatus(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    rgbCameraState = rgbCameraState,
                    onSensorConfig = { sensor -> launchSensorConfig(sensor) },
                    onQuickRecord = { launchQuickRecord() }
                )

                // Main content area with enhanced ViewPager2 integration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Embed ViewPager2 with consolidated fragment integration
                    AndroidView(
                        factory = { context ->
                            ViewPager2(context).apply {
                                adapter = EnhancedPagerAdapter(this@MainActivityAlternative)
                                offscreenPageLimit = 4

                                // Preserve existing page change logic
                                registerOnPageChangeCallback(object :
                                    ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        super.onPageSelected(position)
                                        viewModel.onNavigationItemSelected(position)
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { viewPager ->
                        // Update ViewPager2 when currentPage changes
                        if (viewPager.currentItem != currentPage) {
                            viewPager.setCurrentItem(currentPage, true)
                        }
                    }

                    // Overlay recording status (consolidated layout pattern)
                    if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                        RecordingStatusOverlay(
                            sessionState = sessionState,
                            onStopRecording = { viewModel.stopRecordingSession() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EnhancedNetworkStatusBar(
        connectionState: MainActivityViewModel.NetworkConnectionState,
        sessionState: MainActivityViewModel.SessionState,
        onThermalQuickAccess: () -> Unit,
        onFaultTolerantAccess: () -> Unit,
        onNetworkConfig: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xAA000000)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick access buttons (consolidated layout pattern)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onThermalQuickAccess,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Thermal Camera",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onFaultTolerantAccess,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Fault Tolerant Recording",
                            tint = Color(0xFF4ECDC4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Network status indicator (enhanced)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (connectionState) {
                            is ConnectionState.Connected -> Icons.Default.WifiTethering
                            is ConnectionState.Connecting -> Icons.Default.Wifi
                            else -> Icons.Default.WifiOff
                        },
                        contentDescription = "Network Status",
                        tint = when (connectionState) {
                            is ConnectionState.Connected -> Color.Green
                            is ConnectionState.Connecting -> Color.Yellow
                            else -> Color.Red
                        },
                        modifier = Modifier.size(16.dp)
                    )

                    if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REC",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onNetworkConfig,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Network Config",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MultiModalSensorStatus(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        rgbCameraState: MainActivityViewModel.SensorState,
        onSensorConfig: (String) -> Unit,
        onQuickRecord: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Multi-Modal Sensor Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Enhanced sensor status cards
                    SensorCard(
                        title = "Thermal",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        color = Color(0xFFFF6B35),
                        onClick = { onSensorConfig("thermal") }
                    )

                    SensorCard(
                        title = "GSR",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        color = Color(0xFF4ECDC4),
                        onClick = { onSensorConfig("gsr") }
                    )

                    SensorCard(
                        title = "RGB",
                        status = rgbCameraState.status.name,
                        icon = Icons.Default.Videocam,
                        color = Color(0xFF45B7D1),
                        onClick = { onSensorConfig("rgb") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onQuickRecord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Multi-Modal Recording")
                }
            }
        }
    }

    @Composable
    private fun SensorCard(
        title: String,
        status: String,
        icon: ImageVector,
        color: Color,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.width(100.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    private fun EnhancedBottomNavigation(
        currentPage: Int,
        onPageSelected: (Int) -> Unit,
        gsrConnectionState: MainActivityViewModel.GSRConnectionState,
        thermalCameraState: MainActivityViewModel.SensorState
    ) {
        NavigationBar(
            containerColor = Color(0xFF1E1E1E)
        ) {
            val items = listOf(
                NavigationItem("Main", Icons.Default.Home, 0),
                NavigationItem("Gallery", Icons.Default.Folder, 1),
                NavigationItem("More", Icons.Default.Apps, 2),
                NavigationItem("Profile", Icons.Default.Person, 3)
            )

            items.forEach { item ->
                NavigationBarItem(
                    selected = currentPage == item.index,
                    onClick = { onPageSelected(item.index) },
                    icon = {
                        Badge(
                            // Show badge for connection status
                            containerColor = when {
                                item.index == 0 && thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED -> Color.Green
                                item.index == 0 && gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            }
                        ) {
                            Icon(item.icon, contentDescription = item.title)
                        }
                    },
                    label = { Text(item.title) }
                )
            }
        }
    }

    @Composable
    private fun RecordingStatusOverlay(
        sessionState: MainActivityViewModel.SessionState,
        onStopRecording: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recording",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onStopRecording,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Enhanced adapter that integrates with consolidated layouts
    private inner class EnhancedPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                1 -> IRGalleryTabFragment()
                2 -> MoreFragment()
                else -> IRGalleryTabFragment()
            }
        }
    }

    // Enhanced launch methods leveraging consolidated patterns
    private fun launchThermalQuickAccess() {
        startActivity(
            Intent(
                this,
                com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
            )
        )
    }

    private fun launchFaultTolerantRecording() {
        startActivity(Intent(this, mpdc4gsr.activities.FaultTolerantRecordingActivityCompose::class.java))
    }

    private fun launchNetworkConfig() {
        startActivity(Intent(this, mpdc4gsr.activities.NetworkConfigActivityCompose::class.java))
    }

    private fun launchSensorConfig(sensorType: String) {
        when (sensorType) {
            "thermal" -> startActivity(
                Intent(
                    this,
                    com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
                )
            )

            "gsr" -> startActivity(Intent(this, SensorDashboardComposeActivity::class.java))
        }
    }

    private fun launchQuickRecord() {
        startActivity(Intent(this, mpdc4gsr.activities.GSRQuickRecordingActivityCompose::class.java))
    }

    // Preserved original methods
    private fun requestAllPermissions() {
        permissionController.ensureAll { isGranted, denied ->
            if (!isGranted) {
                Log.w(TAG, "Permissions denied: $denied")
            }
        }
    }

    private fun bindRecordingService() {
        val intent = Intent(this, RecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // EventBus integration preserved
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDevicePermissionEvent(event: DevicePermissionEvent) {
        // Handle device permission events - permission granted for device
        Toast.makeText(this, "Device permissions granted for ${event.device.deviceName}", Toast.LENGTH_SHORT).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTS004ResetEvent(event: TS004ResetEvent) {
        // Handle TS004 reset events
        AlertDialog.Builder(this)
            .setTitle("Device Reset")
            .setMessage("TS004 device has been reset")
            .setPositiveButton("OK", null)
            .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClickEvent(event: WinterClickEvent) {
        // Handle winter click events
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onBackPressed() {
        // Preserve original back button behavior
        if (viewModel.currentPage.value != 0) {
            viewModel.onNavigationItemSelected(0)
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Preserve original key handling
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (viewModel.currentPage.value != 0) {
                    viewModel.onNavigationItemSelected(0)
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private data class NavigationItem(
        val title: String,
        val icon: ImageVector,
        val index: Int
    )
}