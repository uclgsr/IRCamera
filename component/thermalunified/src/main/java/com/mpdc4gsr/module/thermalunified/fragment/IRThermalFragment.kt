package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.IRThermalNightActivity
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusActivity
import com.mpdc4gsr.module.thermalunified.viewmodel.IRThermalFragmentViewModel
import kotlinx.coroutines.launch

class IRThermalFragment : BaseFragment(), View.OnClickListener {

    private val viewModel: IRThermalFragmentViewModel by viewModels()

    private var isTC007 = false

    private lateinit var titleView: com.mpdc4gsr.libunified.app.view.TitleView
    private lateinit var clOpenThermal: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tvMainEnter: android.widget.TextView
    private lateinit var cl07ConnectTips: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tv07Connect: android.widget.TextView
    private lateinit var animationView: com.airbnb.lottie.LottieAnimationView
    private lateinit var clNotConnect: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var clConnect: androidx.constraintlayout.widget.ConstraintLayout

    override fun initContentView() = R.layout.fragment_thermal_ir

    override fun initView() {
        titleView = requireView().findViewById(R.id.title_view)
        clOpenThermal = requireView().findViewById(R.id.cl_open_thermal)
        tvMainEnter = requireView().findViewById(R.id.tv_main_enter)
        cl07ConnectTips = requireView().findViewById(R.id.cl_07_connect_tips)
        tv07Connect = requireView().findViewById(R.id.tv_07_connect)
        animationView = requireView().findViewById(R.id.animation_view)
        clNotConnect = requireView().findViewById(R.id.cl_not_connect)
        clConnect = requireView().findViewById(R.id.cl_connect)

        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        titleView.setTitleText(if (isTC007) "TC007" else getString(R.string.tc_has_line_device))

        clOpenThermal.setOnClickListener(this)
        tvMainEnter.setOnClickListener(this)
        cl07ConnectTips.setOnClickListener(this)
        tv07Connect.setOnClickListener(this)

        tvMainEnter.isVisible = !isTC007
        cl07ConnectTips.isVisible = isTC007
        tv07Connect.isVisible = isTC007

        setupAnimation()
        setupObservers()
        setupLifecycleObserver()

        // Initial device state check
        viewModel.checkDeviceConnection(isTC007)
    }

    private fun setupAnimation() {
        if (isTC007) {
            animationView.setAnimation("TC007AnimationJSON.json")
        } else {
            animationView.setAnimation("TDAnimationJSON.json")
        }
    }

