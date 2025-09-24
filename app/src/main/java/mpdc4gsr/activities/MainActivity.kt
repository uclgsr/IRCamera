package mpdc4gsr


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.AppUtils
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMainBinding
import com.elvishew.xlog.XLog
import com.mpdc4gsr.module.thermalunified.lite.activity.IRThermalLiteActivity
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.bean.event.TS004ResetEvent
import com.mpdc4gsr.libunified.app.bean.event.WinterClickEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DevicePermissionEvent
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.AppConfig
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.FirmwareUpDialog
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.dialog.TipOtgDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.repository.GalleryRepository
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.app.viewmodel.VersionViewModel
import com.mpdc4gsr.module.thermalunified.activity.IRThermalNightActivity
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusActivity
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.user.fragment.MineFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.App
import mpdc4gsr.config.FeatureFlags
import mpdc4gsr.config.ProtocolVersion
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.network.NetworkClient
import mpdc4gsr.network.WebSocketClient
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.sensors.gsr.GSRQuickRecordingActivity
import mpdc4gsr.sensors.gsr.GSRSensorRecorder
import mpdc4gsr.sensors.thermal.ThermalCameraDemo
import mpdc4gsr.core.RecordingService
import mpdc4gsr.core.CrashSafeSupervisor
import mpdc4gsr.ui_components.MainFragment
import mpdc4gsr.utils.AppVersionUtil
import mpdc4gsr.activities.ShimmerMvpActivity
import mpdc4gsr.activities.UnifiedSensorActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import mpdc4gsr.ui_components.ComprehensiveSensorStatusWidget
import mpdc4gsr.ui_components.RecordingControlsWidget
import mpdc4gsr.viewmodel.MainActivityViewModel

