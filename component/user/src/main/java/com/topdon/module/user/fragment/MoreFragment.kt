package com.topdon.module.user.fragment

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.ui.SettingNightView
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.event.TS004ResetEvent
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.common.WifiSaveSettingUtil
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.ConfirmSelectDialog
import com.topdon.lib.core.dialog.FirmwareUpDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.http.tool.DownloadTool
import com.topdon.lib.core.repository.ProductBean
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.lib.core.viewmodel.FirmwareViewModel
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.user.R
import com.topdon.lib.core.R as RCore
import com.topdon.module.user.dialog.DownloadProDialog
import com.topdon.module.user.dialog.FirmwareInstallDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.text.DecimalFormat

/**
 * 插件式 “更多” 页面
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007
 */
// Legacy ARouter route annotation - now using NavigationManager
class MoreFragment : BaseFragment(), View.OnClickListener {

    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false
    /**
     * TC007 固件升级 ViewModel.
     */
    private val firmwareViewModel: FirmwareViewModel by viewModels()
    
    // View references
    private lateinit var settingItemModel: View
    private lateinit var settingItemCorrection: View
    private lateinit var settingItemDual: View
    private lateinit var settingItemUnit: View
    private lateinit var settingVersion: View
    private lateinit var settingDeviceInformation: SettingNightView
    private lateinit var settingReset: SettingNightView
    private lateinit var settingItemConfigSelect: androidx.appcompat.widget.SwitchCompat
    private lateinit var tvUpgradePoint: TextView
    private lateinit var itemSettingBottomText: TextView
    private lateinit var tvRightText: TextView

    override fun initContentView() = R.layout.fragment_more

    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false
        
        // Initialize views
        settingItemModel = requireView().findViewById(R.id.setting_item_model)
        settingItemCorrection = requireView().findViewById(R.id.setting_item_correction)  
        settingItemDual = requireView().findViewById(R.id.setting_item_dual)
        settingItemUnit = requireView().findViewById(R.id.setting_item_unit)
        settingVersion = requireView().findViewById(R.id.setting_version)
        settingDeviceInformation = requireView().findViewById(R.id.setting_device_information)
        settingReset = requireView().findViewById(R.id.setting_reset)
        settingItemConfigSelect = requireView().findViewById(R.id.setting_item_config_select)
        tvUpgradePoint = requireView().findViewById(R.id.tv_upgrade_point)
        itemSettingBottomText = requireView().findViewById(R.id.item_setting_bottom_text)
        tvRightText = requireView().findViewById(R.id.tv_right_text)

        settingItemModel.setOnClickListener(this)//温度修正
        settingItemCorrection.setOnClickListener(this)//图像校正
        settingItemDual.setOnClickListener(this)//双光校正
        settingItemUnit.setOnClickListener(this)//温度单温
        settingVersion.setOnClickListener(this) //TC007固件升级
        settingDeviceInformation.setOnClickListener(this)//TC007设备信息
        settingReset.setOnClickListener(this)//TC007恢复出厂设置

        //根据 2024/5/23 评审会结论，TC007没有多少需要恢复出厂的配置，产品决定砍掉
        settingReset.isVisible = false

        settingVersion.isVisible = isTC007 && Build.VERSION.SDK_INT >= 29
        settingDeviceInformation.isVisible = isTC007
        settingItemDual.isVisible = !isTC007 && DeviceTools.isTC001PlusConnect()

        if (isTC007) {
            refresh07Connect(WebSocketProxy.getInstance().isTC007Connect())
        }

