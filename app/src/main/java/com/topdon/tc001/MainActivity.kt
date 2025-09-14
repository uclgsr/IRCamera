package com.topdon.tc001

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.AppUtils
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Note: SupHelp library integration is not included in this build configuration
import com.example.thermal_lite.activity.IRThermalLiteActivity
import com.csl.irCamera.R
import com.topdon.tc001.gsr.GSRQuickRecordingActivity
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
// Note: HIK thermal camera activity implementation is module-specific
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.TS004ResetEvent
import com.topdon.lib.core.bean.event.WinterClickEvent
import com.topdon.lib.core.bean.event.device.DevicePermissionEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.AppConfig
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.FirmwareUpDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipOtgDialog
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.lib.core.repository.GalleryRepository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.tools.ConstantLanguages
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.utils.PermissionUtils
import com.topdon.lib.core.viewmodel.VersionViewModel
import com.topdon.lms.sdk.LMS
import com.topdon.module.thermal.ir.activity.IRThermalNightActivity
import com.topdon.module.thermal.ir.activity.IRThermalPlusActivity
import com.topdon.module.thermal.ir.fragment.IRGalleryTabFragment
import com.topdon.module.user.fragment.MineFragment
import com.topdon.tc001.app.App
import com.topdon.tc001.fragment.MainFragment
import com.topdon.tc001.utils.AppVersionUtil
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.databinding.ActivityMainBinding
// Network integration imports - Enhanced JSON protocol
import com.topdon.tc001.network.NetworkController
import com.topdon.tc001.service.RecordingService
import com.topdon.tc001.controller.RecordingController
import com.topdon.gsr.model.SessionInfo
// Phase 0 baseline imports
import com.topdon.tc001.config.FeatureFlags
import com.topdon.tc001.viewmodel.MainActivityViewModel
import com.topdon.tc001.config.ProtocolVersion
import com.topdon.tc001.logging.StructuredLogger
import com.topdon.tc001.supervisor.CrashSafeSupervisor
// Zoho dependencies commented out - not available in build
// import com.zoho.commons.LauncherModes
// import com.zoho.commons.LauncherProperties
// import com.zoho.salesiqembed.ZohoSalesIQ
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

// Legacy ARouter route annotation - now using NavigationManager
class MainActivity : BaseBindingActivity<ActivityMainBinding>(), View.OnClickListener {
    private val versionViewModel: VersionViewModel by viewModels()
    private val mainViewModel: MainActivityViewModel by viewModels()

    private var checkPermissionType: Int = -1 // 0 initData数据 1 图库  2 connect方法
    
    // PC-to-phone control networking - JSON protocol implementation
    // NetworkController is managed by MainActivityViewModel
    private var recordingService: RecordingService? = null
    private var isServiceBound = false
    
    // UI elements for network status
    private var networkStatusIndicator: ImageView? = null
    private var networkStatusText: TextView? = null
    
    // UI elements for GSR status
    private var gsrStatusIndicator: ImageView? = null
    private var gsrStatusText: TextView? = null
    
    // UI elements for session control
    private var sessionControlButton: Button? = null
    
    // Phase 0 baseline components
    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            isServiceBound = true
            Log.i(TAG, "Recording service connected")
            
            // Now that we have the service, we can set up remote control
            setupRemoteControl()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
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
            // Enable automatic reconnection with exponential backoff
            enableAutoReconnection()
            
            // Start connection health monitoring
            startHeartbeat()
            
            // Set up UI click handlers for network status
            networkStatusIndicator?.setOnClickListener { handleNetworkStatusClick() }
            networkStatusText?.setOnClickListener { handleNetworkStatusClick() }
            
