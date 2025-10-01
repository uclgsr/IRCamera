package com.mpdc4gsr.module.user.fragment

// TS004ResetEvent removed
// import com.mpdc4gsr.libunified.app.bean.event.TS004ResetEvent
// ProductBean functionality removed
// import com.mpdc4gsr.libunified.app.repository.ProductBean

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.common.WifiSaveSettingUtils
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.ConfirmSelectDialog
import com.mpdc4gsr.libunified.app.dialog.FirmwareUpDialog
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.http.tool.DownloadTool
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.app.viewmodel.FirmwareViewModel
import com.mpdc4gsr.libunified.ui.SettingNightView
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.module.user.dialog.DownloadProDialog
import com.mpdc4gsr.module.user.dialog.FirmwareInstallDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import com.mpdc4gsr.libunified.R as RCore


class MoreFragment : BaseFragment(), View.OnClickListener {

    private var isTC007 = false

    private val firmwareViewModel: FirmwareViewModel by viewModels()

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

        settingItemModel.setOnClickListener(this)
        settingItemCorrection.setOnClickListener(this)
        settingItemDual.setOnClickListener(this)
        settingItemUnit.setOnClickListener(this)
        settingVersion.setOnClickListener(this)
        settingDeviceInformation.setOnClickListener(this)
        settingReset.setOnClickListener(this)

