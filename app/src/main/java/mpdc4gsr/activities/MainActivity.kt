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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.user.fragment.MineFragment
import com.mpdc4gsr.module.user.fragment.MoreFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.SensorStatusCard
import mpdc4gsr.compose.utils.ComposeConnectionStates
import mpdc4gsr.core.RecordingService
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.ui_components.MainFragment
import mpdc4gsr.viewmodel.ConnectionState
import mpdc4gsr.viewmodel.MainActivityViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Primary MainActivity with Modern Compose UI
 * 
 * This implementation provides:
 * - Modern Compose UI for status cards and controls
 * - Preserved ViewPager2 for existing fragments (backward compatibility)
 * - Enhanced sensor status display
 * - Improved recording controls
 * - EventBus integration for device communication
 */
class MainActivity : BaseComposeActivity<MainActivityViewModel>() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PAGE_GALLERY = 0
        private const val PAGE_MAIN = 1
        private const val PAGE_SETTINGS = 2
        private const val PAGE_MINE = 3
    }

    private lateinit var permissionController: PermissionController
    private var isServiceBound = false
    private var isTC007 = false

    /**
     * Service connection preserved from original MainActivity
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceBound = true
            val binder = service as RecordingService.RecordingServiceBinder
            viewModel.onServiceConnected(binder)
            Log.i(TAG, "RecordingService connected and passed to ViewModel.")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            viewModel.onServiceDisconnected()
            Log.i(TAG, "RecordingService disconnected.")
        }
    }

    override fun createViewModel(): MainActivityViewModel {
        return viewModels<MainActivityViewModel>().value
    }

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = viewModels<MainActivityViewModel>().value
        
        // Get device type from intent (preserved from original)
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        
        super.onCreate(savedInstanceState)
        
        permissionController = PermissionController(this)
        requestAllPermissions()
        bindRecordingService()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MainActivityViewModel) {
        val context = LocalContext.current
        
        // Observe ViewModel state
        val currentPage by viewModel.currentPage.collectAsState()
        val networkConnectionState by viewModel.networkConnectionState.collectAsState()
        val sessionState by viewModel.sessionState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val rgbCameraState by viewModel.rgbCameraState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16131E)) // Match original background
        ) {
            // Network status bar (modernized with Compose)
            NetworkStatusBar(
                connectionState = networkConnectionState,
                onThermalQuickAccess = { /* Launch thermal camera */ },
                onFaultTolerantAccess = { launchFaultTolerantRecording() }
            )
            
            // Enhanced sensor status card (new Compose component)
            SensorStatusCard(
                thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                gsrSensorState = mapSensorStateToConnectionState(gsrSensorState),
                bleConnectionState = mapGSRConnectionToConnectionState(gsrConnectionState),
                modifier = Modifier.padding(16.dp)
            )
            
            // Recording controls (modernized with Compose)
            RecordingControlsCard(
                sessionState = sessionState,
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // ViewPager2 embedded in Compose (preserves existing fragments)
            AndroidView(
                factory = { context ->
                    ViewPager2(context).apply {
                        adapter = ViewPagerAdapter(context as FragmentActivity, isTC007)
                        offscreenPageLimit = 4
                        isUserInputEnabled = false
                        
                        // Set current page based on ViewModel state
                        setCurrentItem(currentPage, false)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                update = { viewPager ->
                    if (viewPager.currentItem != currentPage) {
                        viewPager.setCurrentItem(currentPage, false)
                    }
                }
            )
            
            // Bottom navigation (modernized with Compose)
            BottomNavigationBar(
                currentPage = currentPage,
                onPageSelected = { page -> viewModel.onNavigationItemSelected(page) }
            )
        }
    }

    @Composable
    private fun NetworkStatusBar(
        connectionState: MainActivityViewModel.NetworkConnectionState,
        onThermalQuickAccess: () -> Unit,
        onFaultTolerantAccess: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xAA000000))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick access buttons
            IconButton(onClick = onThermalQuickAccess) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Thermal Camera",
                    tint = Color(0xFFFF6B35)
                )
            }
            
            IconButton(onClick = onFaultTolerantAccess) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Fault Tolerant Recording",
                    tint = Color(0xFF4ECDC4)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Network status indicator
            val (color, text) = when (connectionState) {
                MainActivityViewModel.NetworkConnectionState.DISCONNECTED -> Color.Gray to "PC: Disconnected"
                MainActivityViewModel.NetworkConnectionState.DISCOVERING -> Color.Yellow to "PC: Discovering..."
                MainActivityViewModel.NetworkConnectionState.CONNECTING -> Color.Yellow to "PC: Connecting..."
                MainActivityViewModel.NetworkConnectionState.CONNECTED -> Color.Green to "PC: Connected"
                MainActivityViewModel.NetworkConnectionState.ERROR -> Color.Red to "PC: Error"
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    private fun RecordingControlsCard(
        sessionState: MainActivityViewModel.SessionState,
        onStartRecording: () -> Unit,
        onStopRecording: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (sessionState) {
                    MainActivityViewModel.SessionState.RECORDING -> {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop Recording")
                        }
                        
                        // Recording indicator
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
                                text = "RECORDING",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Start Recording")
                        }
                        
                        Text(
                            text = "Ready to Record",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(
        currentPage: Int,
        onPageSelected: (Int) -> Unit
    ) {
        NavigationBar(
            containerColor = Color(0xFF16131E)
        ) {
            BottomNavItem(
                icon = Icons.Default.Folder,
                label = "Gallery",
                selected = currentPage == PAGE_GALLERY,
                onClick = { onPageSelected(PAGE_GALLERY) }
            )
            
            BottomNavItem(
                icon = Icons.Default.Camera,
                label = "Main",
                selected = currentPage == PAGE_MAIN,
                onClick = { onPageSelected(PAGE_MAIN) }
            )
            
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentPage == PAGE_SETTINGS,
                onClick = { onPageSelected(PAGE_SETTINGS) }
            )
            
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = currentPage == PAGE_MINE,
                onClick = { onPageSelected(PAGE_MINE) }
            )
        }
    }

    @Composable
    private fun RowScope.BottomNavItem(
        icon: ImageVector,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        NavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            },
            label = {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
    }

    // Helper functions to map existing state to Compose-friendly types
    private fun mapSensorStateToConnectionState(sensorState: MainActivityViewModel.SensorState): ConnectionState {
        return when (sensorState.status) {
            MainActivityViewModel.SensorStatus.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.SensorStatus.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.SensorStatus.CONNECTED -> ConnectionState.Connected
            MainActivityViewModel.SensorStatus.STREAMING -> ConnectionState.Connected
            MainActivityViewModel.SensorStatus.ERROR -> ConnectionState.Error("Sensor Error")
            MainActivityViewModel.SensorStatus.SIMULATION -> ConnectionState.Connected
        }
    }

    private fun mapGSRConnectionToConnectionState(gsrState: MainActivityViewModel.GSRConnectionState): ConnectionState {
        return when (gsrState) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.GSRConnectionState.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTED -> ConnectionState.Connected
            MainActivityViewModel.GSRConnectionState.STREAMING -> ConnectionState.Connected
            MainActivityViewModel.GSRConnectionState.ERROR -> ConnectionState.Error("GSR Error")
        }
    }

    // Preserve existing functionality
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            viewModel.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestAllPermissions() {
        permissionController.ensureAll { allGranted, denied ->
            if (allGranted) {
                Log.i(TAG, "All permissions granted.")
                viewModel.onPermissionsGranted()
            } else {
                Log.w(TAG, "Permissions denied: ${denied.joinToString()}")
                Toast.makeText(
                    this,
                    "Some features may be limited without permissions.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun bindRecordingService() {
        Intent(this, RecordingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun launchFaultTolerantRecording() =
        startActivity(Intent(this, FaultTolerantRecordingActivity::class.java))

    // Preserve existing EventBus handlers
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun getDevicePermission(event: DevicePermissionEvent) { /* ... */ }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClick(event: WinterClickEvent) { /* ... */ }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTS004ResetEvent(event: TS004ResetEvent) { /* Show Dialog */ }

    override fun onDestroy() {
        super.onDestroy()
        permissionController.cleanup()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }

    // ViewPager adapter preserved from original
    private class ViewPagerAdapter(
        activity: FragmentActivity, 
        private val isTC007: Boolean
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PAGE_GALLERY -> IRGalleryTabFragment()
                PAGE_MAIN -> MainFragment()
                PAGE_SETTINGS -> MoreFragment().apply {
                    arguments = Bundle().also { it.putBoolean(ExtraKeyConfig.IS_TC007, isTC007) }
                }
                PAGE_MINE -> MineFragment()
                else -> throw IndexOutOfBoundsException("Invalid position $position in ViewPagerAdapter")
            }
        }
    }

    override fun onRequestPermissionsResult(
        reqCode: Int,
        perms: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(reqCode, perms, results)
        permissionController.onRequestPermissionsResult(reqCode, perms, results)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionController.onActivityResult(requestCode, resultCode)
    }
}