        val settingItemAutoShow = requireView().findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.setting_item_auto_show)
        settingItemAutoShow.isChecked = if (isTC007) SharedManager.isConnect07AutoOpen else SharedManager.isConnectAutoOpen
        settingItemAutoShow.setOnCheckedChangeListener { _, isChecked ->
            if (isTC007) {
                SharedManager.isConnect07AutoOpen = isChecked
            } else {
                SharedManager.isConnectAutoOpen = isChecked
            }
        }

        settingItemConfigSelect.isChecked = if (isTC007) WifiSaveSettingUtil.isSaveSetting else SaveSettingUtil.isSaveSetting
        settingItemConfigSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                TipDialog.Builder(requireContext())
                    .setMessage(RCore.string.save_setting_tips)
                    .setPositiveListener(RCore.string.app_ok) {
                        if (isTC007){
                            WifiSaveSettingUtil.isSaveSetting = true
                        }else{
                            SaveSettingUtil.isSaveSetting = true
                        }
                    }
                    .setCancelListener(RCore.string.app_cancel) {
                        settingItemConfigSelect.isChecked = false
                    }
                    .setCanceled(false)
                    .create().show()
            } else {
                if (isTC007){
                    WifiSaveSettingUtil.reset()
                    WifiSaveSettingUtil.isSaveSetting = false
                }else{
                    SaveSettingUtil.reset()
                    SaveSettingUtil.isSaveSetting = false
                }
            }
        }

        firmwareViewModel.firmwareDataLD.observe(this) {
            tvUpgradePoint.isVisible = it != null
            dismissLoadingDialog()
            if (it == null) {//请求成功但没有固件升级包，即已是最新
                ToastUtils.showShort(RCore.string.setting_firmware_update_latest_version)
            } else {
                showFirmwareUpDialog(it)
            }
        }
        firmwareViewModel.failLD.observe(this) {
            dismissLoadingDialog()
            TToast.shortToast(requireContext(), if (it) RCore.string.upgrade_bind_error else RCore.string.operation_failed_tips)
            tvUpgradePoint.isVisible = false
        }
    }

    override fun initData() {
    }

    override fun connected() {
        settingItemDual.isVisible = !isTC007 && DeviceTools.isTC001PlusConnect()
    }

    override fun disConnected() {
        settingItemDual.isVisible = false
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            refresh07Connect(true)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (!isTS004 && isTC007) {
            refresh07Connect(false)
        }
    }

    override fun onClick(v: View?) {
       when(v){
           settingItemModel -> {//温度修正
               NavigationManager.getInstance().build(RouterConfig.IR_SETTING).withBoolean(ExtraKeyConfig.IS_TC007, isTC007).navigation(requireContext())
           }
           settingItemDual->{
               NavigationManager.getInstance().build(RouterConfig.MANUAL_START).navigation(requireContext())
           }
           settingItemUnit -> {//温度单位
               NavigationManager.getInstance().build(RouterConfig.UNIT).navigation(requireContext())
           }
           settingItemCorrection->{//锅盖校正
               NavigationManager.getInstance().build(RouterConfig.IR_CORRECTION).withBoolean(ExtraKeyConfig.IS_TC007, isTC007).navigation(requireContext())
           }
           settingVersion -> {//TC007固件升级
               //由于双通道方案存在问题，V3.30临时使用 apk 内置固件升级包，此处注释强制登录逻辑
//               if (LMS.getInstance().isLogin) {
                   val firmwareData = firmwareViewModel.firmwareDataLD.value
                   if (firmwareData != null) {
                       showFirmwareUpDialog(firmwareData)
                   } else {
                       XLog.i("TC007 固件升级 - 点击查询")
                       showLoadingDialog()
                       firmwareViewModel.queryFirmware(false)
                   }
//               } else {
//                   LMS.getInstance().activityLogin()
//               }
           }
           settingDeviceInformation -> {//TC007设备信息
               if (WebSocketProxy.getInstance().isTC007Connect()) {
                   NavigationManager.getInstance()
                       .build(RouterConfig.DEVICE_INFORMATION)
                       .withBoolean(ExtraKeyConfig.IS_TC007, true)
                       .navigation(requireContext())
               }
           }
           settingReset -> {//TC007恢复出厂设置
               if (WebSocketProxy.getInstance().isTC007Connect()) {
                   restoreFactory()
               }
           }
       }
    }


    /**
     * 仅 TC007 页面时，刷新连接或未连接状态.
     */
    private fun refresh07Connect(isConnect: Boolean) {
        settingDeviceInformation.isRightArrowVisible = isConnect
        settingDeviceInformation.setRightTextId(if (isConnect) 0 else RCore.string.app_no_connect)
        settingReset.isRightArrowVisible = isConnect
        settingReset.setRightTextId(if (isConnect) 0 else RCore.string.app_no_connect)
        tvRightText.isVisible = isConnect

        if (isConnect) {
            lifecycleScope.launch {
                val productBean: ProductBean? = TC007Repository.getProductInfo()
                if (productBean == null) {
                    TToast.shortToast(requireContext(), RCore.string.operation_failed_tips)
                } else {
                    itemSettingBottomText.text = getString(RCore.string.setting_firmware_update_version) + "V" + productBean.getVersionStr()
                }
            }
        } else {
            itemSettingBottomText.setText(RCore.string.setting_firmware_update_version)
        }
    }


    /**
     * 显示固件升级提示弹框.
     */
    private fun showFirmwareUpDialog(firmwareData: FirmwareViewModel.FirmwareData) {
        val dialog = FirmwareUpDialog(requireContext())
        dialog.titleStr = "${getString(RCore.string.update_new_version)} ${firmwareData.version}"
        dialog.sizeStr = "${getString(RCore.string.detail_len)}: ${getFileSizeStr(firmwareData.size)}"
        dialog.contentStr = firmwareData.updateStr
        dialog.isShowRestartTips = true
        dialog.onConfirmClickListener = {
            //由于双通道方案存在问题，V3.30临时使用 apk 内置固件升级包，此处注释下载逻辑
            //downloadFirmware(firmwareData)
            installFirmware(FileConfig.getFirmwareFile(firmwareData.downUrl))
        }
        dialog.show()
    }

    private fun getFileSizeStr(size: Long): String = if (size < 1024) {
        "${size}B"
    } else if (size < 1024 * 1024) {
        DecimalFormat("#.0").format(size.toDouble() / 1024) + "KB"
    } else if (size < 1024 * 1024 * 1024) {
        DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024) + "MB"
    } else {
        DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024 / 1024) + "GB"
    }

    /**
     * 下载指定固件升级包
     */
    private fun downloadFirmware(firmwareData: FirmwareViewModel.FirmwareData) {
        lifecycleScope.launch {
            val progressDialog = DownloadProDialog(requireContext())
            progressDialog.show()

            val file = File(requireContext().getExternalFilesDir("firmware"), "TC007${firmwareData.version}.zip")
            val isSuccess = DownloadTool.download(firmwareData.downUrl, file) { current, total ->
                progressDialog.refreshProgress(current, total)
            }
            progressDialog.dismiss()
            if (isSuccess) {
                installFirmware(file)
            } else {
                showReDownloadDialog(firmwareData)
            }
        }
    }

    private fun installFirmware(file: File) {
        lifecycleScope.launch {
            XLog.d("TC007 固件升级 - 开始安装固件升级包")
            val installDialog = FirmwareInstallDialog(requireContext())
            installDialog.show()

            val isSuccess = TC007Repository.updateFirmware(file)
            installDialog.dismiss()
            if (isSuccess) {
                XLog.d("TC007 固件升级 - 固件升级包发送往 TC007 成功，即将断开连接")
                (requireActivity().application as BaseApplication).disconnectWebSocket()
                TipDialog.Builder(requireContext())
                    .setTitleMessage(getString(RCore.string.app_tip))
                    .setMessage(RCore.string.firmware_up_success)
                    .setPositiveListener(RCore.string.app_confirm) {
                        NavigationManager.getInstance().build(RouterConfig.MAIN).navigation(requireContext())
                        requireActivity().finish()
                    }
                    .setCancelListener(RCore.string.app_cancel) {

                    }
                    .create().show()
            } else {
                XLog.w("TC007 固件升级 - 固件升级包发送往 TC007 失败!")
                showReInstallDialog(file)
            }
        }
    }

    private fun showReInstallDialog(file: File) {
        val dialog = ConfirmSelectDialog(requireContext())
        dialog.setShowIcon(true)
        dialog.setTitleRes(RCore.string.ts004_install_tips)
        dialog.setCancelText(RCore.string.ts004_install_cancel)
        dialog.setConfirmText(RCore.string.ts004_install_continue)
        dialog.onConfirmClickListener = {
            installFirmware(file)
        }
        dialog.show()
    }

    private fun showReDownloadDialog(firmwareData: FirmwareViewModel.FirmwareData) {
        val dialog = ConfirmSelectDialog(requireContext())
        dialog.setShowIcon(true)
        dialog.setTitleRes(RCore.string.ts004_download_tips)
        dialog.setCancelText(RCore.string.ts004_download_cancel)
        dialog.setConfirmText(RCore.string.ts004_download_continue)
        dialog.onConfirmClickListener = {
            downloadFirmware(firmwareData)
        }
        dialog.show()
    }


    private fun restoreFactory() {
        TipDialog.Builder(requireContext())
            .setTitleMessage(getString(RCore.string.ts004_reset_tip1, "TC007"))
            .setMessage(getString(RCore.string.ts004_reset_tip2))
            .setPositiveListener(RCore.string.app_ok) {
                resetAll()
            }
            .setCancelListener(RCore.string.app_cancel) {
            }
            .setCanceled(true)
            .create().show()
    }


    private fun resetAll() {
        showLoadingDialog(RCore.string.ts004_reset_tip3)
        lifecycleScope.launch {
            val isSuccess = TC007Repository.resetToFactory()
            if (isSuccess) {
                XLog.d("TC007 恢复出厂设置成功，即将断开连接")
                TToast.shortToast(requireContext(), RCore.string.ts004_reset_tip4)
                (requireActivity().application as BaseApplication).disconnectWebSocket()
                EventBus.getDefault().post(TS004ResetEvent())
                NavigationManager.getInstance().build(RouterConfig.MAIN).navigation(requireContext())
                requireActivity().finish()
            } else {
                TToast.shortToast(requireContext(), RCore.string.operation_failed_tips)
            }
            delay(500)
            dismissLoadingDialog()
        }
    }
}
