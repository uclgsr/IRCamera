package com.topdon.tc001

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
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
import com.blankj.utilcode.util.Utils
import com.elvishew.xlog.XLog
// import com.example.suplib.wrapper.SupHelp // TODO: Fix SupHelp library access
import com.example.thermal_lite.activity.IRThermalLiteActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
// import com.topdon.hik.activity.IRThermalHikActivity // TODO: Implement HIK activity
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
import com.topdon.lib.core.ktbase.BaseActivity
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
import com.csl.irCamera.R
import com.csl.irCamera.BuildConfig
import com.zoho.commons.LauncherModes
import com.zoho.commons.LauncherProperties
import com.zoho.salesiqembed.ZohoSalesIQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


// Legacy ARouter route annotation - now using NavigationManager
class MainActivity : BaseActivity(), View.OnClickListener {

    private val versionViewModel: VersionViewModel by viewModels()
    
    // findViewById declarations  
    private val viewPage: ViewPager2 by lazy { findViewById(R.id.view_page) }
    private val viewMain: View by lazy { findViewById(R.id.view_main) }
    private val clIconGallery: ConstraintLayout by lazy { findViewById(R.id.cl_icon_gallery) }
    private val clIconMine: ConstraintLayout by lazy { findViewById(R.id.cl_icon_mine) }
    private val ivIconGallery: ImageView by lazy { findViewById(R.id.iv_icon_gallery) }
    private val ivIconMine: ImageView by lazy { findViewById(R.id.iv_icon_mine) }
    private val tvIconMine: TextView by lazy { findViewById(R.id.tv_icon_mine) }
    private val tvIconGallery: TextView by lazy { findViewById(R.id.tv_icon_gallery) }
    private val ivBottomMainBg: ImageView by lazy { findViewById(R.id.iv_bottom_main_bg) }
    private val viewMinePoint: View by lazy { findViewById(R.id.view_mine_point) }

    private var checkPermissionType: Int = -1 //0 initData数据 1 图库  2 connect方法
    override fun initContentView() = R.layout.activity_main

    //记录设备信息
    private fun logInfo() {
        try {
            val str = StringBuilder()
            str.append("Info").append("\n")
            str.append("FLAVOR: ${BuildConfig.FLAVOR}").append("\n")
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

    override fun initView() {
        logInfo()
        lifecycleScope.launch(Dispatchers.IO){
            // TODO: Fix SupHelp library access
            // SupHelp.getInstance().initAiUpScaler(Utils.getApp())
        }
        viewPage.offscreenPageLimit = 3
        viewPage.isUserInputEnabled = false
        viewPage.adapter = ViewPagerAdapter(this)
        viewPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                refreshTabSelect(position)
            }
        })
        if (savedInstanceState == null) {
            viewPage.setCurrentItem(1, false)
        }

        viewMinePoint.isVisible = !SharedManager.hasClickWinter

        clIconGallery.setOnClickListener(this)
        viewMain.setOnClickListener(this)
        clIconMine.setOnClickListener(this)
        App.instance.initWebSocket()
        copyFile("SR.pb", File(filesDir, "SR.pb"))
        BaseApplication.instance.clearDb()
        if (BaseApplication.instance.isDomestic()) {
            checkAppVersion(true)
        } else {
            versionViewModel.checkVersion()
        }

