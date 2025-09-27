package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.bean.event.PDFEvent
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.libunified.app.utils.PermissionUtils
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.databinding.ActivityIrMainBinding
import com.mpdc4gsr.module.thermalunified.dialog.HomeGuideDialog
import com.mpdc4gsr.module.thermalunified.fragment.AbilityFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRThermalFragment
import com.mpdc4gsr.module.thermalunified.fragment.PDFListFragment
import com.mpdc4gsr.module.user.fragment.MoreFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import com.mpdc4gsr.libunified.R as LibR


class IRMainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityIrMainBinding


    private var isTC007 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIrMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initView()
    }

    private fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        binding.viewPage.offscreenPageLimit = 5
        binding.viewPage.isUserInputEnabled = false
        binding.viewPage.adapter = ViewPagerAdapter(this, isTC007)
        binding.viewPage.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    refreshTabSelect(position)
                }
            },
        )
        binding.viewPage.setCurrentItem(2, false)

        binding.clIconMonitor.setOnClickListener(this)
        binding.clIconGallery.setOnClickListener(this)

        binding.clIconReport.setOnClickListener(this)
        binding.clIconMine.setOnClickListener(this)

        showGuideDialog()
    }

    override fun onResume() {
        super.onResume()

        if (isTC007) {
            if (WebSocketProxy.getInstance().isTC007Connect()) {
                NetWorkUtils.switchNetwork(false)
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                lifecycleScope.launch {
                    // TC007Repository.syncTime() // TC007Repository functionality removed
                }
                if (SharedManager.isConnect07AutoOpen) {
                    NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_07)
                        .navigation(this)
                }
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        } else {
            if (DeviceTools.isConnect(isAutoRequest = false)) {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
            }
        }
    }

    private fun initData() {
    }

    private fun connected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    private fun disConnected() {
        if (!isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    private fun onSocketConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
        }
    }

    private fun onSocketDisConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_disconnect)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clIconMonitor -> {
                binding.viewPage.setCurrentItem(0, false)
            }

            binding.clIconGallery -> {
                checkStoragePermission()
            }


            binding.clIconReport -> {
                if (LMS.getInstance().isLogin) {
                    binding.viewPage.setCurrentItem(3, false)
                } else {
                    LMS.getInstance().activityLogin(null) {
                        if (it) {
                            binding.viewPage.setCurrentItem(3, false)
                            EventBus.getDefault().post(PDFEvent())
                        }
                    }
                }
            }

            binding.clIconMine -> {
                binding.viewPage.setCurrentItem(4, false)
            }
        }
    }


    private fun refreshTabSelect(index: Int) {
        binding.ivIconMonitor.isSelected = false
        binding.tvIconMonitor.isSelected = false
        binding.ivIconGallery.isSelected = false
        binding.tvIconGallery.isSelected = false
        binding.ivIconReport.isSelected = false
        binding.tvIconReport.isSelected = false
        binding.ivIconMine.isSelected = false
        binding.tvIconMine.isSelected = false
        when (index) {
            0 -> {
                binding.ivIconMonitor.isSelected = true
                binding.tvIconMonitor.isSelected = true
            }

            1 -> {
                binding.ivIconGallery.isSelected = true
                binding.tvIconGallery.isSelected = true
            }

            3 -> {
                binding.ivIconReport.isSelected = true
                binding.tvIconReport.isSelected = true
            }

            4 -> {
                binding.ivIconMine.isSelected = true
                binding.tvIconMine.isSelected = true
            }
        }
    }

    private fun showGuideDialog() {
        if (SharedManager.homeGuideStep == 0) {
            return
        }

        when (SharedManager.homeGuideStep) {
            1 -> binding.viewPage.setCurrentItem(0, false)
            2 -> binding.viewPage.setCurrentItem(4, false)
            3 -> binding.viewPage.setCurrentItem(2, false)
        }

        val guideDialog = HomeGuideDialog(this, SharedManager.homeGuideStep)
        guideDialog.onNextClickListener = {
            when (it) {
                1 -> {
                    binding.viewPage.setCurrentItem(4, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(binding.clRoot)
                        }
                    }
                    SharedManager.homeGuideStep = 2
                }

                2 -> {
                    binding.viewPage.setCurrentItem(2, false)
                    if (Build.VERSION.SDK_INT < 31) {
                        lifecycleScope.launch {
                            delay(100)
                            guideDialog.blurBg(binding.clRoot)
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
            window?.decorView?.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20f,
                    20f,
                    Shader.TileMode.MIRROR
                )
            )
        } else {
            lifecycleScope.launch {


                delay(100)
                guideDialog.blurBg(binding.clRoot)
            }
        }
    }

    private fun checkStoragePermission() {
        val permissionList: List<String> =
            if (this.applicationInfo.targetSdkVersion >= 34) {
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else if (this.applicationInfo.targetSdkVersion >= 34) {
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

        if (!XXPermissions.isGranted(this, permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                // Show tooltip only once per session to avoid spam
                if (!SharedManager.hasShownStoragePermissionTip) {
                    TipDialog.Builder(this)
                        .setMessage(
                            getString(
                                LibR.string.permission_request_storage_app,
                                CommUtils.getAppName()
                            )
                        )
                        .setCancelListener(LibR.string.app_cancel)
                        .setPositiveListener(LibR.string.app_confirm) {
                            initStoragePermission(permissionList)
                        }
                        .create().show()
                    SharedManager.hasShownStoragePermissionTip = true
                } else {
                    // Skip dialog if already shown
                    initStoragePermission(permissionList)
                }
            } else {
                initStoragePermission(permissionList)
            }
        } else {
            initStoragePermission(permissionList)
        }
    }

    private fun initStoragePermission(permissionList: List<String>) {
        if (PermissionUtils.isVisualUser()) {
            binding.viewPage.setCurrentItem(1, false)
            return
        }
        XXPermissions.with(this)
            .permission(permissionList)
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        if (allGranted) {
                            binding.viewPage.setCurrentItem(1, false)
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (doNotAskAgain) {

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
                },
            )
    }

    private class ViewPagerAdapter(val activity: FragmentActivity, val isTC007: Boolean) :
        FragmentStateAdapter(activity) {
        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment {
            if (position == 1) {
                return IRGalleryTabFragment().apply {
                    arguments =
                        Bundle().also {
                            val dirType =
                                if (isTC007) DirType.TC007.ordinal else DirType.LINE.ordinal
                            it.putBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false)
                            it.putBoolean(ExtraKeyConfig.HAS_BACK_ICON, false)
                            it.putInt(ExtraKeyConfig.DIR_TYPE, dirType)
                        }
                }
            } else {
                val fragment =
                    when (position) {
                        0 -> AbilityFragment()
                        2 -> IRThermalFragment()
                        3 -> PDFListFragment()
                        else -> MoreFragment()
                    }
                fragment.arguments =
                    Bundle().also { it.putBoolean(ExtraKeyConfig.IS_TC007, isTC007) }
                return fragment
            }
        }
    }
}
