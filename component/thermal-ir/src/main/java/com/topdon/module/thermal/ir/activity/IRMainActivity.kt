package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.PDFEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.repository.GalleryRepository.DirType
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.utils.PermissionUtils
import com.topdon.lms.sdk.LMS
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.R as LibR
import com.topdon.module.thermal.ir.dialog.HomeGuideDialog
import com.topdon.module.thermal.ir.fragment.IRGalleryTabFragment
import com.topdon.module.thermal.ir.fragment.IRThermalFragment
import com.topdon.module.thermal.ir.fragment.AbilityFragment
import com.topdon.module.thermal.ir.fragment.PDFListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * 插件式 或 TC007 首页.
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007
 *
 * Created by LCG on 2024/4/18.
 */
// Legacy ARouter route annotation - now using NavigationManager
class IRMainActivity : BaseActivity(), View.OnClickListener {

    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false

    // View references - migrated from synthetic views  
    private lateinit var viewPage: ViewPager2
    private lateinit var clRoot: ConstraintLayout
    private lateinit var ivMainBg: ImageView
    private lateinit var clIconMonitor: ConstraintLayout
    private lateinit var clIconGallery: ConstraintLayout
    private lateinit var clIconReport: ConstraintLayout
    private lateinit var clIconMine: ConstraintLayout
    private lateinit var ivIconMonitor: ImageView
    private lateinit var ivIconGallery: ImageView
    private lateinit var ivIconReport: ImageView
    private lateinit var ivIconMine: ImageView
    private lateinit var tvIconMonitor: TextView
    private lateinit var tvIconGallery: TextView
    private lateinit var tvIconReport: TextView
    private lateinit var tvIconMine: TextView

