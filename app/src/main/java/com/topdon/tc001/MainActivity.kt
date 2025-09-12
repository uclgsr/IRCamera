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
// Network integration imports - Phase 1 WebSocket client
import com.topdon.tc001.network.WebSocketClient
import com.topdon.tc001.service.RecordingService
import com.topdon.tc001.controller.RecordingController
import com.topdon.gsr.model.SessionInfo
// Phase 0 baseline imports
import com.topdon.tc001.config.FeatureFlags
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

    private var checkPermissionType: Int = -1 // 0 initData数据 1 图库  2 connect方法
    
    // PC-to-phone control networking - Phase 1 WebSocket implementation
    private var webSocketClient: WebSocketClient? = null
    private var recordingService: RecordingService? = null
    private var isServiceBound = false
    private var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
    
    // UI elements for network status
    private var networkStatusIndicator: ImageView? = null
    private var networkStatusText: TextView? = null
    
    // Phase 0 baseline components
    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor
    
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
    
    /**
     * Set up remote control with enhanced features
     */
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
        
        // Initialize PC-to-phone control networking
        initNetworking()
    }

    private fun initView() {
        // Check if clause needs to be shown (moved from SplashActivity)
        if (!SharedManager.getHasShowClause()) {
            NavigationManager.build(RouterConfig.CLAUSE).navigation(this)
            finish()
            return
        }

        logInfo()
        
        // Initialize network status UI elements
        networkStatusIndicator = binding.networkStatusIndicator
        networkStatusText = binding.networkStatusText
        
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
        
        // Add click listener for network status (for manual connection)
        binding.networkStatusBar.setOnClickListener {
            handleNetworkStatusClick()
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

    /**
     * 刷新 3 个 tab 的选中状态
     * @param index 当前选中哪个 tab，`[0, 2]`
     */
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

    /**
     * 权限检测
     * 因申请权限前需要弹窗提示用户，所以修改成key value形式
     * @return key：权限种类 value：具体权限
     */
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

    /**
     * 动态申请权限
     */
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

    /**
     * 动态申请权限
     */
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
    
    /**
     * Initialize Phase 0 baseline components: feature flags, structured logging, 
     * protocol versioning, and crash-safe supervision
     */
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
    
    /**
     * Initialize the PC-to-phone control networking system with Phase 0 supervision
     */
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
            // Initialize WebSocket client with supervision - Phase 1
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
            
            // Bind to recording service for remote control capability
            bindRecordingService()
            
            // Start recording service with server socket automatically
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
                mapOf("error" to e.message)
            )
            updateConnectionStatus(ConnectionStatus.ERROR)
            showNetworkError("Network initialization failed: ${e.message}")
        }
    }
    
    /**
     * Initialize WebSocket client under supervision - Phase 1 implementation
     */
    private suspend fun initializeWebSocketClientSupervised(stopToken: CrashSafeSupervisor.StopToken) {
        while (!stopToken.isStopRequested()) {
            try {
                // Initialize WebSocket client
                webSocketClient = WebSocketClient(this@MainActivity).apply {
                    // Set up event listener for WebSocket events
                    setEventListener(createWebSocketEventListener())
                    
                    // Start connection (includes discovery and auto-connect)
                    start()
                }
                
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "WebSocketClient",
                    "initialized_successfully"
                )
                
                // Wait for connection or stop signal
                while (!stopToken.isStopRequested() && webSocketClient?.isConnected() != true) {
                    kotlinx.coroutines.delay(1000)
                }
                
                if (webSocketClient?.isConnected() == true) {
                    updateConnectionStatus(ConnectionStatus.CONNECTED)
                }
                
                // Keep running while connected
                while (!stopToken.isStopRequested() && webSocketClient?.isConnected() == true) {
                    kotlinx.coroutines.delay(1000)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in WebSocket client supervision", e)
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "WebSocketClient",
                    "supervision_error",
                    mapOf("error" to e.message)
                )
                
                updateConnectionStatus(ConnectionStatus.ERROR)
                
                // Wait before retry
                kotlinx.coroutines.delay(5000)
            }
        }
        
        // Clean up on stop
        webSocketClient?.stop()
    }
                
                // Start automatic discovery and connection if enabled
                if (FeatureFlags.MDNS_ENABLE) {
                    startNetworkDiscovery()
                } else {
                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "NetworkClient",
                        "mdns_discovery_disabled"
                    )
                }
                
                // Wait for stop signal or connection status changes
                while (!stopToken.isStopRequested() && connectionStatus != ConnectionStatus.ERROR) {
                    kotlinx.coroutines.delay(1000)
                }
                
            } catch (e: Exception) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "NetworkClient",
                    "supervised_execution_error",
                    mapOf("error" to e.message)
                )
                
                if (!stopToken.isStopRequested()) {
                    kotlinx.coroutines.delay(10000) // Wait before retry
                }
            }
        }
        
        // Cleanup WebSocket client
        webSocketClient?.stop()
        webSocketClient = null
    }
    
    /**
     * Create network event listener to handle PC controller events with structured logging
     */
    /**
     * Create WebSocket event listener - Phase 1 implementation
     */
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
                    Toast.makeText(this@MainActivity, 
                        "Found PC Server: ${serverInfo.name}", 
                        Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity, 
                        "Connected to PC: ${serverInfo.name}", 
                        Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@MainActivity, 
                        "Authenticated with PC controller", 
                        Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity, 
                        "Disconnected from PC: $reason", 
                        Toast.LENGTH_SHORT).show()
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
                        // Handle other message types as needed
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
                // Update last heartbeat time for connection health monitoring
                structuredLogger.log(
                    StructuredLogger.LogLevel.DEBUG,
                    "WebSocketClient",
                    "heartbeat_received",
                    emptyMap()
                )
            }
        }
    }
    
    /**
     * Handle remote session start request - Phase 1
     */
    private fun handleRemoteSessionStart(sessionId: String) {
        try {
            structuredLogger.logSessionEvent(
                "remote_session_start",
                sessionId,
                emptyMap()
            )
            
            // Start recording through the service
            recordingService?.getRecordingController()?.startRecording()
            
            Toast.makeText(this, "Remote recording started: $sessionId", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote session start", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RemoteControl",
                "session_start_error",
                mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * Handle remote session stop request - Phase 1
     */
    private fun handleRemoteSessionStop() {
        try {
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "RemoteControl",
                "remote_session_stop",
                emptyMap()
            )
            
            // Stop recording through the service
            recordingService?.getRecordingController()?.stopRecording()
            
            Toast.makeText(this, "Remote recording stopped", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote session stop", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RemoteControl",
                "session_stop_error",
                mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * Bind to the recording service for remote control capability
     */
    private fun bindRecordingService() {
        val intent = Intent(this, RecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * Start network discovery to find PC controllers
     */
    /**
     * Start network discovery to find PC controllers - Phase 1 WebSocket implementation
     */
    private fun startNetworkDiscovery() {
        Log.i(TAG, "Starting WebSocket discovery for PC servers")
        updateConnectionStatus(ConnectionStatus.DISCOVERING)
        
        // WebSocket client handles discovery automatically when started
        // Discovery is handled in the initialization process
        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "WebSocketClient",
            "discovery_started",
            emptyMap()
        )
        
        // The WebSocket client will automatically discover and connect
        // Event callbacks handle the UI updates
    }
    
    /**
     * Handle remote recording request from PC controller
     */
    private fun handleRemoteRecordingRequest(sessionInfo: SessionInfo) {
        Log.i(TAG, "Processing remote recording request for session: ${sessionInfo.sessionId}")
        
        if (!isServiceBound || recordingService == null) {
            Log.e(TAG, "Recording service not available for remote request")
            showNetworkError("Recording service not ready")
            return
        }
        
        try {
            // Create session directory
            val baseDir = File(getExternalFilesDir(null), "recordings")
            val sessionDir = File(baseDir, sessionInfo.sessionId)
            
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            
            // Start recording via service
            RecordingService.startRecording(this, sessionDir.absolutePath)
            
            Toast.makeText(this, 
                "Remote recording started: ${sessionInfo.studyName}", 
                Toast.LENGTH_LONG).show()
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start remote recording", e)
            showNetworkError("Failed to start recording: ${e.message}")
        }
    }
    
    /**
     * Perform screen flash for synchronization
     */
    private fun performSyncFlash(durationMs: Int) {
        Log.i(TAG, "Performing sync flash for ${durationMs}ms")
        
        // Flash the screen white for synchronization
        val originalBackground = window.decorView.background
        
        try {
            // Set screen to white
            window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
            
            // Add sync marker to recording if active
            lifecycleScope.launch {
                recordingService?.getRecordingController()?.addSyncMarker("pc_sync_flash", System.nanoTime())
            }
            
            // Restore original background after duration
            window.decorView.postDelayed({
                window.decorView.background = originalBackground
            }, durationMs.toLong())
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform sync flash", e)
            // Ensure we restore the background even if there's an error
            window.decorView.background = originalBackground
        }
    }
    
    /**
     * Update connection status and UI
     */
    private fun updateConnectionStatus(status: ConnectionStatus) {
        connectionStatus = status
        
        // Update UI elements if they exist
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
    
    /**
     * Show network error to user with actionable options
     */
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
            // Just dismiss - user can try again later
        }
        
        builder.show()
    }
    
    /**
     * Enhanced PC connection with better error handling
     */
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
                    // Try both secure and non-secure connections
                    var success = networkClient?.connectToController(ipAddress, port, true) ?: false
                    
                    if (!success) {
                        Log.i(TAG, "Secure connection failed, trying non-secure...")
                        success = networkClient?.connectToController(ipAddress, port, false) ?: false
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Log.i(TAG, "Connection successful to $ipAddress:$port")
                            Toast.makeText(this@MainActivity, 
                                "Connected to PC at $ipAddress", 
                                Toast.LENGTH_LONG).show()
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
    
    /**
     * Get current connection status
     */
    fun getConnectionStatus(): ConnectionStatus = connectionStatus
    
    /**
     * Get network performance metrics
     */
    /**
     * Get network metrics for WebSocket connection - Phase 1
     */
    fun getNetworkMetrics(): String {
        val client = webSocketClient ?: return "WebSocket client not available"
        
        return "Status: ${if (client.isConnected()) "Connected" else "Disconnected"}, " +
               "Authenticated: ${client.isAuthenticated()}, " +
               "Reconnecting: ${client.isReconnecting()}"
    }
    
    /**
     * Enable automatic reconnection with exponential backoff
     */
    private fun enableAutoReconnection() {
        lifecycleScope.launch {
            var reconnectDelay = 5000L // Start with 5 seconds
            val maxDelay = 60000L // Max 60 seconds
            
            while (!isFinishing) {
                if (connectionStatus == ConnectionStatus.ERROR || 
                    connectionStatus == ConnectionStatus.DISCONNECTED) {
                    
                    Log.i(TAG, "Attempting auto-reconnection...")
                    updateConnectionStatus(ConnectionStatus.CONNECTING)
                    
                    val success = tryReconnection()
                    if (success) {
                        Log.i(TAG, "Auto-reconnection successful")
                        reconnectDelay = 5000L // Reset delay on success
                    } else {
                        Log.w(TAG, "Auto-reconnection failed, retrying in ${reconnectDelay}ms")
                        kotlinx.coroutines.delay(reconnectDelay)
                        reconnectDelay = minOf(reconnectDelay * 2, maxDelay) // Exponential backoff
                    }
                } else {
                    kotlinx.coroutines.delay(10000L) // Check every 10 seconds when connected
                }
            }
        }
    }
    
    /**
     * Try reconnection using various strategies
     */
    private suspend fun tryReconnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Strategy 1: Try last known controllers
                val controllers = networkClient?.getDiscoveredControllers()
                if (!controllers.isNullOrEmpty()) {
                    for (controller in controllers) {
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
                
                // Strategy 2: Start fresh discovery
                networkClient?.startDiscovery { discoverySuccess ->
                    if (discoverySuccess) {
                        val newControllers = networkClient?.getDiscoveredControllers()
                        if (!newControllers.isNullOrEmpty()) {
                            // Try connecting to first discovered controller
                            lifecycleScope.launch {
                                val controller = newControllers.first()
                                networkClient?.connectToController(
                                    controller.ipAddress, 
                                    controller.port
                                ) { connectSuccess ->
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
    
    /**
     * Send keep-alive heartbeat to maintain connection
     */
    private fun startHeartbeat() {
        lifecycleScope.launch {
            while (!isFinishing) {
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    try {
                        // WebSocket client handles heartbeat automatically
                        // Just send a status request periodically to test connection
                        webSocketClient?.sendStatusRequest()
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Status request exception", e)
                        updateConnectionStatus(ConnectionStatus.ERROR)
                    }
                }
                
                kotlinx.coroutines.delay(30000L) // Send status request every 30 seconds
            }
        }
    }
    
    /**
     * Get comprehensive status report for debugging
     */
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
                val recordingController = recordingService?.getRecordingController()
                val summary = recordingController?.getSensorStatusSummary()
                sb.append("Recording Status: $summary\n")
                
                // Add server socket status
                val serverStatus = recordingService?.getServerStatus()
                sb.append("Server Socket: $serverStatus\n")
                
                val connectedClients = recordingService?.getConnectedClients()
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
    
    /**
     * Handle network status bar click for manual connection
     */
    private fun handleNetworkStatusClick() {
        when (connectionStatus) {
            ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                // Show dialog for manual IP connection or status report
                showConnectionOptionsDialog()
            }
            ConnectionStatus.DISCOVERING, ConnectionStatus.CONNECTING -> {
                // Show discovery progress
                Toast.makeText(this, "Connection in progress...", Toast.LENGTH_SHORT).show()
            }
            ConnectionStatus.CONNECTED -> {
                // Show connection info and metrics
                showConnectionInfoDialog()
            }
        }
    }
    
    /**
     * Show comprehensive connection options dialog
     */
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
                    // Retry Discovery
                    Log.i(TAG, "Retrying network discovery")
                    startNetworkDiscovery()
                }
                1 -> {
                    // Manual IP Connection
                    showManualConnectionDialog()
                }
                2 -> {
                    // Status Report
                    showStatusReportDialog()
                }
                3 -> {
                    // Cancel - do nothing
                }
            }
        }
        builder.show()
    }
    
    /**
     * Show connection information dialog
     */
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
    
    /**
     * Show detailed status report dialog
     */
    private fun showStatusReportDialog() {
        val statusReport = getConnectionStatusReport()
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Detailed Status Report")
        builder.setMessage(statusReport)
        builder.setPositiveButton("Copy to Clipboard") { _, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Status Report", statusReport)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Status report copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }
    
    /**
     * Test remote recording capability
     */
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
            
            Toast.makeText(this, "Testing remote recording capability...", Toast.LENGTH_SHORT).show()
            
            // Simulate remote recording request
            handleRemoteRecordingRequest(testSessionInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing remote recording", e)
            Toast.makeText(this, "Test failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show dialog for manual PC connection
     */
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
                // Try common default IP addresses
                tryCommonIPAddresses()
            }
        }
        
        builder.setNegativeButton("Try Defaults") { _, _ ->
            tryCommonIPAddresses()
        }
        
        builder.setNeutralButton("Cancel", null)
        builder.show()
    }
    
    /**
     * Try connecting to common IP addresses
     */
    private fun tryCommonIPAddresses() {
        val commonIPs = listOf(
            "192.168.1.100", "192.168.1.101", "192.168.1.102",
            "192.168.0.100", "192.168.0.101", "192.168.0.102", 
            "10.0.0.100", "10.0.0.101", "10.0.0.102",
            "127.0.0.1" // localhost for testing
        )
        
        Toast.makeText(this, "Trying common IP addresses...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            var connected = false
            
            for (ip in commonIPs) {
                if (connected) break // Stop if we've already connected
                
                Log.i(TAG, "Trying to connect to $ip")
                updateConnectionStatus(ConnectionStatus.CONNECTING)
                
                try {
                    val success = withContext(Dispatchers.IO) {
                        // Try secure first, then non-secure
                        networkClient?.connectToController(ip, 8080, true) ?: false ||
                        networkClient?.connectToController(ip, 8080, false) ?: false
                    }
                    
                    if (success) {
                        Log.i(TAG, "Successfully connected to $ip")
                        Toast.makeText(this@MainActivity, "Connected to PC at $ip", Toast.LENGTH_LONG).show()
                        connected = true
                        break
                    } else {
                        Log.w(TAG, "Failed to connect to $ip")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Connection error to $ip: ${e.message}")
                }
            }
            
            // If all failed, restart discovery
            if (!connected) {
                updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                Toast.makeText(this@MainActivity, 
                    "No PC found at common addresses. Restarting discovery...", 
                    Toast.LENGTH_LONG).show()
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
        
        // Cleanup networking - Phase 1 WebSocket client
        try {
            webSocketClient?.stop()
            webSocketClient = null
            
            // Stop server socket
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
}
