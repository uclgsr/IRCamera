package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
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
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMainActivityViewModel
import com.mpdc4gsr.module.user.fragment.MoreFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import com.mpdc4gsr.libunified.R as LibR


class IRMainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityIrMainBinding
    private val viewModel: IRMainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIrMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        setupObservers()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initView()
    }

    private fun setupObservers() {
        viewModel.deviceState.observe(this) { deviceState ->
            updateDeviceState(deviceState)
        }

        viewModel.fragmentCommunication.observe(this) { communication ->
            handleFragmentCommunication(communication)
        }

        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is IRMainActivityViewModel.NavigationEvent.ToMonitor -> {
                    NavigationManager.getInstance()
                        .build(RouterConfig.MONITOR_HOME)
                        .withBoolean(ExtraKeyConfig.IS_TC007, event.isTC007)
                        .navigation(this)
                }
                is IRMainActivityViewModel.NavigationEvent.ToGallery -> {
                    NavigationManager.getInstance()
                        .build(RouterConfig.GALLERY)
                        .navigation(this)
                }
            }
        }

        viewModel.viewPagerState.observe(this) { state ->
            when (state) {
                is IRMainActivityViewModel.ViewPagerState.PageSelected -> {
                    refreshTabSelect(state.position)
                }
                is IRMainActivityViewModel.ViewPagerState.NavigateToPage -> {
                    binding.viewPage.setCurrentItem(state.position, false)
                }
            }
        }
    }

    private fun initView() {
        val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        viewModel.setDeviceType(isTC007)

        binding.viewPage.offscreenPageLimit = 5
        binding.viewPage.isUserInputEnabled = false
        binding.viewPage.adapter = ViewPagerAdapter(this, isTC007)
        binding.viewPage.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.onPageSelected(position)
                }
            },
        )
        binding.viewPage.setCurrentItem(2, false)

        binding.clIconMonitor.setOnClickListener(this)
        binding.clIconGallery.setOnClickListener(this)
        binding.clIconReport.setOnClickListener(this)
        binding.clIconMine.setOnClickListener(this)

        viewModel.initializeDeviceState()
        showGuideDialog()
    }

    private fun updateDeviceState(deviceState: IRMainActivityViewModel.DeviceState) {
        if (deviceState.isTC007) {
            if (deviceState.isWebSocketConnected) {
                NetWorkUtils.switchNetwork(false)
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                if (deviceState.shouldAutoOpen) {
                    viewModel.navigateToThermal()
                }
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_no_connect)
            }
        } else {
            if (deviceState.isUsbConnected) {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_connect)
                if (deviceState.shouldAutoOpen) {
                    viewModel.navigateToThermal()
                }
            } else {
                binding.ivMainBg.setImageResource(R.drawable.ic_ir_main_bg_no_connect)
            }
        }

        // Apply blur effect if needed
        if (deviceState.shouldBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.root.setRenderEffect(
                RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
            )
        } else {
            binding.root.setRenderEffect(null)
        }
    }

    private fun handleFragmentCommunication(communication: IRMainActivityViewModel.FragmentCommunicationState) {
        // Handle inter-fragment communication through ViewModel
        when (communication.activeFragment) {
            0 -> {
                // IRThermalFragment communication
            }
            1 -> {
                // IRGalleryTabFragment communication  
            }
            // Handle other fragments as needed
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceState()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.clIconMonitor -> {
                viewModel.navigateToPage(0)
            }
            binding.clIconGallery -> {
                viewModel.navigateToPage(1)
            }
            binding.clIconReport -> {
                viewModel.navigateToPage(3)
            }
            binding.clIconMine -> {
                viewModel.navigateToPage(4)
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
        viewModel.handleGuideDialog { step, navigationTarget ->
            when (step) {
                1 -> viewModel.navigateToPage(0)
                2 -> viewModel.navigateToPage(4) 
                3 -> viewModel.navigateToPage(2)
            }
            
            val guideDialog = HomeGuideDialog(this, step)
            guideDialog.onNextClickListener = { nextStep ->
                viewModel.handleGuideNavigation(nextStep)
                when (nextStep) {
                    1 -> {
                        viewModel.navigateToPage(4)
                        if (Build.VERSION.SDK_INT < 31) {
                            lifecycleScope.launch {
                                delay(100)
                                guideDialog.blurBg(binding.clRoot)
                            }
                        }
                    }
                    2 -> {
                        viewModel.navigateToPage(2)
                        if (Build.VERSION.SDK_INT < 31) {
                            lifecycleScope.launch {
                                delay(100)
                                guideDialog.blurBg(binding.clRoot)
                            }
                        }
                    }
                    3 -> {
                        // Guide completed
                    }
                }
            }
            guideDialog.onSkinClickListener = {
                viewModel.completeGuide()
            }
            guideDialog.setOnDismissListener {
                if (Build.VERSION.SDK_INT >= 31) {
                    window?.decorView?.setRenderEffect(null)
                }
            }
            guideDialog.show()

            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(
                    RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR)
                )
            } else {
                lifecycleScope.launch {
                    delay(100)
                    guideDialog.blurBg(binding.clRoot)
                }
            }
        }
        }
    }

    // ViewPager adapter remains as inner class for fragment management
    private inner class ViewPagerAdapter(fa: FragmentActivity, private val isTC007: Boolean) :
        FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> IRThermalFragment.newInstance(isTC007)
                1 -> IRGalleryTabFragment()
                2 -> AbilityFragment()
                3 -> PDFListFragment()
                4 -> MoreFragment.newInstance(isTC007)
                else -> IRThermalFragment.newInstance(isTC007)
            }
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