    private fun setupObservers() {
        // Device connection state observer
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                updateConnectionUI(uiState)
            }
        }

        // Navigation events observer
        viewModel.navigationEvent.observe(viewLifecycleOwner) { navigationEvent ->
            handleNavigationEvent(navigationEvent)
        }

        // Thermal action events observer
        viewModel.thermalAction.observe(viewLifecycleOwner) { action ->
            handleThermalAction(action)
        }

        // Permission state observer
        viewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            handlePermissionState(permissionState)
        }
    }

    private fun setupLifecycleObserver() {
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    if (WebSocketProxy.getInstance().isConnected()) {
                        NetWorkUtils.switchNetwork(true)
                    } else {
                        NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                    }

                    // Refresh device connection state on resume
                    viewModel.checkDeviceConnection(isTC007)
                }
            }
        )
    }

    private fun updateConnectionUI(uiState: IRThermalFragmentViewModel.ThermalUIState) {
        clConnect.isVisible = uiState.isConnected
        clNotConnect.isVisible = !uiState.isConnected
    }

    override fun initData() {
        // Data initialization handled in ViewModel
    }

    override fun onResume() {
        super.onResume()
        // Device connection check handled in lifecycle observer
    }

    override fun connected() {
        viewModel.onDeviceConnected(isTC007)
    }

    override fun disConnected() {
        viewModel.onDeviceDisconnected()
    }

    override fun onSocketConnected(isTS004: Boolean) {
        viewModel.onSocketConnected(isTS004, isTC007)
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        viewModel.onSocketDisConnected(isTS004, isTC007)
    }

    private fun handleNavigationEvent(event: IRThermalFragmentViewModel.NavigationEvent) {
        when (event) {
            is IRThermalFragmentViewModel.NavigationEvent.NavigateToTC007Thermal -> {
                NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_07)
                    .navigation(requireContext())
            }

            is IRThermalFragmentViewModel.NavigationEvent.StartThermalPlusActivity -> {
                startActivityForResult(
                    Intent(requireContext(), IRThermalPlusActivity::class.java), 101
                )
            }

            is IRThermalFragmentViewModel.NavigationEvent.NavigateToTCLite -> {
                NavigationManager.getInstance().build(RouterConfig.IR_TCLITE)
                    .navigation(requireContext(), 101)
            }

            is IRThermalFragmentViewModel.NavigationEvent.NavigateToHikMain -> {
                NavigationManager.getInstance().build(RouterConfig.IR_HIK_MAIN)
                    .navigation(requireContext())
            }

            is IRThermalFragmentViewModel.NavigationEvent.StartThermalNightActivity -> {
                startActivityForResult(
                    Intent(requireContext(), IRThermalNightActivity::class.java), 101
                )
            }
        }
    }

    private fun handleThermalAction(action: IRThermalFragmentViewModel.ThermalAction) {
        when (action) {
            is IRThermalFragmentViewModel.ThermalAction.ShowDeviceConnectTip -> {
                activity?.let { activity ->
                    TipDialog.Builder(activity)
                        .setMessage(R.string.device_connect_tip)
                        .setPositiveListener(R.string.app_confirm)
                        .create().show()
                }
            }

            is IRThermalFragmentViewModel.ThermalAction.ShowConnectTip -> {
                showConnectTip()
            }

            is IRThermalFragmentViewModel.ThermalAction.ShowPermissionSettingsTip -> {
                context?.let { context ->
                    TipDialog.Builder(context)
                        .setTitleMessage(getString(R.string.app_tip))
                        .setMessage(getString(R.string.app_camera_content))
                        .setPositiveListener(R.string.app_open) {
                            AppUtils.launchAppDetailsSettings()
                        }
                        .setCancelListener(R.string.app_cancel) {
                        }
                        .setCanceled(true)
                        .create().show()
                }
            }
        }
    }

    private fun handlePermissionState(state: IRThermalFragmentViewModel.PermissionState) {
        when (state) {
            is IRThermalFragmentViewModel.PermissionState.RequestCameraPermission -> {
                requestCameraPermission()
            }

            else -> {
                // Other permission states handled by callbacks
            }
        }
    }

    private fun requestCameraPermission() {
        XXPermissions.with(requireContext())
            .permission(listOf(Permission.CAMERA))
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (allGranted) {
                        viewModel.onPermissionGranted()
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    viewModel.onPermissionDenied(doNotAskAgain)
                }
            })
    }

    override fun onClick(v: View?) {
        when (v) {
            clOpenThermal -> {
                viewModel.handleThermalOpen(isTC007)
            }

            tvMainEnter -> {
                viewModel.handleMainEnter()
            }

            cl07ConnectTips -> {
                NavigationManager.getInstance().build(RouterConfig.IR_CONNECT_TIPS)
                    .withBoolean(ExtraKeyConfig.IS_TC007, true)
                    .navigation(requireContext())
            }

            tv07Connect -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_DEVICE_ADD)
                    .withBoolean("isTS004", false)
                    .navigation(requireContext())
            }
        }
    }

    // Remaining methods maintain original functionality but are now better organized
    private var tipConnectDialog: TipDialog? = null
    private var isCancelUpdateVersion = false

    private fun showConnectTip() {

        if (requireContext().applicationInfo.targetSdkVersion >= Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
        ) {
            if (isCancelUpdateVersion) {
                return
            }
            if (tipConnectDialog != null && tipConnectDialog!!.isShowing) {
                return
            }
            tipConnectDialog =
                TipDialog.Builder(requireContext())
                    .setMessage(getString(R.string.tip_target_sdk))
                    .setPositiveListener(R.string.app_confirm) {
                        val url = "https://www.topdon.com/pages/pro-down?fuzzy=TS001"
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    }.setCancelListener(R.string.app_cancel, {
                        isCancelUpdateVersion = true
                    })
                    .create()
            tipConnectDialog?.show()
        }
    }

    private fun checkStoragePermission() {
        val permissionList: List<String> =
            if (activity?.applicationInfo?.targetSdkVersion!! >= 34) {
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else if (activity?.applicationInfo?.targetSdkVersion!! >= 33) {
                listOf(
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.WRITE_EXTERNAL_STORAGE,
                )
            } else {
                listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
            }

        if (!XXPermissions.isGranted(requireContext(), permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                context?.let { context ->
                    TipDialog.Builder(context)
                        .setMessage(
                            getString(
                                R.string.permission_request_storage_app,
                                CommUtils.getAppName()
                            )
                        )
                        .setCancelListener(R.string.app_cancel)
                        .setPositiveListener(R.string.app_confirm) {
                            initStoragePermission(permissionList)
                        }
                        .create().show()
                }
            } else {
                initStoragePermission(permissionList)
            }
        } else {
            initStoragePermission(permissionList)
        }
    }

    private fun initStoragePermission(permissionList: List<String>) {
    }
}
