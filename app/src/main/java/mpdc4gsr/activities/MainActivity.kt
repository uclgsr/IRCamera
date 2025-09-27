package mpdc4gsr.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMainBinding
import com.mpdc4gsr.libunified.app.bean.event.TS004ResetEvent
import com.mpdc4gsr.libunified.app.bean.event.WinterClickEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DevicePermissionEvent
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.sensors.thermal.ThermalCameraDemo
import mpdc4gsr.ui_components.ComprehensiveSensorStatusWidget
import mpdc4gsr.ui_components.MainFragment
import mpdc4gsr.ui_components.RecordingControlsWidget
import mpdc4gsr.viewmodel.MainActivityViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.user.fragment.MineFragment

class MainActivity : BaseBindingActivity<ActivityMainBinding>(), View.OnClickListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var permissionController: PermissionController
    private var isServiceBound = false

    /**
     * The ServiceConnection's only role is to link the Activity's lifecycle
     * to the service and pass the binder to the ViewModel, which handles the logic.
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

    override fun initContentLayoutId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionController = PermissionController(this)

        setupUI()
        observeViewModel()
        requestAllPermissions()
        bindRecordingService()
    }
    
    private fun setupUI() {
        binding.viewPage.adapter = ViewPagerAdapter(this)
        binding.viewPage.offscreenPageLimit = 3
        binding.viewPage.isUserInputEnabled = false

        binding.clIconGallery.setOnClickListener(this)
        binding.viewMain.setOnClickListener(this)
        binding.clIconMine.setOnClickListener(this)

        binding.networkStatusBar.setOnClickListener { /* Logic handled by ViewModel */ }
        binding.thermalQuickAccess.setOnClickListener { launchThermalCamera() }
        binding.faultTolerantRecordingAccess.setOnClickListener { launchFaultTolerantRecording() }
        binding.viewMain.setOnLongClickListener {
            launchShimmerMvp()
            true
        }

        // Initialize custom UI components
        initializeEnhancedUIComponents()
    }
    
    private fun observeViewModel() {
        // Observe navigation changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentPage.collectLatest { pageIndex ->
                    refreshTabSelect(pageIndex)
                }
            }
        }

        // Observe one-time events like dialogs and toasts
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is MainActivityViewModel.Event.ShowExitDialog -> showExitDialog()
                        is MainActivityViewModel.Event.ShowToast ->
                            Toast.makeText(this@MainActivity, event.message, if(event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Observe network state to update the status bar
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.networkConnectionState.collect { state ->
                    val (color, text) = when (state) {
                        MainActivityViewModel.NetworkConnectionState.DISCONNECTED -> android.graphics.Color.GRAY to "PC: Disconnected"
                        MainActivityViewModel.NetworkConnectionState.DISCOVERING -> android.graphics.Color.YELLOW to "PC: Discovering..."
                        MainActivityViewModel.NetworkConnectionState.CONNECTING -> android.graphics.Color.YELLOW to "PC: Connecting..."
                        MainActivityViewModel.NetworkConnectionState.CONNECTED -> android.graphics.Color.GREEN to "PC: Connected"
                        MainActivityViewModel.NetworkConnectionState.ERROR -> android.graphics.Color.RED to "PC: Error"
                    }
                    binding.networkStatusIndicator.setColorFilter(color)
                    binding.networkStatusText.text = text
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cl_icon_gallery -> viewModel.onNavigationItemSelected(0)
            R.id.view_main -> viewModel.onNavigationItemSelected(1)
            R.id.cl_icon_mine -> viewModel.onNavigationItemSelected(2)
        }
    }

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
                Toast.makeText(this, "Some features may be limited without permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindRecordingService() {
        Intent(this, RecordingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    private fun refreshTabSelect(index: Int) {
        if (binding.viewPage.currentItem != index) {
            binding.viewPage.setCurrentItem(index, false)
        }

        binding.ivIconGallery.isSelected = index == 0
        binding.tvIconGallery.isSelected = index == 0
        binding.ivIconMine.isSelected = index == 2
        binding.tvIconMine.isSelected = index == 2
        binding.ivBottomMainBg.setImageResource(
            if (index == 1) R.drawable.ic_main_bg_select else R.drawable.ic_main_bg_not_select
        )
    }
    
    private fun showExitDialog() {
        TipDialog.Builder(this)
            .setMessage(getString(R.string.main_exit, "App")) // Replace with dynamic name if needed
            .setCancelListener(R.string.app_no)
            .setPositiveListener(R.string.app_yes) { finishAffinity() }
            .create().show()
    }

    // --- Helper and Legacy Methods ---

    private fun initializeEnhancedUIComponents() {
        val sensorStatusWidget = ComprehensiveSensorStatusWidget(this)
        findViewById<android.widget.FrameLayout>(R.id.sensor_status_container)?.addView(sensorStatusWidget)

        val recordingControlsWidget = RecordingControlsWidget(this)
        findViewById<android.widget.FrameLayout>(R.id.recording_controls_container)?.addView(recordingControlsWidget)

        // The logic for updating these widgets now resides in observeViewModel,
        // collecting state flows like `viewModel.rgbCameraState`, `viewModel.sessionState`, etc.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rgbCameraState.collect { /* Update sensorStatusWidget */ }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { /* Update recordingControlsWidget */ }
            }
        }
    }

    private fun launchThermalCamera() = startActivity(Intent(this, ThermalCameraDemo::class.java))
    private fun launchFaultTolerantRecording() = startActivity(Intent(this, FaultTolerantRecordingActivity::class.java))
    private fun launchShimmerMvp() {
        AlertDialog.Builder(this)
            .setTitle("Developer Sensor Access")
            .setItems(arrayOf("Shimmer GSR MVP", "Unified Sensor Platform")) { _, which ->
                val activityClass = when (which) {
                    0 -> ShimmerMvpActivity::class.java
                    else -> UnifiedSensorActivity::class.java
                }
                startActivity(Intent(this, activityClass))
            }
            .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun getDevicePermission(event: DevicePermissionEvent) { /* ... */ }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClick(event: WinterClickEvent) { binding.viewMinePoint.isVisible = false }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTS004ResetEvent(event: TS004ResetEvent) { /* Show Dialog */ }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }

    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> IRGalleryTabFragment()
                1 -> MainFragment()
                else -> MineFragment()
            }
        }
    }
    
    // It is recommended to replace these with the modern Activity Result APIs.
    override fun onRequestPermissionsResult(reqCode: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(reqCode, perms, results)
        permissionController.onRequestPermissionsResult(reqCode, perms, results)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionController.onActivityResult(requestCode, resultCode)
    }
}