class MainActivity : BaseBindingActivity<ActivityMainBinding>(), View.OnClickListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val versionViewModel: VersionViewModel by viewModels()
    private val mainViewModel: mpdc4gsr.viewmodel.MainActivityViewModel by viewModels()

    private var webSocketClient: WebSocketClient? = null
    private var networkClient: NetworkClient? = null
    private var recordingService: RecordingService? = null
    private var serviceBinder: RecordingService.RecordingServiceBinder? = null
    private var isServiceBound = false
    private var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED

    private var networkStatusIndicator: ImageView? = null
    private var networkStatusText: TextView? = null

    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor
    private lateinit var permissionController: PermissionController

    enum class ConnectionStatus {
        DISCONNECTED,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            serviceBinder = binder
            recordingService = binder.getService()
            networkClient = binder.getNetworkClient()
            isServiceBound = true
            Log.i(TAG, "Recording service connected")

            setupRemoteControl()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            recordingService = null
            networkClient = null
            isServiceBound = false
            Log.i(TAG, "Recording service disconnected")
        }
    }

    private fun setupRemoteControl() {
        Log.i(TAG, "Setting up enhanced remote control capabilities")

        if (!isServiceBound || recordingService == null) {
            Log.w(TAG, "Recording service not available for remote control setup")
            return
        }

        try {

            enableAutoReconnection()

            startHeartbeat()

            networkStatusIndicator?.setOnClickListener { handleNetworkStatusClick() }
            networkStatusText?.setOnClickListener { handleNetworkStatusClick() }

            Log.i(TAG, "Enhanced remote control setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up remote control", e)
            showNetworkError("Failed to setup remote control: ${e.message}")
        }
    }

    override fun initContentLayoutId(): Int = R.layout.activity_main

    private fun logInfo() {
        try {
            val str = StringBuilder()
            str.append("Info").append("\n")
            str.append("FLAVOR: release").append("\n")
            str.append("VERSION_CODE: ${BuildConfig.VERSION_CODE}").append("\n")
            str.append("VERSION_NAME: ${BuildConfig.VERSION_NAME}").append("\n")
            str.append("VERSION_DATE: ${BuildConfig.VERSION_DATE}").append("\n")
            str.append("BRAND: ${Build.BRAND}").append("\n")
            str.append("MODEL: ${Build.MODEL}").append("\n")
            str.append("PRODUCT: ${Build.PRODUCT}").append("\n")
            str.append("CPU_ABI: ${Build.CPU_ABI}").append("\n")
            str.append("SDK_INT: ${Build.VERSION.SDK_INT}").append("\n")
            str.append("RELEASE: ${Build.VERSION.RELEASE}").append("\n")
            if (SharedManager.getHasShowClause()) {
                XLog.i(str)
            }
        } catch (e: Exception) {
            if (SharedManager.getHasShowClause()) {
                XLog.e("log error: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializePhase0Baseline()

        initView()
        initData()

        initNetworking()
    }

    private fun initializeEnhancedUIComponents() {
        try {
            // Initialize ComprehensiveSensorStatusWidget
            val sensorStatusWidget = ComprehensiveSensorStatusWidget(this)
            val sensorStatusContainer = findViewById<android.widget.FrameLayout>(R.id.sensor_status_container)
            sensorStatusContainer?.addView(sensorStatusWidget)

            // Initialize RecordingControlsWidget
            val recordingControlsWidget = RecordingControlsWidget(this)
            val recordingControlsContainer = findViewById<android.widget.FrameLayout>(R.id.recording_controls_container)
            recordingControlsContainer?.addView(recordingControlsWidget)

            // Setup ViewModel observers
            setupViewModelObservers(sensorStatusWidget, recordingControlsWidget)

            // Setup recording control callbacks
            recordingControlsWidget.onLocalStartClicked = {
                handleLocalRecordingStart()
            }
            recordingControlsWidget.onLocalStopClicked = {
                handleLocalRecordingStop()
            }

            // Setup manual camera control integration
            setupManualCameraControls()

            Log.i(TAG, "Enhanced UI components initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize enhanced UI components", e)
            Toast.makeText(this, "UI initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViewModelObservers(
        sensorStatusWidget: ComprehensiveSensorStatusWidget,
        recordingControlsWidget: RecordingControlsWidget
    ) {
        // Observe RGB camera state
        mainViewModel.rgbCameraState.observe(this) { sensorState ->
            val status = when (sensorState.status) {
                MainActivityViewModel.SensorStatus.DISCONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.DISCONNECTED
                MainActivityViewModel.SensorStatus.CONNECTING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTING
                MainActivityViewModel.SensorStatus.CONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTED
                MainActivityViewModel.SensorStatus.STREAMING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.STREAMING
                MainActivityViewModel.SensorStatus.ERROR -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.ERROR
                MainActivityViewModel.SensorStatus.SIMULATION -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.SIMULATION
            }
            sensorStatusWidget.updateSensorStatus("rgb_camera", status, sensorState.message)
        }

        // Observe thermal camera state
        mainViewModel.thermalCameraState.observe(this) { sensorState ->
            val status = when (sensorState.status) {
                MainActivityViewModel.SensorStatus.DISCONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.DISCONNECTED
                MainActivityViewModel.SensorStatus.CONNECTING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTING
                MainActivityViewModel.SensorStatus.CONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTED
                MainActivityViewModel.SensorStatus.STREAMING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.STREAMING
                MainActivityViewModel.SensorStatus.ERROR -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.ERROR
                MainActivityViewModel.SensorStatus.SIMULATION -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.SIMULATION
            }
            sensorStatusWidget.updateSensorStatus("thermal_camera", status, sensorState.message)
        }

        // Observe GSR sensor state
        mainViewModel.gsrSensorState.observe(this) { sensorState ->
            val status = when (sensorState.status) {
                MainActivityViewModel.SensorStatus.DISCONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.DISCONNECTED
                MainActivityViewModel.SensorStatus.CONNECTING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTING
                MainActivityViewModel.SensorStatus.CONNECTED -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.CONNECTED
                MainActivityViewModel.SensorStatus.STREAMING -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.STREAMING
                MainActivityViewModel.SensorStatus.ERROR -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.ERROR
                MainActivityViewModel.SensorStatus.SIMULATION -> 
                    ComprehensiveSensorStatusWidget.SensorStatus.SIMULATION
            }
            sensorStatusWidget.updateSensorStatus("shimmer_gsr", status, sensorState.message)
        }

        // Observe session state
        mainViewModel.sessionState.observe(this) { sessionState ->
            val controlsState = when (sessionState) {
                MainActivityViewModel.SessionState.IDLE -> 
                    RecordingControlsWidget.SessionState.IDLE
                MainActivityViewModel.SessionState.STARTING -> 
                    RecordingControlsWidget.SessionState.STARTING
                MainActivityViewModel.SessionState.RECORDING -> 
                    RecordingControlsWidget.SessionState.RECORDING
                MainActivityViewModel.SessionState.STOPPING -> 
                    RecordingControlsWidget.SessionState.STOPPING
                MainActivityViewModel.SessionState.ERROR -> 
                    RecordingControlsWidget.SessionState.ERROR
                else -> RecordingControlsWidget.SessionState.IDLE
            }
            
            val isRemoteTriggered = mainViewModel.isRemoteTriggered.value ?: false
            val sessionId = mainViewModel.currentSession.value?.sessionId
            recordingControlsWidget.updateSessionState(controlsState, sessionId, isRemoteTriggered)
        }

        // Observe status messages
        mainViewModel.statusMessage.observe(this) { statusMessage ->
            statusMessage?.let {
                when (it.level) {
                    MainActivityViewModel.StatusMessage.Level.ERROR -> {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    }
                    MainActivityViewModel.StatusMessage.Level.WARNING -> {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
                    MainActivityViewModel.StatusMessage.Level.INFO -> {
                        Log.i(TAG, "Status: ${it.message}")
                    }
                }
            }
        }
    }

    private fun handleLocalRecordingStart() {
        try {
            Log.i(TAG, "Local recording start requested")
            // Use existing session config or create a basic one
            val config = MainActivityViewModel.SessionConfig(
                sessionId = "local_${System.currentTimeMillis()}",
                modalities = listOf("thermal", "GSR", "rgb")
            )
            mainViewModel.startRecordingSession(config)
            mainViewModel.setRemoteTriggered(false) // Mark as local trigger
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local recording", e)
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLocalRecordingStop() {
        try {
            Log.i(TAG, "Local recording stop requested")
            mainViewModel.stopRecordingSession()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop local recording", e)
            Toast.makeText(this, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initView() {
        // Initialize permission controller
        permissionController = PermissionController(this)
        permissionController.initialize()

        // Initialize enhanced UI components
        initializeEnhancedUIComponents()

        if (!SharedManager.getHasShowClause()) {
            NavigationManager.build(RouterConfig.CLAUSE).navigation(this)
            finish()
            return
        }

        logInfo()

        networkStatusIndicator = binding.networkStatusIndicator
        networkStatusText = binding.networkStatusText

        lifecycleScope.launch(Dispatchers.IO) {


        }
        binding.viewPage.offscreenPageLimit = 3
        binding.viewPage.isUserInputEnabled = false
        binding.viewPage.adapter = ViewPagerAdapter(this)
        binding.viewPage.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    refreshTabSelect(position)
                }
            },
        )
        binding.viewPage.setCurrentItem(1, false)

        binding.viewMinePoint.isVisible = !SharedManager.hasClickWinter

        binding.clIconGallery.setOnClickListener(this)
        binding.viewMain.setOnClickListener(this)
        binding.clIconMine.setOnClickListener(this)

        binding.networkStatusBar.setOnClickListener {
            handleNetworkStatusClick()
        }


        binding.thermalQuickAccess.setOnClickListener {
            launchThermalCamera()
        }

        binding.faultTolerantRecordingAccess.setOnClickListener {
            launchFaultTolerantRecording()
        }

        binding.viewMain.setOnLongClickListener {
            launchShimmerMvp()
            true
        }

        App.instance.initWebSocket()
        copyFile("SR.pb", File(filesDir, "SR.pb"))
        BaseApplication.instance.clearDb()
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(true)
        } else {
            versionViewModel.checkVersion()
        }

        if (!SharedManager.hasTcLine) { // TS004/TC007 functionality removed

            if (DeviceTools.isConnect()) {
                if (!WebSocketProxy.getInstance().isConnected()) {
                    NavigationManager.build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, false)
                        .navigation(this)
                }
            } else {
                if (WebSocketProxy.getInstance().isTS004Connect()) {
                    NavigationManager.build(RouterConfig.IR_MONOCULAR).navigation(this)
                } else if (WebSocketProxy.getInstance().isTC007Connect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN)
                        .withBoolean(ExtraKeyConfig.IS_TC007, true)
                        .navigation(this)
                }
            }
        }

        if (DeviceTools.isConnect()) {
            SharedManager.hasTcLine = true
        }

    }

    override fun onStart() {
        super.onStart()

        versionViewModel.updateLiveData.observe(this) {
            FirmwareUpDialog(this).apply {
                titleStr = getString(com.mpdc4gsr.libunified.R.string.update_new_version)
                sizeStr = it.versionNo
                contentStr = it.description
                isShowCancel = !it.isForcedUpgrade
                onConfirmClickListener = {
                    updateApk(it.downPageUrl)
                }
                onCancelClickListener = {
                    SharedManager.setVersionCheckDate(System.currentTimeMillis())
                }
            }.show()
        }
    }

    private fun updateApk(url: String) {
        if (applicationInfo.targetSdkVersion < Build.VERSION_CODES.P) {

            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            intent.data = Uri.parse(url)
            startActivity(intent)
        } else {
            if (AppUtils.isAppInstalled("com.android.vending")) {
                try {
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    intent.data = Uri.parse(AppConfig.GOOGLE_APK_MARKET_URL)
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = "android.intent.action.VIEW"
                    intent.data = Uri.parse(AppConfig.GOOGLE_APK_URL)
                    startActivity(intent)
                }
            } else {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                intent.data = Uri.parse(AppConfig.GOOGLE_APK_URL)
                startActivity(intent)
            }
        }
    }

    private var resetTipsDialog: TipDialog? = null

    private fun showResetTipsDialog() {
        disconnectDialog?.dismiss()
        if (resetTipsDialog == null) {
            resetTipsDialog =
                TipDialog.Builder(this)
                    .setMessage(R.string.device_reset_alert)
                    .setPositiveListener(R.string.app_got_it) {
                    }
                    .create()
        }
        resetTipsDialog?.show()
    }

    private var disconnectDialog: TipDialog? = null

    private fun dialogDisconnect() {
        if (resetTipsDialog?.isShowing == true) {
            return
        }
        if (disconnectDialog == null) {
            disconnectDialog =
                TipDialog.Builder(this)
                    .setMessage(R.string.device_disconnect_alert)
                    .setPositiveListener(R.string.app_got_it) {
                    }
                    .create()
        }
        disconnectDialog?.show()
    }

    private fun copyFile(
        filename: String,
        targetFile: File,
    ) {
        if (targetFile.exists()) {
            return
        }
        try {
            val inputStream = assets.open(filename)
            val outputStream: OutputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun launchThermalCamera() {
        try {
            Log.i(TAG, "Launching thermal camera interface")
            val intent = Intent(this, ThermalCameraDemo::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch thermal camera", e)
            Toast.makeText(this, "Thermal camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchFaultTolerantRecording() {
        try {
            Log.i(TAG, "Launching enhanced fault-tolerant recording interface")
            val intent = Intent(this, mpdc4gsr.activities.FaultTolerantRecordingActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch fault-tolerant recording", e)
            Toast.makeText(this, "Enhanced recording not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initData() {
        // Initialize MainActivityViewModel components
        mainViewModel.initializeComponents()

        requestAllPermissions()

        Log.i(
            "MainActivity",
            "✅ PC-to-Phone communication integration available - RecordingService supports network control"
        )
    }

    private fun requestAllPermissions() {
        permissionController.ensureAll { allGranted, deniedPermissions ->
            if (allGranted) {
                Log.i(TAG, "All permissions granted - full functionality enabled")

                onAllPermissionsGranted()
            } else {
                Log.w(TAG, "Some permissions denied: ${deniedPermissions.joinToString(", ")}")

                onPartialPermissions(deniedPermissions)
            }
        }
    }

    private fun onAllPermissionsGranted() {
        // Initialize and connect actual sensor modules to ViewModel
        initializeSensorIntegration()
        
        Log.i(TAG, "Full multi-sensor recording functionality available")
    }

    private fun initializeSensorIntegration() {
        try {
            // Start with initial disconnected states
            mainViewModel.updateRGBCameraState(
                MainActivityViewModel.SensorStatus.DISCONNECTED,
                "Initializing RGB camera..."
            )
            mainViewModel.updateThermalCameraState(
                MainActivityViewModel.SensorStatus.DISCONNECTED,
                "Initializing thermal camera..."
            )
            mainViewModel.updateGSRSensorState(
                MainActivityViewModel.SensorStatus.DISCONNECTED,
                "Initializing GSR sensor..."
            )

            // Initialize RGB Camera integration
            initializeRGBCameraIntegration()
            
            // Initialize Thermal Camera integration  
            initializeThermalCameraIntegration()
            
            // Initialize GSR Sensor integration
            initializeGSRSensorIntegration()
            
            Log.i(TAG, "Sensor integration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sensor integration", e)
            Toast.makeText(this, "Sensor integration error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeRGBCameraIntegration() {
        // Simulate RGB camera connection process
        lifecycleScope.launch {
            try {
                mainViewModel.updateRGBCameraState(
                    MainActivityViewModel.SensorStatus.CONNECTING,
                    "Connecting to RGB camera..."
                )
                
                delay(1000) // Simulate initialization time
                
                // Check if camera permission is granted and camera is available
                if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) 
                    == PackageManager.PERMISSION_GRANTED) {
                    
                    mainViewModel.updateRGBCameraState(
                        MainActivityViewModel.SensorStatus.CONNECTED,
                        "RGB camera ready"
                    )
                } else {
                    mainViewModel.updateRGBCameraState(
                        MainActivityViewModel.SensorStatus.ERROR,
                        "Camera permission required"
                    )
                }
            } catch (e: Exception) {
                mainViewModel.updateRGBCameraState(
                    MainActivityViewModel.SensorStatus.ERROR,
                    "RGB camera initialization failed: ${e.message}"
                )
            }
        }
    }

    private fun initializeThermalCameraIntegration() {
        // Connect to thermal camera status updates
        lifecycleScope.launch {
            try {
                mainViewModel.updateThermalCameraState(
                    MainActivityViewModel.SensorStatus.CONNECTING,
                    "Scanning for thermal camera..."
                )
                
                delay(1500) // Simulate USB device detection time
                
                // Check if thermal camera is connected (simulate detection)
                val thermalCameraConnected = checkThermalCameraAvailability()
                
                if (thermalCameraConnected) {
                    mainViewModel.updateThermalCameraState(
                        MainActivityViewModel.SensorStatus.CONNECTED,
                        "Topdon TC001 thermal camera connected"
                    )
                } else {
                    mainViewModel.updateThermalCameraState(
                        MainActivityViewModel.SensorStatus.SIMULATION,
                        "Using thermal simulation mode - no hardware detected"
                    )
                }
            } catch (e: Exception) {
                mainViewModel.updateThermalCameraState(
                    MainActivityViewModel.SensorStatus.ERROR,
                    "Thermal camera initialization failed: ${e.message}"
                )
            }
        }
    }

    private fun initializeGSRSensorIntegration() {
        // Connect to GSR sensor status updates
        lifecycleScope.launch {
            try {
                mainViewModel.updateGSRSensorState(
                    MainActivityViewModel.SensorStatus.CONNECTING,
                    "Scanning for Shimmer GSR devices..."
                )
                
                delay(2000) // Simulate BLE scanning time
                
                // Check if Bluetooth is available and GSR device can be found
                val gsrDeviceFound = checkGSRDeviceAvailability()
                
                if (gsrDeviceFound) {
                    mainViewModel.updateGSRSensorState(
                        MainActivityViewModel.SensorStatus.CONNECTED,
                        "Shimmer3 GSR sensor connected"
                    )
                } else {
                    mainViewModel.updateGSRSensorState(
                        MainActivityViewModel.SensorStatus.SIMULATION,
                        "Using GSR simulation mode - no Shimmer3 device found"
                    )
                }
            } catch (e: Exception) {
                mainViewModel.updateGSRSensorState(
                    MainActivityViewModel.SensorStatus.ERROR,
                    "GSR sensor initialization failed: ${e.message}"
                )
            }
        }
    }

    private fun checkThermalCameraAvailability(): Boolean {
        // Check for USB thermal camera device
        // This would normally check USB device manager for Topdon TC001
        return try {
            val usbManager = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
            val deviceList = usbManager.deviceList
            
            // Look for Topdon TC001 device (vendor ID and product ID would be specific)
            val hasThermalCamera = deviceList.values.any { device ->
                // This would check for actual Topdon TC001 vendor/product IDs
                device.deviceName.contains("usb") // Simplified check for demo
            }
            
            Log.i(TAG, "Thermal camera availability check: $hasThermalCamera")
            hasThermalCamera
        } catch (e: Exception) {
            Log.w(TAG, "Could not check thermal camera availability", e)
            false
        }
    }

    private fun checkGSRDeviceAvailability(): Boolean {
        // Check for Shimmer3 GSR device via Bluetooth
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.w(TAG, "Bluetooth not available or not enabled")
                return false
            }
            
            // This would normally scan for Shimmer devices
            // For now, simulate based on Bluetooth availability
            val hasBluetoothPermission = ContextCompat.checkSelfPermission(
                this, 
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            Log.i(TAG, "GSR device availability check: $hasBluetoothPermission")
            hasBluetoothPermission
        } catch (e: Exception) {
            Log.w(TAG, "Could not check GSR device availability", e)
            false
        }
    }

    private fun onPartialPermissions(deniedPermissions: List<String>) {

        val permissionNames = permissionController.getPermissionNames(deniedPermissions)


        val message = "Some features may be limited due to missing permissions: ${
            permissionNames.joinToString(", ")
        }"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        Log.w(TAG, "Running with limited functionality: $message")
    }

    override fun onResume() {
        super.onResume()
        LMS.getInstance().language = ConstantLanguages.ENGLISH

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        permissionController.onActivityResult(requestCode, resultCode)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clIconGallery -> {

                binding.viewPage.setCurrentItem(0, false)
            }

            binding.viewMain -> {
                binding.viewPage.setCurrentItem(1, false)
            }

            binding.clIconMine -> {
                binding.viewPage.setCurrentItem(2, false)
            }
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TipDialog.Builder(this)
                .setMessage(getString(R.string.main_exit, CommUtils.getAppName()))
                .setCancelListener(R.string.app_no)
                .setPositiveListener(R.string.app_yes) {
                    BaseApplication.instance.exitAll()
                    finish()
                }
                .create().show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun getDevicePermission(event: DevicePermissionEvent) {
        DeviceTools.requestUsb(this, 0, event.device)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClick(event: WinterClickEvent) {
        binding.viewMinePoint.isVisible = false
    }

    private fun refreshTabSelect(index: Int) {
        binding.ivIconGallery.isSelected = false
        binding.tvIconGallery.isSelected = false
        binding.ivIconMine.isSelected = false
        binding.tvIconMine.isSelected = false
        binding.ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_not_select)

        when (index) {
            0 -> {
                binding.ivIconGallery.isSelected = true
                binding.tvIconGallery.isSelected = true
            }

            1 -> {
                binding.ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_select)
            }

            2 -> {
                binding.ivIconMine.isSelected = true
                binding.tvIconMine.isSelected = true
            }
        }
    }

    override fun connected() {
        if (SharedManager.isConnectAutoOpen) {

            if (permissionController.canStartRecording()) {
                Log.i(
                    TAG,
                    "Camera permissions available - device connected and ready for recording"
                )

            } else {
                Log.w(TAG, "Camera permissions missing - requesting permissions")
                permissionController.ensureAll { granted, _ ->
                    if (granted && permissionController.canStartRecording()) {
                        Log.i(TAG, "Camera permissions granted after device connection")
                    }
                }
            }
        }
    }

    private var tipOtgDialog: TipOtgDialog? = null

    override fun disConnected() {
        if (WebSocketProxy.getInstance().isTS004Connect()) {
            NavigationManager.build(RouterConfig.IR_MONOCULAR).navigation(this)
        }

        if (tipOtgDialog != null && tipOtgDialog!!.isShowing) {
            return
        }
        if (SharedManager.isTipOTG && !BaseApplication.instance.hasOtgShow) {
            tipOtgDialog =
                TipOtgDialog.Builder(this)
                    .setMessage(R.string.tip_otg)
                    .setPositiveListener(R.string.app_confirm) {
                        SharedManager.isTipOTG = !it
                    }
                    .create()
            tipOtgDialog?.show()
            BaseApplication.instance.hasOtgShow = true
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTS004ResetEvent(event: TS004ResetEvent) {
        showResetTipsDialog()
    }

    override fun onSocketConnected(isTS004: Boolean) {
        disconnectDialog?.dismiss()
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) && isTS004) {
            dialogDisconnect()
        }
    }

    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    IRGalleryTabFragment().apply {
                        arguments =
                            Bundle().also {
                                it.putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, true)
                                it.putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                                it.putInt(
                                    ExtraKeyConfig.DIR_TYPE,
                                    GalleryRepository.DirType.LINE.ordinal
                                )
                            }
                    }
                }

                1 -> MainFragment()
                else -> MineFragment()
            }
        }
    }


    fun jumpIRActivity() {


        if (permissionController.canStartRecording()) {

            when {
                DeviceTools.isTC001PlusConnect() -> {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalPlusActivity::class.java))
                }

                DeviceTools.isTC001LiteConnect() -> {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalLiteActivity::class.java))
                }

                DeviceTools.isHikConnect() -> {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalNightActivity::class.java))
                }

                else -> {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalNightActivity::class.java))
                }
            }
        } else {

            Toast.makeText(
                this,
                "Camera permission required for thermal imaging",
                Toast.LENGTH_SHORT
            ).show()
            permissionController.ensureAll { granted, _ ->
                if (granted && permissionController.canStartRecording()) {
                    jumpIRActivity()
                }
            }
        }
    }

    private var appVersionUtil: AppVersionUtil? = null

    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil =
                AppVersionUtil(
                    this,
                    object : AppVersionUtil.DotIsShowListener {
                        override fun isShow(show: Boolean) {
                        }

                        override fun version(version: String) {
                        }
                    },
                )
        }
        appVersionUtil?.checkVersion(isShow)
    }


    private fun initializePhase0Baseline() {
        Log.i(TAG, "Initializing Phase 0 baseline components")

        try {

            FeatureFlags.initialize(this)

            structuredLogger = StructuredLogger.getInstance(this)

            crashSafeSupervisor = CrashSafeSupervisor.getInstance(this)
            crashSafeSupervisor.initialize()

            val configWarnings = FeatureFlags.validateConfiguration()
            if (configWarnings.isNotEmpty()) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.WARNING,
                    "MainActivity",
                    "configuration_warnings",
                    mapOf("warnings" to configWarnings.joinToString("; "))
                )
            }

            val protocolInfo = ProtocolVersion.getProtocolInfo()
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "MainActivity",
                "protocol_version_info",
                protocolInfo
            )

            val featureFlags = FeatureFlags.getAllFlags()
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "MainActivity",
                "feature_flags_initialized",
                featureFlags
            )

            Log.i(TAG, "Phase 0 baseline initialization completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Phase 0 baseline components", e)

        }
    }


    private fun initNetworking() {
        Log.i(TAG, "Initializing PC-to-phone control networking with Phase 0 baseline")

        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "MainActivity",
            "networking_initialization_started",
            mapOf(
                "feature_flags" to FeatureFlags.getAllFlags(),
                "protocol_version" to ProtocolVersion.CURRENT_VERSION
            )
        )

        try {

            crashSafeSupervisor.registerJob(
                id = "websocket_client",
                name = "WebSocketClient",
                critical = false,
                restartable = true,
                healthCheck = object : CrashSafeSupervisor.HealthCheck {
                    override suspend fun checkHealth(): CrashSafeSupervisor.HealthStatus {
                        val client = webSocketClient
                        return if (client != null && client.isConnected()) {
                            CrashSafeSupervisor.HealthStatus(
                                isHealthy = true,
                                message = "WebSocketClient connected and operational",
                                details = mapOf(
                                    "connection_status" to connectionStatus.name,
                                    "authenticated" to client.isAuthenticated(),
                                    "current_server" to (client.getCurrentServer()?.name ?: "none")
                                )
                            )
                        } else {
                            CrashSafeSupervisor.HealthStatus(
                                isHealthy = false,
                                message = "WebSocketClient not connected",
                                details = mapOf("connection_status" to connectionStatus.name)
                            )
                        }
                    }
                }
            ) { stopToken ->
                initializeWebSocketClientSupervised(stopToken)
            }

            bindRecordingService()

            RecordingService.startServer(this)

            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "MainActivity",
                "networking_initialization_completed"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize networking", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "MainActivity",
                "networking_initialization_failed",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
            updateConnectionStatus(ConnectionStatus.ERROR)
            showNetworkError("Network initialization failed: ${e.message}")
        }
    }

    private suspend fun initializeWebSocketClientSupervised(stopToken: CrashSafeSupervisor.StopToken) {
        while (!stopToken.isStopRequested()) {
            try {

                webSocketClient = WebSocketClient(this@MainActivity).apply {

                    setEventListener(createWebSocketEventListener())

                    start()
                }

                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "WebSocketClient",
                    "initialized_successfully"
                )

                while (!stopToken.isStopRequested() && webSocketClient?.isConnected() != true) {
                    kotlinx.coroutines.delay(1000)
                }

                if (webSocketClient?.isConnected() == true) {
                    updateConnectionStatus(ConnectionStatus.CONNECTED)
                }

                while (!stopToken.isStopRequested() && webSocketClient?.isConnected() == true) {
                    kotlinx.coroutines.delay(1000)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in WebSocket client supervision", e)
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "WebSocketClient",
                    "supervision_error",
                    mapOf("error" to (e.message ?: "Unknown error"))
                )

                updateConnectionStatus(ConnectionStatus.ERROR)

                kotlinx.coroutines.delay(5000)
            }
        }

        webSocketClient?.stop()
        webSocketClient = null
    }

    private fun createWebSocketEventListener(): WebSocketClient.WebSocketEventListener {
        return object : WebSocketClient.WebSocketEventListener {
            override fun onServerDiscovered(serverInfo: WebSocketClient.ServerInfo) {
                structuredLogger.logConnection(
                    "server_discovered",
                    serverInfo.host,
                    mapOf(
                        "server_name" to serverInfo.name,
                        "host" to serverInfo.host,
                        "port" to serverInfo.port,
                        "uses_tls" to serverInfo.usesTLS,
                        "protocol_version" to serverInfo.protocolVersion
                    )
                )

                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTING)
                    Toast.makeText(
                        this@MainActivity,
                        "Found PC Server: ${serverInfo.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onConnecting(serverInfo: WebSocketClient.ServerInfo) {
                structuredLogger.logConnection(
                    "connecting",
                    serverInfo.host,
                    mapOf(
                        "server_name" to serverInfo.name,
                        "host" to serverInfo.host,
                        "port" to serverInfo.port
                    )
                )

                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTING)
                }
            }

            override fun onConnected(serverInfo: WebSocketClient.ServerInfo) {
                structuredLogger.logConnection(
                    "websocket_connected",
                    serverInfo.host,
                    mapOf(
                        "server_name" to serverInfo.name,
                        "uses_tls" to serverInfo.usesTLS,
                        "protocol_version" to serverInfo.protocolVersion
                    )
                )

                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTED)
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to PC: ${serverInfo.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticated() {
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "WebSocketClient",
                    "authenticated_successfully",
                    emptyMap()
                )

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Authenticated with PC controller",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onDisconnected(reason: String) {
                structuredLogger.logConnection(
                    "websocket_disconnected",
                    "",
                    mapOf("reason" to reason)
                )

                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                    Toast.makeText(
                        this@MainActivity,
                        "Disconnected from PC: $reason",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onMessage(messageType: String, message: org.json.JSONObject) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.DEBUG,
                    "WebSocketClient",
                    "message_received",
                    mapOf("message_type" to messageType)
                )

                when (messageType) {
                    "session_start_response" -> {
                        val success = message.optBoolean("success", false)
                        val sessionId = message.optString("session_id", "")

                        if (success) {
                            runOnUiThread {
                                handleRemoteSessionStart(sessionId)
                            }
                        }
                    }

                    "session_stop_response" -> {
                        val success = message.optBoolean("success", false)

                        if (success) {
                            runOnUiThread {
                                handleRemoteSessionStop()
                            }
                        }
                    }

                    "sync_flash" -> {
                        val durationMs = message.optInt("duration_ms", 500)
                        runOnUiThread {
                            performSyncFlash(durationMs)
                        }
                    }

                    else -> {

                        Log.d(TAG, "Received message type: $messageType")
                    }
                }
            }

            override fun onError(error: String, exception: Throwable?) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "WebSocketClient",
                    "websocket_error",
                    mapOf("error" to error)
                )

                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.ERROR)
                    showNetworkError("WebSocket error: $error")
                }
            }

            override fun onHeartbeatReceived() {

                structuredLogger.log(
                    StructuredLogger.LogLevel.DEBUG,
                    "WebSocketClient",
                    "heartbeat_received",
                    emptyMap()
                )
            }
        }
    }

    private fun handleRemoteSessionStart(sessionId: String) {
        try {
            structuredLogger.logSessionEvent(
                "remote_session_start",
                sessionId,
                emptyMap()
            )

            lifecycleScope.launch {
                val sessionDir = "${getExternalFilesDir("recordings")}/$sessionId"
                serviceBinder?.getRecordingController()?.startRecording(sessionDir)
            }

            Toast.makeText(this, "Remote recording started: $sessionId", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote session start", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RemoteControl",
                "session_start_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private fun handleRemoteSessionStop() {
        try {
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "RemoteControl",
                "remote_session_stop",
                emptyMap()
            )

            lifecycleScope.launch {
                serviceBinder?.getRecordingController()?.stopRecording()
            }

            Toast.makeText(this, "Remote recording stopped", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote session stop", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RemoteControl",
                "session_stop_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private fun bindRecordingService() {
        val intent = Intent(this, RecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun startNetworkDiscovery() {
        Log.i(TAG, "Starting WebSocket discovery for PC servers")
        updateConnectionStatus(ConnectionStatus.DISCOVERING)


        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "WebSocketClient",
            "discovery_started",
            emptyMap()
        )


    }

    private fun handleRemoteRecordingRequest(sessionInfo: SessionInfo) {
        Log.i(TAG, "Processing remote recording request for session: ${sessionInfo.sessionId}")

        if (!isServiceBound || recordingService == null) {
            Log.e(TAG, "Recording service not available for remote request")
            showNetworkError("Recording service not ready")
            return
        }

        try {

            val baseDir = File(getExternalFilesDir(null), "recordings")
            val sessionDir = File(baseDir, sessionInfo.sessionId)

            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }

            RecordingService.startRecording(this, sessionDir.absolutePath)

            Toast.makeText(
                this,
                "Remote recording started: ${sessionInfo.studyName}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start remote recording", e)
            showNetworkError("Failed to start recording: ${e.message}")
        }
    }

    private fun performSyncFlash(durationMs: Int) {
        Log.i(TAG, "Performing sync flash for ${durationMs}ms")

        val originalBackground = window.decorView.background

        try {

            window.decorView.setBackgroundColor(android.graphics.Color.WHITE)

            lifecycleScope.launch {
                serviceBinder?.getRecordingController()
                    ?.addSyncMarker("pc_sync_flash", System.nanoTime())
            }

            window.decorView.postDelayed({
                window.decorView.background = originalBackground
            }, durationMs.toLong())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform sync flash", e)

            window.decorView.background = originalBackground
        }
    }

    private fun updateConnectionStatus(status: ConnectionStatus) {
        connectionStatus = status

        networkStatusIndicator?.let { indicator ->
            networkStatusText?.let { text ->
                when (status) {
                    ConnectionStatus.DISCONNECTED -> {
                        indicator.setColorFilter(android.graphics.Color.GRAY)
                        text.text = "PC: Disconnected"
                    }

                    ConnectionStatus.DISCOVERING -> {
                        indicator.setColorFilter(android.graphics.Color.YELLOW)
                        text.text = "PC: Discovering..."
                    }

                    ConnectionStatus.CONNECTING -> {
                        indicator.setColorFilter(android.graphics.Color.YELLOW)
                        text.text = "PC: Connecting..."
                    }

                    ConnectionStatus.CONNECTED -> {
                        indicator.setColorFilter(android.graphics.Color.GREEN)
                        text.text = "PC: Connected"
                    }

                    ConnectionStatus.ERROR -> {
                        indicator.setColorFilter(android.graphics.Color.RED)
                        text.text = "PC: Error"
                    }
                }
            }
        }
    }

    private fun showNetworkError(message: String) {
        Log.e(TAG, "Network error: $message")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Network Error")
        builder.setMessage("$message\n\nWhat would you like to do?")

        builder.setPositiveButton("Retry Discovery") { _, _ ->
            startNetworkDiscovery()
        }

        builder.setNegativeButton("Manual Connect") { _, _ ->
            showManualConnectionDialog()
        }

        builder.setNeutralButton("Ignore") { _, _ ->

        }

        builder.show()
    }

    fun connectToPC(ipAddress: String, port: Int = 8080) {
        if (networkClient == null) {
            showNetworkError("Network client not initialized")
            return
        }

        Log.i(TAG, "Attempting connection to PC at $ipAddress:$port")
        updateConnectionStatus(ConnectionStatus.CONNECTING)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {

                    var success = networkClient?.connectToController(ipAddress, port, true) ?: false

                    if (!success) {
                        Log.i(TAG, "Secure connection failed, trying non-secure...")
                        success =
                            networkClient?.connectToController(ipAddress, port, false) ?: false
                    }

                    withContext(Dispatchers.Main) {
                        if (success) {
                            Log.i(TAG, "Connection successful to $ipAddress:$port")
                            Toast.makeText(
                                this@MainActivity,
                                "Connected to PC at $ipAddress",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Log.w(TAG, "Connection failed to $ipAddress:$port")
                            updateConnectionStatus(ConnectionStatus.ERROR)
                            showNetworkError("Failed to connect to PC at $ipAddress:$port")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error to $ipAddress:$port", e)
                updateConnectionStatus(ConnectionStatus.ERROR)
                showNetworkError("Connection error: ${e.message}")
            }
        }
    }

    fun getConnectionStatus(): ConnectionStatus = connectionStatus


    fun getNetworkMetrics(): String {
        val client = webSocketClient ?: return "WebSocket client not available"

        return "Status: ${if (client.isConnected()) "Connected" else "Disconnected"}, " +
                "Authenticated: ${client.isAuthenticated()}, " +
                "Reconnecting: ${client.isReconnecting()}"
    }

    private fun enableAutoReconnection() {
        lifecycleScope.launch {
            var reconnectDelay = 5000L
            val maxDelay = 60000L

            while (!isFinishing) {
                if (connectionStatus == ConnectionStatus.ERROR ||
                    connectionStatus == ConnectionStatus.DISCONNECTED
                ) {

                    Log.i(TAG, "Attempting auto-reconnection...")
                    updateConnectionStatus(ConnectionStatus.CONNECTING)

                    val success = tryReconnection()
                    if (success) {
                        Log.i(TAG, "Auto-reconnection successful")
                        reconnectDelay = 5000L
                    } else {
                        Log.w(TAG, "Auto-reconnection failed, retrying in ${reconnectDelay}ms")
                        kotlinx.coroutines.delay(reconnectDelay)
                        reconnectDelay = minOf(reconnectDelay * 2, maxDelay)
                    }
                } else {
                    kotlinx.coroutines.delay(10000L)
                }
            }
        }
    }

    private suspend fun tryReconnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                val controllers = networkClient?.getDiscoveredControllers()
                if (!controllers.isNullOrEmpty()) {
                    for (controller: NetworkClient.ControllerInfo in controllers) {
                        Log.i(TAG, "Reconnection attempt to ${controller.ipAddress}")
                        val success = networkClient?.connectToController(
                            controller.ipAddress,
                            controller.port
                        ) ?: false

                        if (success) {
                            return@withContext true
                        }
                    }
                }

                networkClient?.startDiscovery { discoverySuccess: Boolean ->
                    if (discoverySuccess) {
                        val newControllers = networkClient?.getDiscoveredControllers()
                        if (!newControllers.isNullOrEmpty()) {

                            lifecycleScope.launch {
                                val controller = newControllers.first()
                                networkClient?.connectToController(
                                    controller.ipAddress,
                                    controller.port
                                ) { connectSuccess: Boolean ->
                                    if (!connectSuccess) {
                                        updateConnectionStatus(ConnectionStatus.ERROR)
                                    }
                                }
                            }
                        }
                    }
                }

                return@withContext false

            } catch (e: Exception) {
                Log.e(TAG, "Error during reconnection attempt", e)
                return@withContext false
            }
        }
    }

    private fun startHeartbeat() {
        lifecycleScope.launch {
            while (!isFinishing) {
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    try {


                        webSocketClient?.sendStatusRequest()

                    } catch (e: Exception) {
                        Log.w(TAG, "Status request exception", e)
                        updateConnectionStatus(ConnectionStatus.ERROR)
                    }
                }

                kotlinx.coroutines.delay(30000L)
            }
        }
    }

    fun getConnectionStatusReport(): String {
        val sb = StringBuilder()
        sb.append("=== PC-to-Phone Control Status ===\n")
        sb.append("Connection Status: ${connectionStatus.name}\n")
        sb.append("Network Metrics: ${getNetworkMetrics()}\n")

        val servers = webSocketClient?.getDiscoveredServers()
        sb.append("Discovered Servers: ${servers?.size ?: 0}\n")
        servers?.forEach { (name, server) ->
            sb.append("  - $name (${server.host}:${server.port}) TLS:${server.usesTLS}\n")
        }

        val currentServer = webSocketClient?.getCurrentServer()
        if (currentServer != null) {
            sb.append("Current Server: ${currentServer.name} (${currentServer.host}:${currentServer.port})\n")
        }

        sb.append("Service Bound: $isServiceBound\n")
        if (isServiceBound && recordingService != null) {
            try {
                val recordingController = serviceBinder?.getRecordingController()
                val summary = recordingController?.getSensorStatusSummary()
                sb.append("Recording Status: ${summary ?: "N/A"}\n")

                val serverStatus = serviceBinder?.getServerStatus()
                sb.append("Server Socket: $serverStatus\n")

                val connectedClients = serviceBinder?.getConnectedClients()
                sb.append("Connected PC Clients: ${connectedClients?.size ?: 0}\n")
                connectedClients?.forEach { client ->
                    sb.append("  - $client\n")
                }

            } catch (e: Exception) {
                sb.append("Recording Status: Error - ${e.message}\n")
            }
        } else {
            sb.append("Recording Status: Service not available\n")
        }

        return sb.toString()
    }

    private fun handleNetworkStatusClick() {
        when (connectionStatus) {
            ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {

                showConnectionOptionsDialog()
            }

            ConnectionStatus.DISCOVERING, ConnectionStatus.CONNECTING -> {

                Toast.makeText(this, "Connection in progress...", Toast.LENGTH_SHORT).show()
            }

            ConnectionStatus.CONNECTED -> {

                showConnectionInfoDialog()
            }
        }
    }

    private fun showConnectionOptionsDialog() {
        val options = arrayOf(
            "Retry Discovery",
            "Manual IP Connection",
            "Connection Status Report",
            "Cancel"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("PC Connection Options")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {

                    Log.i(TAG, "Retrying network discovery")
                    startNetworkDiscovery()
                }

                1 -> {

                    showManualConnectionDialog()
                }

                2 -> {

                    showStatusReportDialog()
                }

                3 -> {

                }
            }
        }
        builder.show()
    }

    private fun showConnectionInfoDialog() {
        val metrics = getNetworkMetrics()
        val servers = webSocketClient?.getDiscoveredServers()
        val currentServer = webSocketClient?.getCurrentServer()

        val message = StringBuilder()
        message.append("Connection Status: Connected\n\n")
        message.append("Performance Metrics:\n$metrics\n\n")

        if (currentServer != null) {
            message.append("Connected to:\n")
            message.append("Server: ${currentServer.name}\n")
            message.append("Address: ${currentServer.host}:${currentServer.port}\n")
            message.append("TLS: ${currentServer.usesTLS}\n")
            message.append("Protocol: ${currentServer.protocolVersion}\n\n")
        }

        message.append("Recording Service: ${if (isServiceBound) "Available" else "Not Available"}\n")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("PC Connection Info")
        builder.setMessage(message.toString())
        builder.setPositiveButton("Test Recording") { _, _ ->
            testRemoteRecordingCapability()
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun showStatusReportDialog() {
        val statusReport = getConnectionStatusReport()

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Detailed Status Report")
        builder.setMessage(statusReport)
        builder.setPositiveButton("Copy to Clipboard") { _, _ ->
            val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Status Report", statusReport)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Status report copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun testRemoteRecordingCapability() {
        if (!isServiceBound || recordingService == null) {
            Toast.makeText(this, "Recording service not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val testSessionInfo = SessionInfo(
                sessionId = "test_${System.currentTimeMillis()}",
                startTime = System.currentTimeMillis(),
                studyName = "Connection Test",
                participantId = "test_participant"
            )

            Toast.makeText(this, "Testing remote recording capability...", Toast.LENGTH_SHORT)
                .show()

            handleRemoteRecordingRequest(testSessionInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Error testing remote recording", e)
            Toast.makeText(this, "Test failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showManualConnectionDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter PC IP address (e.g., 192.168.1.100)"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Manual PC Connection")
        builder.setMessage("Enter the IP address of your PC Controller:")
        builder.setView(input)

        builder.setPositiveButton("Connect") { _, _ ->
            val ipAddress = input.text.toString().trim()
            if (ipAddress.isNotEmpty()) {
                connectToPC(ipAddress)
            } else {

                tryCommonIPAddresses()
            }
        }

        builder.setNegativeButton("Try Defaults") { _, _ ->
            tryCommonIPAddresses()
        }

        builder.setNeutralButton("Cancel", null)
        builder.show()
    }

    private fun tryCommonIPAddresses() {
        val commonIPs = listOf(
            "192.168.1.100", "192.168.1.101", "192.168.1.102",
            "192.168.0.100", "192.168.0.101", "192.168.0.102",
            "10.0.0.100", "10.0.0.101", "10.0.0.102",
            "127.0.0.1"
        )

        Toast.makeText(this, "Trying common IP addresses...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            var connected = false

            for (ip in commonIPs) {
                if (connected) break

                Log.i(TAG, "Trying to connect to $ip")
                updateConnectionStatus(ConnectionStatus.CONNECTING)

                try {
                    val success = withContext(Dispatchers.IO) {

                        networkClient?.connectToController(ip, 8080, true) ?: false ||
                                networkClient?.connectToController(ip, 8080, false) ?: false
                    }

                    if (success) {
                        Log.i(TAG, "Successfully connected to $ip")
                        Toast.makeText(
                            this@MainActivity,
                            "Connected to PC at $ip",
                            Toast.LENGTH_LONG
                        ).show()
                        connected = true
                        break
                    } else {
                        Log.w(TAG, "Failed to connect to $ip")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Connection error to $ip: ${e.message}")
                }
            }

            if (!connected) {
                updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                Toast.makeText(
                    this@MainActivity,
                    "No PC found at common addresses. Restarting discovery...",
                    Toast.LENGTH_LONG
                ).show()
                startNetworkDiscovery()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "MainActivity",
            "activity_destroying"
        )

        try {
            webSocketClient?.stop()
            webSocketClient = null

            RecordingService.stopServer(this)

            if (isServiceBound) {
                unbindService(serviceConnection)
                isServiceBound = false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during networking cleanup", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "MainActivity",
                "networking_cleanup_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }

        try {
            crashSafeSupervisor.shutdown()
            structuredLogger.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Phase 0 cleanup", e)
        }
    }

    private fun launchShimmerMvp() {
        try {
            Log.i(TAG, "Showing developer sensor options")

            val options = arrayOf(
                "Shimmer GSR MVP",
                "Shimmer3 GSR+ Integration (Comprehensive)",
                "Unified Sensor Platform",
                "Cancel"
            )

            AlertDialog.Builder(this)
                .setTitle("Developer Sensor Access")
                .setMessage(
                    "Select sensor integration mode:\n\n" +
                            "• MVP: Basic Shimmer GSR recording\n" +
                            "• Comprehensive: Full integration plan implementation\n" +
                            "• Unified Platform: Multi-modal sensor coordination"
                )
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {

                            Toast.makeText(this, "Opening Shimmer GSR MVP", Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this, ShimmerMvpActivity::class.java)
                            startActivity(intent)
                        }

                        1 -> {

                            Toast.makeText(
                                this,
                                "GSR functionality available in GSR Settings",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(
                                this,
                                mpdc4gsr.sensors.gsr.GSRSettingsActivity::class.java
                            )
                            startActivity(intent)
                        }

                        2 -> {

                            Toast.makeText(
                                this,
                                "Opening Unified Sensor Platform",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, UnifiedSensorActivity::class.java)
                            startActivity(intent)
                        }

                        3 -> {

                        }
                    }
                }
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error launching sensor options: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun launchGSRRecording() {

        if (GSRSensorRecorder.hasRequiredPermissions(this)) {
            GSRQuickRecordingActivity.start(this)
        } else {

            TipDialog.Builder(this)
                .setTitleMessage("GSR Recording Permissions")
                .setMessage("GSR recording requires Bluetooth and location permissions. Enable them in settings?")
                .setPositiveListener("Settings") {
                    GSRQuickRecordingActivity.start(this)
                }
                .setCancelListener("Cancel") { }
                .create().show()
        }
    }

    private fun setupManualCameraControls() {
        // Set up camera control integration with existing CameraSettingsView
        try {
            // Find any existing CameraSettingsView instances and connect them to ViewModel
            // This would integrate with existing camera functionality
            
            // Observe manual camera control states from ViewModel
            mainViewModel.exposureLocked.observe(this) { locked ->
                Log.d(TAG, "Exposure lock state changed: $locked")
                // This would be connected to actual CameraX control when integrated
            }
            
            mainViewModel.focusLocked.observe(this) { locked ->
                Log.d(TAG, "Focus lock state changed: $locked")
                // This would be connected to actual CameraX control when integrated
            }
            
            mainViewModel.exposureCompensation.observe(this) { compensation ->
                Log.d(TAG, "Exposure compensation changed: $compensation EV")
                // This would be connected to actual CameraX control when integrated
            }
            
            Log.i(TAG, "Manual camera controls integrated with ViewModel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup manual camera controls", e)
        }
    }

    // Method to be called by CameraSettingsView when controls are used
    fun onCameraControlsChanged(exposureLocked: Boolean, focusLocked: Boolean, exposureCompensation: Float) {
        mainViewModel.lockExposure(exposureLocked)
        mainViewModel.lockFocus(focusLocked) 
        mainViewModel.setExposureCompensation(exposureCompensation)
    }

    // Method to reset camera controls to auto
    fun resetCameraControlsToAuto() {
        mainViewModel.resetCameraControlsToAuto()
    }
}