        setupObservers()
    }

    private fun setupObservers() {
        settingItemConfigSelect.isChecked =
            if (isTC007) WifiSaveSettingUtils.isSaveSetting else SaveSettingUtils.isSaveSetting
        settingItemConfigSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                TipDialog.Builder(requireContext())
                    .setMessage(RCore.string.save_setting_tips)
                    .setPositiveListener(RCore.string.app_ok) {
                        if (isTC007) {
                            WifiSaveSettingUtils.isSaveSetting = true
                        } else {
                            SaveSettingUtils.isSaveSetting = true
                        }
                    }
                    .setCancelListener(RCore.string.app_cancel) {
                        settingItemConfigSelect.isChecked = false
                    }
                    .setCanceled(false)
                    .create().show()
            } else {
                if (isTC007) {
                    WifiSaveSettingUtils.reset()
                    WifiSaveSettingUtils.isSaveSetting = false
                } else {
                    SaveSettingUtils.reset()
                    SaveSettingUtils.isSaveSetting = false
                }
            }
        }

        firmwareViewModel.firmwareDataLD.observe(this) {
            tvUpgradePoint.isVisible = it != null
            dismissLoadingDialog()
            if (it == null) {
                ToastUtils.showShort(RCore.string.setting_firmware_update_latest_version)
            } else {
                showFirmwareUpDialog(it)
            }
        }
        firmwareViewModel.failLD.observe(this) {
            dismissLoadingDialog()
            TToast.shortToast(
                requireContext(),
                if (it) RCore.string.upgrade_bind_error else RCore.string.operation_failed_tips
            )
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
        when (v) {
            settingItemModel -> {
                NavigationManager.getInstance().build(
                    RouterConfig.IR_SETTING,
                ).withBoolean(ExtraKeyConfig.IS_TC007, isTC007).navigation(requireContext())
            }

            settingItemDual -> {
                NavigationManager.getInstance().build(RouterConfig.MANUAL_START)
                    .navigation(requireContext())
            }

            settingItemUnit -> {
                NavigationManager.getInstance().build(RouterConfig.UNIT)
                    .navigation(requireContext())
            }

            settingItemCorrection -> {
                NavigationManager.getInstance().build(
                    RouterConfig.IR_CORRECTION,
                ).withBoolean(ExtraKeyConfig.IS_TC007, isTC007).navigation(requireContext())
            }

            settingVersion -> {
                val firmwareData = firmwareViewModel.firmwareDataLD.value
                if (firmwareData != null) {
                    showFirmwareUpDialog(firmwareData)
                } else {
                    XLog.i("TC007 firmware check")
                    showLoadingDialog()
                    firmwareViewModel.queryFirmware(false)
                }
            }

            settingDeviceInformation -> {
                if (WebSocketProxy.getInstance().isTC007Connect()) {
                    NavigationManager.getInstance()
                        .build(RouterConfig.DEVICE_INFORMATION)
                        .withBoolean(ExtraKeyConfig.IS_TC007, true)
                        .navigation(requireContext())
                }
            }

            settingReset -> {
                if (WebSocketProxy.getInstance().isTC007Connect()) {
                    restoreFactory()
                }
            }
        }
    }

    private fun refresh07Connect(isConnect: Boolean) {
        settingDeviceInformation.isRightArrowVisible = isConnect
        settingDeviceInformation.setRightTextId(if (isConnect) 0 else RCore.string.app_no_connect)
        settingReset.isRightArrowVisible = isConnect
        settingReset.setRightTextId(if (isConnect) 0 else RCore.string.app_no_connect)
        tvRightText.isVisible = isConnect

        if (isConnect) {
            lifecycleScope.launch {
                // TC007Repository functionality removed
                TToast.shortToast(requireContext(), RCore.string.operation_failed_tips)
            }
        } else {
            itemSettingBottomText.setText(RCore.string.setting_firmware_update_version)
        }
    }

    private fun showFirmwareUpDialog(firmwareData: FirmwareViewModel.FirmwareData) {
        val dialog = FirmwareUpDialog(requireContext())
        dialog.titleStr = "${getString(RCore.string.update_new_version)} ${firmwareData.version}"
        dialog.sizeStr =
            "${getString(RCore.string.detail_len)}: ${getFileSizeStr(firmwareData.size)}"
        dialog.contentStr = firmwareData.updateStr
        dialog.isShowRestartTips = true
        dialog.onConfirmClickListener = {
            installFirmware(FileConfig.getFirmwareFile(firmwareData.downUrl))
        }
        dialog.show()
    }

    private fun getFileSizeStr(size: Long): String =
        if (size < 1024) {
            "${size}B"
        } else if (size < 1024 * 1024) {
            DecimalFormat("#.0").format(size.toDouble() / 1024) + "KB"
        } else if (size < 1024 * 1024 * 1024) {
            DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024) + "MB"
        } else {
            DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024 / 1024) + "GB"
        }

    private fun downloadFirmware(firmwareData: FirmwareViewModel.FirmwareData) {
        lifecycleScope.launch {
            val progressDialog = DownloadProDialog(requireContext())
            progressDialog.show()

            val file = File(
                requireContext().getExternalFilesDir("firmware"),
                "TC007${firmwareData.version}.zip"
            )
            val isSuccess =
                DownloadTool.download(firmwareData.downUrl, file) { current, total ->
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
            XLog.d("TC007 firmware install")
            val installDialog = FirmwareInstallDialog(requireContext())
            installDialog.show()

            // TC007Repository functionality removed
            val isSuccess = false
            installDialog.dismiss()
            if (isSuccess) {
                XLog.d("TC007 firmware install success")
                (requireActivity().application as BaseApplication).disconnectWebSocket()
                TipDialog.Builder(requireContext())
                    .setTitleMessage(getString(RCore.string.app_tip))
                    .setMessage(RCore.string.firmware_up_success)
                    .setPositiveListener(RCore.string.app_confirm) {
                        NavigationManager.getInstance().build(RouterConfig.MAIN)
                            .navigation(requireContext())
                        requireActivity().finish()
                    }
                    .setCancelListener(RCore.string.app_cancel) {
                    }
                    .create().show()
            } else {
                XLog.w("TC007 firmware install failed")
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
            // TC007Repository functionality removed
            val isSuccess = false
            if (isSuccess) {
                XLog.d("TC007 reset success")
                TToast.shortToast(requireContext(), RCore.string.ts004_reset_tip4)
                (requireActivity().application as BaseApplication).disconnectWebSocket()
                // EventBus.getDefault().post(TS004ResetEvent()) // TS004ResetEvent removed
                NavigationManager.getInstance().build(RouterConfig.MAIN)
                    .navigation(requireContext())
                requireActivity().finish()
            } else {
                TToast.shortToast(requireContext(), RCore.string.operation_failed_tips)
            }
            delay(500)
            dismissLoadingDialog()
        }
    }
}
