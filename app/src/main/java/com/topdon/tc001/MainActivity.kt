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
// Network integration imports
import com.topdon.tc001.network.NetworkClient
import com.topdon.tc001.service.RecordingService
import com.topdon.tc001.controller.RecordingController
import com.topdon.gsr.model.SessionInfo
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
    
    // PC-to-phone control networking
    private var networkClient: NetworkClient? = null
    private var recordingService: RecordingService? = null
    private var isServiceBound = false
    private var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
    
    // UI elements for network status
    private var networkStatusIndicator: ImageView? = null
    private var networkStatusText: TextView? = null
    
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
    
    // ==================== PC-to-Phone Control Networking ====================
    
    /**
     * Initialize the PC-to-phone control networking system
     */
    private fun initNetworking() {
        Log.i(TAG, "Initializing PC-to-phone control networking")
        
        try {
            // Initialize network client
            networkClient = NetworkClient(this).apply {
                // Initialize the enhanced network client
                val initSuccess = initialize()
                if (!initSuccess) {
                    Log.w(TAG, "Network client initialization failed")
                    updateConnectionStatus(ConnectionStatus.ERROR)
                    return
                }
                
                // Set up event listener for network events
                setEventListener(createNetworkEventListener())
            }
            
            // Bind to recording service for remote control capability
            bindRecordingService()
            
            // Start recording service with server socket automatically
            RecordingService.startServer(this)
            
            // Start automatic discovery and connection
            startNetworkDiscovery()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize networking", e)
            updateConnectionStatus(ConnectionStatus.ERROR)
            showNetworkError("Network initialization failed: ${e.message}")
        }
    }
    
    /**
     * Create network event listener to handle PC controller events
     */
    private fun createNetworkEventListener(): NetworkClient.NetworkEventListener {
        return object : NetworkClient.NetworkEventListener {
            override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                Log.i(TAG, "PC Controller discovered: ${controller.deviceName} at ${controller.ipAddress}")
                
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTING)
                    Toast.makeText(this@MainActivity, 
                        "Found PC Controller: ${controller.deviceName}", 
                        Toast.LENGTH_SHORT).show()
                }
                
                // Automatically connect to discovered controller
                networkClient?.connectToController(controller.ipAddress, controller.port) { success ->
                    runOnUiThread {
                        if (success) {
                            Log.i(TAG, "Successfully connected to PC Controller")
                        } else {
                            Log.w(TAG, "Failed to connect to PC Controller")
                            updateConnectionStatus(ConnectionStatus.ERROR)
                        }
                    }
                }
            }
            
            override fun onConnected(controller: NetworkClient.ControllerInfo) {
                Log.i(TAG, "Connected to PC Controller: ${controller.deviceName}")
                
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTED)
                    Toast.makeText(this@MainActivity, 
                        "Connected to PC: ${controller.deviceName}", 
                        Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onDisconnected(reason: String) {
                Log.i(TAG, "Disconnected from PC Controller: $reason")
                
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                    Toast.makeText(this@MainActivity, 
                        "Disconnected from PC: $reason", 
                        Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                Log.i(TAG, "Remote measurement request received: ${sessionInfo.sessionId}")
                
                runOnUiThread {
                    handleRemoteRecordingRequest(sessionInfo)
                }
            }
            
            override fun onSyncFlash(durationMs: Int) {
                Log.i(TAG, "Sync flash requested: ${durationMs}ms")
                
                runOnUiThread {
                    performSyncFlash(durationMs)
                }
            }
            
            override fun onTimeSynchronized(offsetNanoseconds: Long) {
                Log.i(TAG, "Time synchronized with PC (offset: ${offsetNanoseconds}ns)")
            }
            
            override fun onDataStreamingStarted() {
                Log.i(TAG, "Data streaming to PC started")
            }
            
            override fun onDataStreamingStopped() {
                Log.i(TAG, "Data streaming to PC stopped")
            }
            
            override fun onError(operation: String, error: String) {
                Log.e(TAG, "Network error in $operation: $error")
                
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.ERROR)
                    showNetworkError("Network error: $error")
                }
            }
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
    private fun startNetworkDiscovery() {
        Log.i(TAG, "Starting network discovery for PC controllers")
        updateConnectionStatus(ConnectionStatus.DISCOVERING)
        
        networkClient?.startDiscovery { success ->
            runOnUiThread {
                if (success) {
                    Log.i(TAG, "Network discovery completed successfully")
                    
                    val controllers = networkClient?.getDiscoveredControllers()
                    if (controllers.isNullOrEmpty()) {
                        Log.i(TAG, "No PC controllers found during discovery")
                        updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                        Toast.makeText(this@MainActivity, 
                            "No PC controllers found. Ensure PC app is running on same network.", 
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Network discovery failed")
                    updateConnectionStatus(ConnectionStatus.ERROR)
                    showNetworkError("Discovery failed. Check network connection.")
                }
            }
        }
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
    fun getNetworkMetrics(): String {
        val client = networkClient ?: return "Network client not available"
        
        return "Latency: ${client.getLatencyMs()}ms, " +
               "Throughput: ${String.format("%.1f", client.getThroughputKBps())}KB/s, " +
               "Secure: ${client.isSecureConnection()}"
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
                        // Send ping message to keep connection alive
                        val heartbeat = org.json.JSONObject().apply {
                            put("type", "heartbeat")
                            put("timestamp", System.currentTimeMillis())
                            put("device_id", android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            ))
                        }
                        
                        val success = try {
                            // Use sendMeasurementData as a heartbeat mechanism
                            networkClient?.sendMeasurementData("heartbeat", heartbeat) ?: false
                        } catch (e: Exception) {
                            Log.w(TAG, "Heartbeat exception", e)
                            false
                        }
                        if (!success) {
                            Log.w(TAG, "Heartbeat failed, connection may be lost")
                            updateConnectionStatus(ConnectionStatus.ERROR)
                        }
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat error", e)
                        updateConnectionStatus(ConnectionStatus.ERROR)
                    }
                }
                
                kotlinx.coroutines.delay(30000L) // Send heartbeat every 30 seconds
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
        
        val controllers = networkClient?.getDiscoveredControllers()
        sb.append("Discovered Controllers: ${controllers?.size ?: 0}\n")
        controllers?.forEach { controller ->
            sb.append("  - ${controller.deviceName} (${controller.ipAddress}:${controller.port})\n")
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
        val controllers = networkClient?.getDiscoveredControllers()
        val currentController = controllers?.firstOrNull()
        
        val message = StringBuilder()
        message.append("Connection Status: Connected\n\n")
        message.append("Performance Metrics:\n$metrics\n\n")
        
        if (currentController != null) {
            message.append("Connected to:\n")
            message.append("Device: ${currentController.deviceName}\n")
            message.append("Address: ${currentController.ipAddress}:${currentController.port}\n\n")
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
        
        // Cleanup networking
        try {
            networkClient?.disconnect()
            networkClient?.cleanup()
            networkClient = null
            
            // Stop server socket
            RecordingService.stopServer(this)
            
            if (isServiceBound) {
                unbindService(serviceConnection)
                isServiceBound = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during networking cleanup", e)
        }
    }
}