        if (!SharedManager.hasTcLine && !SharedManager.hasTS004 && !SharedManager.hasTC007) {
            //仅当设备列表为空时，才执行自动跳转
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

        //版本下载
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
                    SharedManager.setVersionCheckDate(System.currentTimeMillis())//刷新版本提示时间
                }
            }.show()
        }
    }

    private fun updateApk(url : String) {
        if (applicationInfo.targetSdkVersion < Build.VERSION_CODES.P) {
            //目标版本27默认跳到官网下载
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
            resetTipsDialog = TipDialog.Builder(this)
                .setMessage(R.string.device_reset_alert)
                .setPositiveListener(R.string.app_got_it) {
                }
                .create()
        }
        resetTipsDialog?.show()
    }


    private var disconnectDialog: TipDialog? = null
    private fun dialogDisconnect(){
        if (resetTipsDialog?.isShowing == true) {
            return
        }
        if (disconnectDialog == null) {
            disconnectDialog = TipDialog.Builder(this)
                .setMessage(R.string.device_disconnect_alert)
                .setPositiveListener(R.string.app_got_it) {
                }
                .create()
        }
        disconnectDialog?.show()
    }

    private fun copyFile(filename: String, targetFile: File) {
        if (targetFile.exists()) {//已存在就不覆盖了
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

    override fun initData() {
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
            clIconGallery -> {//图库
                checkPermissionType = 1
                checkStoragePermission()
            }
            viewMain -> {//首页
                viewPage.setCurrentItem(1, false)
            }
            clIconMine -> {//我的
                viewPage.setCurrentItem(2, false)
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
        viewMinePoint.isVisible = false
    }

    /**
     * 刷新 3 个 tab 的选中状态
     * @param index 当前选中哪个 tab，`[0, 2]`
     */
    private fun refreshTabSelect(index: Int) {
        ivIconGallery.isSelected = false
        tvIconGallery.isSelected = false
        ivIconMine.isSelected = false
        tvIconMine.isSelected = false
        ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_not_select)

        when (index) {
            0 -> {//图库
                ivIconGallery.isSelected = true
                tvIconGallery.isSelected = true
            }
            1 -> {
                ivBottomMainBg.setImageResource(R.drawable.ic_main_bg_select)
            }
            2 -> {//我的
                ivIconMine.isSelected = true
                tvIconMine.isSelected = true
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
        //无连接OTG提示
        if (tipOtgDialog != null && tipOtgDialog!!.isShowing) {
            return
        }
        if (SharedManager.isTipOTG && !BaseApplication.instance.hasOtgShow) {
            tipOtgDialog = TipOtgDialog.Builder(this)
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
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) && isTS004) {//TC007不用
            dialogDisconnect()
        }
    }



    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    IRGalleryTabFragment().apply {
                        arguments = Bundle().also {
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
        (if (this.applicationInfo.targetSdkVersion >= 34){
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }).let {
            sparseArray.append(R.string.permission_request_storage_app, it)
        }
        return sparseArray
    }

    private fun checkCameraPermission() {
        if (!PermissionUtils.isVisualUser() && !XXPermissions.isGranted(
                this,
                getNeedPermissionList()[R.string.permission_request_camera_app]
            )
        ) {
            if (BaseApplication.instance.isDomestic()) {
                if (SharedManager.getMainPermissionsState()) {
                    //国内版拒绝授权之后就别再授权了华为上架不通过
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
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        checkStoragePermission()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (BaseApplication.instance.isDomestic()) {
                        SharedManager.setMainPermissionsState(true)
                    }
                    if (doNotAskAgain) {
                        //拒绝授权并且不再提醒
                        TipDialog.Builder(this@MainActivity)
                            .setTitleMessage(getString(R.string.app_tip))
                            .setMessage(if (PermissionUtils.hasCameraPermission())
                                getString(R.string.app_album_content)
                                else getString(R.string.app_camera_content))
                            .setPositiveListener(R.string.app_open) {
                                AppUtils.launchAppDetailsSettings()
                            }
                            .setCancelListener(R.string.app_cancel) {
                            }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
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
        if (PermissionUtils.isVisualUser()){
            jumpIRActivity()
            return
        }
        XXPermissions.with(this)
            .permission(
                getNeedPermissionList()[R.string.permission_request_storage_app]
            )
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        jumpIRActivity()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        //拒绝授权并且不再提醒
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
            })
    }


    fun jumpIRActivity(){
        when (checkPermissionType) {
            0 -> {
                DeviceTools.isConnect(isSendConnectEvent = true)
            }
            1 -> {
                viewPage.setCurrentItem(0, false)
            }
            2 -> {

                if (DeviceTools.isTC001PlusConnect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalPlusActivity::class.java), 101)
                }else if(DeviceTools.isTC001LiteConnect()){
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalLiteActivity::class.java), 101)
                } else if (DeviceTools.isHikConnect()) {
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    // TODO: Implement IRThermalHikActivity or use alternative thermal activity
                    startActivityForResult(Intent(this@MainActivity, IRThermalNightActivity::class.java), 101)
                } else{
                    NavigationManager.build(RouterConfig.IR_MAIN).navigation(this@MainActivity)
                    startActivityForResult(Intent(this@MainActivity, IRThermalNightActivity::class.java), 101)
                }
            }
        }
    }

    private var appVersionUtil: AppVersionUtil? = null
    private fun checkAppVersion(isShow: Boolean) {
        if (appVersionUtil == null) {
            appVersionUtil = AppVersionUtil(this, object : AppVersionUtil.DotIsShowListener {
                override fun isShow(show: Boolean) {
                }

                override fun version(version: String) {
                }
            })
        }
        appVersionUtil?.checkVersion(isShow)
    }
}