    override fun initContentView(): Int = R.layout.activity_ir_main

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initView()
    }

    override fun initView() {
        // Initialize views - migrated from synthetic views
        viewPage = findViewById(R.id.view_page)
        // clRoot = findViewById(R.id.clRoot)  // TODO: Verify this ID exists in layout
        ivMainBg = findViewById(R.id.iv_main_bg)
        clIconMonitor = findViewById(R.id.cl_icon_monitor)
        clIconGallery = findViewById(R.id.cl_icon_gallery)
        clIconReport = findViewById(R.id.cl_icon_report)
        clIconMine = findViewById(R.id.cl_icon_mine)
        ivIconMonitor = findViewById(R.id.iv_icon_monitor)
        ivIconGallery = findViewById(R.id.iv_icon_gallery)
        ivIconReport = findViewById(R.id.iv_icon_report)
        ivIconMine = findViewById(R.id.iv_icon_mine)
        tvIconMonitor = findViewById(R.id.tv_icon_monitor)
        tvIconGallery = findViewById(R.id.tv_icon_gallery)
        tvIconReport = findViewById(R.id.tv_icon_report)
        tvIconMine = findViewById(R.id.tv_icon_mine)

        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        viewPage.offscreenPageLimit = 5
        viewPage.isUserInputEnabled = false
        viewPage.adapter = ViewPagerAdapter(this, isTC007)
        viewPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                refreshTabSelect(position)
            }
        })
        viewPage.setCurrentItem(2, false)

        clIconMonitor.setOnClickListener(this)
        clIconGallery.setOnClickListener(this)
        // view_main_thermal.setOnClickListener(this) // Not found in view declarations, likely unused
        clIconReport.setOnClickListener(this)
        clIconMine.setOnClickListener(this)

        showGuideDialog()
    }

    override fun onResume() {
        super.onResume()
//        DeviceTools.isConnect(true)
        if (isTC007) {
            if (WebSocketProxy.getInstance().isTC007Connect()) {
                NetWorkUtils.switchNetwork(false)
                ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                lifecycleScope.launch {
                    TC007Repository.syncTime()
                }
                if (SharedManager.isConnect07AutoOpen) {
                    NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_07).navigation(this)
                }
            } else {
                ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        } else {
            if (DeviceTools.isConnect(isAutoRequest = false)) {
                ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
            } else {
                ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        }
    }

    override fun initData() {
    }

    override fun connected() {
        if (!isTC007) {
            ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            clIconMonitor -> {//监控
                viewPage.setCurrentItem(0, false)
            }
            clIconGallery -> {//图库
                checkStoragePermission()
            }
            // view_main_thermal -> {//首页 - Commented out as not in view declarations
            //     viewPage.setCurrentItem(2, false)  
            // }
            clIconReport -> {//报告
                if (LMS.getInstance().isLogin) {
                    viewPage.setCurrentItem(3, false)
                } else {
                    LMS.getInstance().activityLogin(null) {
                        if (it) {
                            viewPage.setCurrentItem(3, false)
                            EventBus.getDefault().post(PDFEvent())
                        }
                    }
                }
            }
            clIconMine -> {//我的
                viewPage.setCurrentItem(4, false)
            }
        }
    }

    /**
     * 刷新 5 个 tab 的选中状态
     * @param index 当前选中哪个 tab，`[0, 4]`
     */
    private fun refreshTabSelect(index: Int) {
        ivIconMonitor.isSelected = false
        tvIconMonitor.isSelected = false
        ivIconGallery.isSelected = false
        tvIconGallery.isSelected = false
        ivIconReport.isSelected = false
        tvIconReport.isSelected = false
        ivIconMine.isSelected = false
        tvIconMine.isSelected = false
        when (index) {
            0 -> {
                ivIconMonitor.isSelected = true
                tvIconMonitor.isSelected = true
            }
            1 -> {
                ivIconGallery.isSelected = true
                tvIconGallery.isSelected = true
            }
            3 -> {
                ivIconReport.isSelected = true
                tvIconReport.isSelected = true
            }
            4 -> {
                ivIconMine.isSelected = true
                tvIconMine.isSelected = true
            }
        }
    }

    /**
     * 显示操作指引弹框.
     */
    private fun showGuideDialog() {
        if (SharedManager.homeGuideStep == 0) {//已看过或不再提示
            return
        }

        when (SharedManager.homeGuideStep) {
            1 -> viewPage.setCurrentItem(0, false)
            2 -> viewPage.setCurrentItem(4, false)
            3 -> viewPage.setCurrentItem(2, false)
        }

        val guideDialog = HomeGuideDialog(this, SharedManager.homeGuideStep)
        guideDialog.onNextClickListener = {
            when (it) {
                1 -> {
                    viewPage.setCurrentItem(4, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(clRoot)
                        }
                    }
                    SharedManager.homeGuideStep = 2
                }
                2 -> {
                    viewPage.setCurrentItem(2, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(clRoot)
                        }
                    }
                    SharedManager.homeGuideStep = 3
                }
                3 -> {
                    SharedManager.homeGuideStep = 0
                }
            }
        }
        guideDialog.onSkinClickListener = {
            SharedManager.homeGuideStep = 0
        }
        guideDialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(null)
            }
        }
        guideDialog.show()

        if (Build.VERSION.SDK_INT >= 31) {
            window?.decorView?.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR))
        } else {
            lifecycleScope.launch {
                //界面切换及温度监控历史列表加载均需要时间，所以需要等待1000毫秒再去刷新背景
                //而若等待1000毫秒太过久，界面会非模糊1000毫秒，所以先刷新一次背景占位
                delay(100)
                guideDialog.blurBg(clRoot)
            }
        }
    }


    private fun checkStoragePermission() {
        val permissionList: List<String> =
            if (this.applicationInfo.targetSdkVersion >= 34){
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else if (this.applicationInfo.targetSdkVersion >= 34){
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else if (this.applicationInfo.targetSdkVersion == 33) {
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }

        if (!XXPermissions.isGranted(this, permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(getString(LibR.string.permission_request_storage_app, CommUtils.getAppName()))
                    .setCancelListener(LibR.string.app_cancel)
                    .setPositiveListener(LibR.string.app_confirm) {
                        initStoragePermission(permissionList)
                    }
                    .create().show()
            } else {
                initStoragePermission(permissionList)
            }
        } else {
            initStoragePermission(permissionList)
        }
    }

    /**
     * 动态申请权限
     */
    private fun initStoragePermission(permissionList: List<String>) {
        if (PermissionUtils.isVisualUser()){
            viewPage.setCurrentItem(1, false)
            return
        }
        XXPermissions.with(this)
            .permission(permissionList)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        viewPage.setCurrentItem(1, false)
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        //拒绝授权并且不再提醒
                        TipDialog.Builder(this@IRMainActivity)
                            .setTitleMessage(getString(LibR.string.app_tip))
                            .setMessage(getString(LibR.string.app_album_content))
                            .setPositiveListener(LibR.string.app_open) {
                                AppUtils.launchAppDetailsSettings()
                            }
                            .setCancelListener(LibR.string.app_cancel) {
                            }
                            .setCanceled(true)
                            .create().show()
                    }
                }
            })
    }



    private class ViewPagerAdapter(val activity: FragmentActivity, val isTC007: Boolean) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment {
            if (position == 1) {//图库
                return IRGalleryTabFragment().apply {
                    arguments = Bundle().also {
                        val dirType = if (isTC007) DirType.TC007.ordinal else DirType.LINE.ordinal
                        it.putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false)
                        it.putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                        it.putInt(ExtraKeyConfig.DIR_TYPE, dirType)
                    }
                }
            } else {
                val fragment = when (position) {
                    0 -> AbilityFragment()
                    2 -> IRThermalFragment()
                    3 -> PDFListFragment()
                    else -> NavigationManager.getInstance().build(RouterConfig.TC_MORE).navigation(activity) as Fragment
                }
                fragment.arguments = Bundle().also { it.putBoolean(ExtraKeyConfig.IS_TC007, isTC007) }
                return fragment
            }
        }
    }
}