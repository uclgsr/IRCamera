package com.topdon.module.thermal.ir.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.activity.IRThermalNightActivity
import com.topdon.module.thermal.ir.activity.IRThermalPlusActivity

class IRThermalFragment : BaseFragment(), View.OnClickListener {

    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false

    // View declarations
    private lateinit var titleView: com.topdon.lib.core.view.TitleView
    private lateinit var clOpenThermal: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tvMainEnter: android.widget.TextView
    private lateinit var cl07ConnectTips: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tv07Connect: android.widget.TextView
    private lateinit var animationView: com.airbnb.lottie.LottieAnimationView
    private lateinit var clNotConnect: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var clConnect: androidx.constraintlayout.widget.ConstraintLayout

    override fun initContentView() = R.layout.fragment_thermal_ir

    override fun initView() {
        // Initialize views
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

        if (isTC007) {
            animationView.setAnimation("TC007AnimationJSON.json")
            clNotConnect.isVisible = !WebSocketProxy.getInstance().isTC007Connect()
            clConnect.isVisible = WebSocketProxy.getInstance().isTC007Connect()
        } else {
            animationView.setAnimation("TDAnimationJSON.json")
            checkConnect()
        }
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // 要是当前已连接 TS004、TC007，切到流量上，不然登录注册意见反馈那些没网
                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.switchNetwork(true)
                }else{
                    NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                }
            }
        })
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        if (!isTC007) {
            checkConnect()
        }
    }

    override fun connected() {
        SharedManager.hasTcLine = true
        if (!isTC007) {
            clConnect.isVisible = true
            clNotConnect.isVisible = false
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            clConnect.isVisible = false
            clNotConnect.isVisible = true
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            clConnect.isVisible = true
            clNotConnect.isVisible = false
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            clConnect.isVisible = false
            clNotConnect.isVisible = true
        }
    }

    /**
     * 主动检测连接设备
     */
    private fun checkConnect() {
        if (DeviceTools.isConnect(isAutoRequest = false)) {
            connected()
        } else {
            disConnected()
            if (DeviceTools.findUsbDevice() != null) {//找到设备,但不能连接
                showConnectTip()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            clOpenThermal -> {
                if (isTC007) {
                    NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_07).navigation(requireContext())
                } else {
                    if (DeviceTools.isTC001PlusConnect()) {
                        startActivityForResult(Intent(requireContext(), IRThermalPlusActivity::class.java), 101)
                    }else if(DeviceTools.isTC001LiteConnect()){
                        NavigationManager.getInstance().build(RouterConfig.IR_TCLITE).navigation(requireActivity(),101)
                    } else if (DeviceTools.isHikConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_HIK_MAIN).navigation(requireActivity())
                    } else {
                        startActivityForResult(Intent(requireContext(), IRThermalNightActivity::class.java), 101)
                    }
                }
            }
            tvMainEnter -> {
                if (!DeviceTools.isConnect()) {
                    //没有接入设备不需要提示，有系统授权提示框
                    if (DeviceTools.findUsbDevice() == null) {
                        activity?.let {
                            TipDialog.Builder(it)
                                .setMessage(R.string.device_connect_tip)
                                .setPositiveListener(R.string.app_confirm)
                                .create().show()
                        }
                    } else {
                        XXPermissions.with(this)
                            .permission(listOf(
                                Permission.CAMERA
                            ))
                            .request(object : OnPermissionCallback {
                                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                                    if (allGranted) {
                                        showConnectTip()
                                    }
                                }

                                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                                    if (doNotAskAgain) {
                                        //拒绝授权并且不再提醒
                                        context?.let {
                                            TipDialog.Builder(it)
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
                            })
                    }
                }
            }
            cl07ConnectTips -> {//TC007 连接提示
                NavigationManager.getInstance().build(RouterConfig.IR_CONNECT_TIPS)
                    .withBoolean(ExtraKeyConfig.IS_TC007, true)
                    .navigation(requireContext())
            }
            tv07Connect -> {//TC007 连接设备
                NavigationManager.getInstance()
                    .build(RouterConfig.IR_DEVICE_ADD)
                    .withBoolean("isTS004", false)
                    .navigation(requireContext())
            }
        }
    }

    private var tipConnectDialog: TipDialog? = null

    private var isCancelUpdateVersion = false
    // 针对android10 usb连接问题,提供android 27版本
    private fun showConnectTip() {
        // targetSdk高于27且android os为10
        if (requireContext().applicationInfo.targetSdkVersion >= Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
        ) {
            if (isCancelUpdateVersion) {
                return
            }
            if (tipConnectDialog != null && tipConnectDialog!!.isShowing) {
                return
            }
            tipConnectDialog = TipDialog.Builder(requireContext())
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
        val permissionList: List<String> = if (activity?.applicationInfo?.targetSdkVersion!! >= 34){
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else if (activity?.applicationInfo?.targetSdkVersion!! >= 33) {
            listOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            listOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }

        if (!XXPermissions.isGranted(requireContext(), permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                context?.let {
                    TipDialog.Builder(it)
                        .setMessage(getString(R.string.permission_request_storage_app, CommUtils.getAppName()))
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

    /**
     * 动态申请权限
     */
    private fun initStoragePermission(permissionList: List<String>) {

    }



}