            Log.i(TAG, "Enhanced remote control setup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up remote control", e)
            showNetworkError("Failed to setup remote control: ${e.message}")
        }
    }
    
    override fun initContentLayoutId(): Int = R.layout.activity_main
    
    companion object {
        private const val TAG = "MainActivity"
    }

    // 记录设备信息
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
        
        // Phase 0 - Initialize baseline components first
        initializePhase0Baseline()
        
        initView()
        initData()
        
        // Initialize ViewModel and set up observers
        setupViewModelObservers()
        mainViewModel.initializeComponents()
        
        // Initialize PC-to-phone control networking via ViewModel
        initNetworkingViaViewModel()
    }

    private fun initView() {
        // Check if clause needs to be shown (moved from SplashActivity)
        if (!SharedManager.getHasShowClause()) {
            NavigationManager.build(RouterConfig.CLAUSE).navigation(this)
            finish()
            return
        }

        logInfo()
        
        // Initialize status UI elements
        networkStatusIndicator = binding.networkStatusIndicator
        networkStatusText = binding.networkStatusText
        gsrStatusIndicator = binding.gsrStatusIndicator
        gsrStatusText = binding.gsrStatusText
        sessionControlButton = binding.sessionControlButton
        
        lifecycleScope.launch(Dispatchers.IO) {
            // Note: SupHelp AI upscaler integration is not included in this build
            // SupHelp.getInstance().initAiUpScaler(Utils.getApp())
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
        
        // Add click listeners for status and control elements
        binding.networkStatusBar.setOnClickListener {
            handleNetworkStatusClick()
        }
        
        gsrStatusIndicator?.setOnClickListener {
            handleGSRStatusClick()
        }
        
        sessionControlButton?.setOnClickListener {
            handleSessionControlClick()
        }
        
        App.instance.initWebSocket()
        copyFile("SR.pb", File(filesDir, "SR.pb"))
        BaseApplication.instance.clearDb()
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(true)
        } else {
            versionViewModel.checkVersion()
        }

        if (!SharedManager.hasTcLine && !SharedManager.hasTS004 && !SharedManager.hasTC007) {
            // 仅当设备列表为空时，才执行自动跳转
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
        if (WebSocketProxy.getInstance().isTS004Connect()) {
            SharedManager.hasTS004 = true
        }
        if (WebSocketProxy.getInstance().isTC007Connect()) {
            SharedManager.hasTC007 = true
        }
//        initLauncher()
    }

    override fun onStart() {
        super.onStart()

        // 版本下载
        versionViewModel.updateLiveData.observe(this) {
            FirmwareUpDialog(this).apply {
                titleStr = getString(com.topdon.lib.core.R.string.update_new_version)
                sizeStr = it.versionNo
                contentStr = it.description
                isShowCancel = !it.isForcedUpgrade
                onConfirmClickListener = {
                    updateApk(it.downPageUrl)
                }
                onCancelClickListener = {
                    SharedManager.setVersionCheckDate(System.currentTimeMillis()) // 刷新版本提示时间
                }
            }.show()
        }
    }

    private fun updateApk(url: String) {
        if (applicationInfo.targetSdkVersion < Build.VERSION_CODES.P) {
            // 目标版本27默认跳到官网下载
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
        if (targetFile.exists()) { // 已存在就不覆盖了
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

    private fun initData() {
        checkPermissionType = 0
        checkCameraPermission()
        
        // Log that PC-to-Phone communication support is available
        Log.i("MainActivity", "✅ PC-to-Phone communication integration available - RecordingService supports network control")
    }

    override fun onResume() {
        super.onResume()
        LMS.getInstance().language = ConstantLanguages.ENGLISH
//        DeviceTools.isConnect(true)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clIconGallery -> { // 图库
                checkPermissionType = 1
                checkStoragePermission()
            }
            binding.viewMain -> { // 首页
                binding.viewPage.setCurrentItem(1, false)
            }
            binding.clIconMine -> { // 我的
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
            0 -> { // 图库
                binding.ivIconGallery.isSelected = true
                binding.tvIconGallery.isSelected = true
            }
            1 -> {
                binding.ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_select)
            }
            2 -> { // 我的
                binding.ivIconMine.isSelected = true
                binding.tvIconMine.isSelected = true
            }
        }
    }

    override fun connected() {
        if (SharedManager.isConnectAutoOpen) {
            checkPermissionType = 2
            checkCameraPermission()
        }
    }

    private var tipOtgDialog: TipOtgDialog? = null

    override fun disConnected() {
        if (WebSocketProxy.getInstance().isTS004Connect()) {
            NavigationManager.build(RouterConfig.IR_MONOCULAR).navigation(this)
        }
        // 无连接OTG提示
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
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) && isTS004) { // TC007不用
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
                                it.putInt(ExtraKeyConfig.DIR_TYPE, GalleryRepository.DirType.LINE.ordinal)
                            }
                    }
                }
                1 -> MainFragment()
                else -> MineFragment()
            }
        }
    }


    private fun getNeedPermissionList(): SparseArray<List<String>> {
        val sparseArray = SparseArray<List<String>>()
        sparseArray.append(R.string.permission_request_camera_app, listOf(Manifest.permission.CAMERA))
        (
            if (this.applicationInfo.targetSdkVersion >= 34) {
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else if (this.applicationInfo.targetSdkVersion == 33) {
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else {
                listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
            }
        ).let {
            sparseArray.append(R.string.permission_request_storage_app, it)
        }
        return sparseArray
    }

    private fun checkCameraPermission() {
        if (!PermissionUtils.isVisualUser() &&
            !XXPermissions.isGranted(
                this,
                getNeedPermissionList()[R.string.permission_request_camera_app],
            )
        ) {
            if (BaseApplication.instance.isDomestic()) {
                if (SharedManager.getMainPermissionsState()) {
                    // 国内版拒绝授权之后就别再授权了华为上架不通过
                    return
                }
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_camera_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initCameraPermission()
                    }
                    .create().show()
            } else {
                initCameraPermission()
            }
        } else {
            initCameraPermission()
        }
    }


    private fun initCameraPermission() {
        XXPermissions.with(this)
            .permission(getNeedPermissionList()[R.string.permission_request_camera_app])
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        if (allGranted) {
                            checkStoragePermission()
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (BaseApplication.instance.isDomestic()) {
                            SharedManager.setMainPermissionsState(true)
                        }
                        if (doNotAskAgain) {
                            // 拒绝授权并且不再提醒
                            TipDialog.Builder(this@MainActivity)
                                .setTitleMessage(getString(R.string.app_tip))
                                .setMessage(
                                    if (PermissionUtils.hasCameraPermission()) {
                                        getString(R.string.app_album_content)
                                    } else {
                                        getString(R.string.app_camera_content)
                                    },
                                )
                                .setPositiveListener(R.string.app_open) {
                                    AppUtils.launchAppDetailsSettings()
                                }
                                .setCancelListener(R.string.app_cancel) {
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                    }
                },
            )
    }

    private fun checkStoragePermission() {
        if (!XXPermissions.isGranted(this, getNeedPermissionList()[R.string.permission_request_storage_app])) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(getString(R.string.permission_request_storage_app, CommUtils.getAppName()))
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        initStoragePermission()
                    }
                    .create().show()
            } else {
                initStoragePermission()
            }
        } else {
            initStoragePermission()
        }
    }


    private fun initStoragePermission() {
        if (PermissionUtils.isVisualUser()) {
            jumpIRActivity()
            return
        }
        XXPermissions.with(this)
            .permission(
                getNeedPermissionList()[R.string.permission_request_storage_app],
            )
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        if (allGranted) {
                            jumpIRActivity()
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (doNotAskAgain) {
                            // 拒绝授权并且不再提醒
                            TipDialog.Builder(this@MainActivity)
                                .setTitleMessage(getString(R.string.app_tip))
                                .setMessage(getString(R.string.app_album_content))
                                .setPositiveListener(R.string.app_open) {
                                    AppUtils.launchAppDetailsSettings()
                                }
                                .setCancelListener(R.string.app_cancel) {
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                    }
                },
            )
    }

    fun jumpIRActivity() {
        when (checkPermissionType) {
            0 -> {
                DeviceTools.isConnect(isSendConnectEvent = true)
            }
            1 -> {
                binding.viewPage.setCurrentItem(0, false)
            }
            2 -> {
                if (DeviceTools.isTC001PlusConnect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    // Fixed: Replaced deprecated startActivityForResult with startActivity as no result processing is needed
                    startActivity(Intent(this@MainActivity, IRThermalPlusActivity::class.java))
                } else if (DeviceTools.isTC001LiteConnect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalLiteActivity::class.java))
                } else if (DeviceTools.isHikConnect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    // Note: Using IRThermalNightActivity as fallback for HIK thermal devices
                    startActivity(Intent(this@MainActivity, IRThermalNightActivity::class.java))
                } else {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivity(Intent(this@MainActivity, IRThermalNightActivity::class.java))
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
    
    // ==================== PHASE 0 BASELINE & GUARDRAILS ====================
    
    private fun initializePhase0Baseline() {
        Log.i(TAG, "Initializing Phase 0 baseline components")
        
        try {
            // 1. Initialize feature flags with defaults
            FeatureFlags.initialize(this)
            
            // 2. Initialize structured logging system
            structuredLogger = StructuredLogger.getInstance(this)
            
            // 3. Initialize crash-safe supervisor
            crashSafeSupervisor = CrashSafeSupervisor.getInstance(this)
            crashSafeSupervisor.initialize()
            
            // 4. Validate configuration
            val configWarnings = FeatureFlags.validateConfiguration()
            if (configWarnings.isNotEmpty()) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.WARNING,
                    "MainActivity",
                    "configuration_warnings",
                    mapOf("warnings" to configWarnings.joinToString("; "))
                )
            }
            
            // 5. Log protocol information
            val protocolInfo = ProtocolVersion.getProtocolInfo()
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "MainActivity",
                "protocol_version_info",
                protocolInfo
            )
            
            // 6. Log feature flags
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
            // Continue with app startup even if Phase 0 fails
        }
    }
    
    // ==================== PC-to-Phone Control Networking ====================
    

    private fun initNetworkingViaViewModel() {
        Log.i(TAG, "Integrating PC-to-phone control via ViewModel NetworkController")
        
        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "MainActivity",
            "networking_integration_started",
            mapOf(
                "feature_flags" to FeatureFlags.getAllFlags(),
                "protocol_version" to ProtocolVersion.CURRENT_VERSION
            )
        )
        
        try {
            // NetworkController is initialized by MainActivityViewModel
            // Set up additional observers for MainActivity integration
            
            // Network controller status is already handled by setupViewModelObservers()
            // No additional setup needed as ViewModel observers handle UI updates
            
            // Observe session state changes for PC remote control commands
            mainViewModel.sessionState.observe(this) { state ->
                when (state) {
                    MainActivityViewModel.SessionState.RECORDING -> {
                        // Update UI to reflect recording state
                        updateSessionStatusUI(state)
                    }
                    MainActivityViewModel.SessionState.IDLE -> {
                        // Update UI to reflect idle state
                        updateSessionStatusUI(state)
                    }
                    else -> {
                        // Handle other states
                        updateSessionStatusUI(state)
                    }
                }
            }
            
            // Bind to recording service for remote control capability
            bindRecordingService()
            
            // Start recording service with server socket automatically
            RecordingService.startServer(this)
            
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "MainActivity",
                "networking_integration_completed"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to integrate networking via ViewModel", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "MainActivity",
                "networking_integration_failed",
                mapOf("error" to e.message)
            )
            showNetworkError("Network integration failed: ${e.message}")
        }
    }
    
    /**
     * Bind to recording service for remote control capability
     */
    private fun bindRecordingService() {
        val intent = Intent(this, RecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * Show network error dialog with options
     */
    private fun showNetworkError(message: String) {
        Log.e(TAG, "Network error: $message")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Network Error")
        builder.setMessage("$message\n\nWhat would you like to do?")
        
        builder.setPositiveButton("Retry Discovery") { _, _ ->
            mainViewModel.startNetworkDiscovery()
        }
        
        builder.setNegativeButton("Ignore") { _, _ ->
            // Just dismiss - user can try again later
        }
        
        builder.show()
    }
    

    
    // Note: WebSocket client methods have been replaced by NetworkController in MainActivityViewModel
    // The JSON-based PC remote control is now handled via the ViewModel pattern
    
    /**
     * Set up observers for MainActivityViewModel to update UI based on state changes
     */
    private fun setupViewModelObservers() {
        // GSR Connection State Observer
        mainViewModel.gsrConnectionState.observe(this) { state ->
            updateGSRStatusUI(state)
        }
        
        // Network Connection State Observer
        mainViewModel.networkConnectionState.observe(this) { state ->
            updateNetworkStatusUI(state)
        }
        
        // Session State Observer
        mainViewModel.sessionState.observe(this) { state ->
            updateSessionStatusUI(state)
        }
        
        // Status Message Observer
        mainViewModel.statusMessage.observe(this) { message ->
            message?.let {
                showStatusMessage(it)
                mainViewModel.clearStatusMessage()
            }
        }
        
        // Connected Controller Info Observer
        mainViewModel.connectedControllerInfo.observe(this) { controller ->
            updateNetworkConnectionInfo(controller)
        }
        
        // Current Session Observer
        mainViewModel.currentSession.observe(this) { session ->
            updateCurrentSessionInfo(session)
        }
    }
    
    /**
     * Update GSR sensor status in the UI
     */
    private fun updateGSRStatusUI(state: MainActivityViewModel.GSRConnectionState) {
        runOnUiThread {
            gsrStatusText?.text = when (state) {
                MainActivityViewModel.GSRConnectionState.DISCONNECTED -> "GSR: Disconnected"
                MainActivityViewModel.GSRConnectionState.DISCOVERING -> "GSR: Searching..."
                MainActivityViewModel.GSRConnectionState.CONNECTING -> "GSR: Connecting..."
                MainActivityViewModel.GSRConnectionState.CONNECTED -> "GSR: Connected"
                MainActivityViewModel.GSRConnectionState.ERROR -> "GSR: Error"
            }
            
            // Update indicator color
            gsrStatusIndicator?.setImageResource(when (state) {
                MainActivityViewModel.GSRConnectionState.CONNECTED -> android.R.drawable.presence_online
                MainActivityViewModel.GSRConnectionState.CONNECTING,
                MainActivityViewModel.GSRConnectionState.DISCOVERING -> android.R.drawable.presence_away
                MainActivityViewModel.GSRConnectionState.ERROR -> android.R.drawable.presence_busy
                MainActivityViewModel.GSRConnectionState.DISCONNECTED -> android.R.drawable.presence_invisible
            })
        }
    }
    
    /**
     * Update network connection status in the UI
     */
    private fun updateNetworkStatusUI(state: MainActivityViewModel.NetworkConnectionState) {
        runOnUiThread {
            networkStatusText?.text = when (state) {
                MainActivityViewModel.NetworkConnectionState.DISCONNECTED -> "PC: Disconnected"
                MainActivityViewModel.NetworkConnectionState.DISCOVERING -> "PC: Searching..."
                MainActivityViewModel.NetworkConnectionState.CONNECTING -> "PC: Connecting..."
                MainActivityViewModel.NetworkConnectionState.CONNECTED -> "PC: Connected"
                MainActivityViewModel.NetworkConnectionState.ERROR -> "PC: Error"
            }
            
            // Update indicator color
            networkStatusIndicator?.setImageResource(when (state) {
                MainActivityViewModel.NetworkConnectionState.CONNECTED -> android.R.drawable.presence_online
                MainActivityViewModel.NetworkConnectionState.CONNECTING,
                MainActivityViewModel.NetworkConnectionState.DISCOVERING -> android.R.drawable.presence_away
                MainActivityViewModel.NetworkConnectionState.ERROR -> android.R.drawable.presence_busy
                MainActivityViewModel.NetworkConnectionState.DISCONNECTED -> android.R.drawable.presence_invisible
            })
        }
    }
    
    /**
     * Update session status in the UI
     */
    private fun updateSessionStatusUI(state: MainActivityViewModel.SessionState) {
        runOnUiThread {
            sessionControlButton?.text = when (state) {
                MainActivityViewModel.SessionState.IDLE -> "Start Recording"
                MainActivityViewModel.SessionState.STARTING -> "Starting..."
                MainActivityViewModel.SessionState.RECORDING -> "Stop Recording"
                MainActivityViewModel.SessionState.STOPPING -> "Stopping..."
                MainActivityViewModel.SessionState.PAUSED -> "Resume Recording"
                MainActivityViewModel.SessionState.ERROR -> "Session Error"
            }
            
            // Enable/disable button based on state
            sessionControlButton?.isEnabled = when (state) {
                MainActivityViewModel.SessionState.STARTING,
                MainActivityViewModel.SessionState.STOPPING -> false
                else -> true
            }
            
            Log.d(TAG, "Session Status: $state")
        }
    }
    
    /**
     * Show status message to user (Toast for now, can be enhanced with custom UI)
     */
    private fun showStatusMessage(message: MainActivityViewModel.StatusMessage) {
        runOnUiThread {
            val context = when (message.level) {
                MainActivityViewModel.StatusMessage.Level.INFO -> this
                MainActivityViewModel.StatusMessage.Level.WARNING -> this
                MainActivityViewModel.StatusMessage.Level.ERROR -> this
            }
            Toast.makeText(context, message.message, Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Status: ${message.level.name} - ${message.message}")
        }
    }
    
    /**
     * Update network connection information display
     */
    private fun updateNetworkConnectionInfo(controller: NetworkClient.ControllerInfo?) {
        runOnUiThread {
            if (controller != null) {
                networkStatusText?.text = "PC: ${controller.deviceName}"
                Log.i(TAG, "Connected to PC controller: ${controller.deviceName} (${controller.ipAddress})")
            }
        }
    }
    
    /**
     * Update current session information display
     */
    private fun updateCurrentSessionInfo(session: SessionInfo?) {
        runOnUiThread {
            if (session != null) {
                Log.i(TAG, "Current session: ${session.sessionId}")
                // TODO: Update session UI elements
            } else {
                Log.i(TAG, "No active session")
            }
        }
    }
    
    /**
     * Enhanced network status click handler - integrates with ViewModel
     */
    private fun handleNetworkStatusClick() {
        when (mainViewModel.networkConnectionState.value) {
            MainActivityViewModel.NetworkConnectionState.DISCONNECTED -> {
                // Start network discovery
                mainViewModel.startNetworkDiscovery()
            }
            MainActivityViewModel.NetworkConnectionState.CONNECTED -> {
                // Show connection info or disconnect option
                mainViewModel.connectedControllerInfo.value?.let { controller ->
                    Toast.makeText(this, "Connected to: ${controller.deviceName}\n${controller.ipAddress}:${controller.port}", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                // Show current status
                Toast.makeText(this, "Network status: ${mainViewModel.networkConnectionState.value}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    /**
     * Handle GSR status click - start connection or show details
     */
    private fun handleGSRStatusClick() {
        when (mainViewModel.gsrConnectionState.value) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> {
                // Start GSR connection
                mainViewModel.startGSRConnection()
            }
            MainActivityViewModel.GSRConnectionState.CONNECTED -> {
                // Show GSR sensor details
                Toast.makeText(this, "GSR sensor connected and streaming", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Show current status
                Toast.makeText(this, "GSR status: ${mainViewModel.gsrConnectionState.value}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Handle session control button click - start/stop recording
     */
    private fun handleSessionControlClick() {
        when (mainViewModel.sessionState.value) {
            MainActivityViewModel.SessionState.IDLE -> {
                // Start recording session
                val config = MainActivityViewModel.SessionConfig(
                    modalities = listOf("thermal", "GSR"),
                    saveImages = true
                )
                mainViewModel.startRecordingSession(config)
                
                // Set up thermal camera integration after session starts
                setupThermalCameraIntegration()
            }
            MainActivityViewModel.SessionState.RECORDING -> {
                // Stop recording session
                mainViewModel.stopRecordingSession()
            }
            MainActivityViewModel.SessionState.PAUSED -> {
                // Resume recording (TODO: implement pause/resume functionality)
                Toast.makeText(this, "Resume functionality coming soon", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Show current status
                Toast.makeText(this, "Session status: ${mainViewModel.sessionState.value}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Set up thermal camera integration to work with our ThermalRecorder
     * This method should be called when a recording session starts to hook into the existing camera system
     */
    private fun setupThermalCameraIntegration() {
        // This is a placeholder for integration with the existing thermal camera system
        // In a real implementation, this would hook into the UVCCamera callbacks
        // and forward frame data to our ThermalRecorder
        
        Log.i(TAG, "Setting up thermal camera integration for multi-modal recording")
        
        // TODO: Hook into existing UVCCamera system to get frame callbacks
        // For now, we'll create a simulation for testing purposes
        lifecycleScope.launch {
            delay(1000) // Simulate camera startup delay
            simulateThermalFrames()
        }
    }
    
    /**
     * Simulate thermal frames for testing (replace with real camera integration)
     */
    private fun simulateThermalFrames() {
        if (mainViewModel.sessionState.value != MainActivityViewModel.SessionState.RECORDING) {
            return
        }
        
        lifecycleScope.launch {
            repeat(10) { frameIndex ->
                if (mainViewModel.sessionState.value == MainActivityViewModel.SessionState.RECORDING) {
                    // Simulate thermal frame data
                    val width = 256
                    val height = 192
                    val frameData = ByteArray(width * height)
                    
                    // Generate fake temperature data (simulate room temperature around 20-25°C)
                    for (i in frameData.indices) {
                        val temp = 20 + (Math.random() * 5).toFloat() // 20-25°C range
                        frameData[i] = temp.toInt().toByte()
                    }
                    
                    // Process frame through our ThermalRecorder
                    // Note: This is a simplified simulation - real implementation would get actual thermal data
                    mainViewModel.processThermalFrame(frameData, width, height, 20f, 30f)
                    
                    delay(111) // ~9 FPS (1000ms / 9 ≈ 111ms)
                } else {
                    break
                }
            }
        }
    }

    /**
     * Enhanced network status click handler - integrates with ViewModel
     */
    private fun handleNetworkStatusClick() {
        when (mainViewModel.networkConnectionState.value) {
            MainActivityViewModel.NetworkConnectionState.DISCONNECTED,
            MainActivityViewModel.NetworkConnectionState.ERROR -> {
                // Start network discovery
                mainViewModel.startNetworkDiscovery()
            }
            MainActivityViewModel.NetworkConnectionState.DISCOVERING,
            MainActivityViewModel.NetworkConnectionState.CONNECTING -> {
                // Show discovery progress
                Toast.makeText(this, "Connection in progress...", Toast.LENGTH_SHORT).show()
            }
            MainActivityViewModel.NetworkConnectionState.CONNECTED -> {
                // Show connection info
                showConnectionInfoDialog()
            }
            else -> {
                // Show current status
                Toast.makeText(this, "Network status: ${mainViewModel.networkConnectionState.value}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Show connection information from ViewModel state
     */
    private fun showConnectionInfoDialog() {
        val connectionState = mainViewModel.networkConnectionState.value
        val controllerInfo = mainViewModel.connectedControllerInfo.value
        
        val message = StringBuilder()
        message.append("Connection Status: ${connectionState?.name ?: "Unknown"}\n\n")
        
        if (controllerInfo != null) {
            message.append("Connected to:\n")
            message.append("Device: ${controllerInfo.deviceName}\n")
            message.append("Address: ${controllerInfo.ipAddress}:${controllerInfo.port}\n\n")
        }
        
        message.append("Recording Service: ${if (isServiceBound) "Available" else "Not Available"}\n")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("PC Connection Info")
        builder.setMessage(message.toString())
        builder.setPositiveButton("Start Test Recording") { _, _ ->
            // Test recording capability via ViewModel
            val config = MainActivityViewModel.SessionConfig(
                modalities = listOf("thermal", "GSR"),
                saveImages = true
            )
            mainViewModel.startRecordingSession(config)
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "MainActivity",
            "activity_destroying"
        )
        
        // Cleanup NetworkController via ViewModel
        try {
            // NetworkController cleanup is handled by MainActivityViewModel.onCleared()
            
            // Stop server socket via RecordingService
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
                mapOf("error" to e.message)
            )
        }
        
        // Cleanup Phase 0 components
        try {
            crashSafeSupervisor.shutdown()
            structuredLogger.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Phase 0 cleanup", e)
        }
    }


    fun launchGSRRecording() {
        // Check GSR permissions first
        if (GSRSensorRecorder.hasRequiredPermissions(this)) {
            GSRQuickRecordingActivity.start(this)
        } else {
            // Show permission explanation and launch settings if needed
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
}
