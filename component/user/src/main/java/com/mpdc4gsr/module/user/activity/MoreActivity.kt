package com.mpdc4gsr.module.user.activity

// TS004ResetEvent removed
// import com.mpdc4gsr.libunified.app.bean.event.TS004ResetEvent
// TS004Repository functionality removed
// import com.mpdc4gsr.libunified.app.repository.TS004Repository
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.ConfirmSelectDialog
import com.mpdc4gsr.libunified.app.dialog.FirmwareUpDialog
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.http.tool.DownloadTool
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.libunified.app.viewmodel.FirmwareViewModel
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.module.user.dialog.DownloadProDialog
import com.mpdc4gsr.module.user.dialog.FirmwareInstallDialog
import com.kotlinx.coroutines.delay
import com.kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import com.mpdc4gsr.libunified.R as RCore

class MoreActivity : BaseActivity(), View.OnClickListener {
    private val firmwareViewModel: FirmwareViewModel by viewModels()

    private lateinit var settingDeviceInformation: View
    private lateinit var settingTisr: View
    private lateinit var settingStorageSpace: View
    private lateinit var settingReset: View
    private lateinit var settingVersion: View
    private lateinit var settingDisconnect: View
    private lateinit var settingAutoSave: View
    private lateinit var itemSettingBottomText: TextView
    private lateinit var tvUpgradePoint: TextView

    override fun initContentView() = R.layout.activity_more

    override fun initView() {

        settingDeviceInformation = findViewById(R.id.setting_device_information)
        settingTisr = findViewById(R.id.setting_tisr)
        settingStorageSpace = findViewById(R.id.setting_storage_space)
        settingReset = findViewById(R.id.setting_reset)
        settingVersion = findViewById(R.id.setting_version)
        settingDisconnect = findViewById(R.id.setting_disconnect)
        settingAutoSave = findViewById(R.id.setting_auto_save)
        itemSettingBottomText = findViewById(R.id.item_setting_bottom_text)
        tvUpgradePoint = findViewById(R.id.tv_upgrade_point)

        settingDeviceInformation.setOnClickListener(this)
        settingTisr.setOnClickListener(this)
        settingStorageSpace.setOnClickListener(this)
        settingReset.setOnClickListener(this)
        settingVersion.setOnClickListener(this)
        settingDisconnect.setOnClickListener(this)
        settingAutoSave.setOnClickListener(this)



        settingVersion.isVisible = false
    }

    override fun initData() {
        updateVersion()

        firmwareViewModel.firmwareDataLD.observe(this) {
            tvUpgradePoint.isVisible = it != null
            dismissCameraLoading()
            if (it == null) {
                ToastUtils.showShort(RCore.string.setting_firmware_update_latest_version)
            } else {
                showFirmwareUpDialog(it)
            }
        }
        firmwareViewModel.failLD.observe(this) {
            dismissCameraLoading()
            TToast.shortToast(
                this,
                if (it) RCore.string.upgrade_bind_error else RCore.string.operation_failed_tips
            )
            tvUpgradePoint.isVisible = false
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            settingDeviceInformation -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.DEVICE_INFORMATION)
                    .withBoolean(ExtraKeyConfig.IS_TC007, false)
                    .navigation(this@MoreActivity)
            }

            settingTisr -> {
                NavigationManager.getInstance().build(RouterConfig.TISR)
                    .navigation(this@MoreActivity)
            }

            settingAutoSave -> {
                NavigationManager.getInstance().build(RouterConfig.AUTO_SAVE)
                    .navigation(this@MoreActivity)
            }

            settingStorageSpace -> {
                NavigationManager.getInstance().build(RouterConfig.STORAGE_SPACE)
                    .navigation(this@MoreActivity)
            }

            settingVersion -> {


                val firmwareData = firmwareViewModel.firmwareDataLD.value
                if (firmwareData != null) {
                    showFirmwareUpDialog(firmwareData)
                } else {
                    XLog.i("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph]")
                    showCameraLoading()
                    firmwareViewModel.queryFirmware(true)
                }


            }

            settingReset -> {
                restoreFactory()
            }

            settingDisconnect -> {
                NavigationManager.getInstance().build(RouterConfig.IR_MORE_HELP)
                    .withInt(Constants.SETTING_CONNECTION_TYPE, Constants.SETTING_DISCONNECTION)
                    .navigation(this@MoreActivity)
            }
        }
    }

    private fun showFirmwareUpDialog(firmwareData: FirmwareViewModel.FirmwareData) {
        val dialog = FirmwareUpDialog(this)
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
            XLog.d("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph]")
            val progressDialog = DownloadProDialog(this@MoreActivity)
            progressDialog.show()

            val file = FileConfig.getFirmwareFile("TS004${firmwareData.version}.zip")
            val isSuccess =
                DownloadTool.download(firmwareData.downUrl, file) { current, total ->
                    progressDialog.refreshProgress(current, total)
                }
            progressDialog.dismiss()
            if (isSuccess) {
                XLog.d("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph]，[ph][ph][ph][ph][ph][ph]")
                installFirmware(file)
            } else {
                XLog.w("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph]!")
                showReDownloadDialog(firmwareData)
            }
        }
    }

    private fun installFirmware(file: File) {
        lifecycleScope.launch {
            XLog.d("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph]")
            val installDialog = FirmwareInstallDialog(this@MoreActivity)
            installDialog.show()

            // TS004Repository functionality removed
            val isSuccess = false
            installDialog.dismiss()
            if (isSuccess) {
                XLog.d("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph] TS004 [ph][ph]，[ph][ph][ph][ph][ph][ph]")
                (application as BaseApplication).disconnectWebSocket()
                NavigationManager.getInstance().build(RouterConfig.MAIN)
                    .navigation(this@MoreActivity)
                finish()
            } else {
                XLog.w("TS004 [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph] TS004 [ph][ph]!")
                showReInstallDialog(file)
            }
        }
    }

    private fun showReInstallDialog(file: File) {
        val dialog = ConfirmSelectDialog(this)
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
        val dialog = ConfirmSelectDialog(this)
        dialog.setShowIcon(true)
        dialog.setTitleRes(RCore.string.ts004_download_tips)
        dialog.setCancelText(RCore.string.ts004_download_cancel)
        dialog.setConfirmText(RCore.string.ts004_download_continue)
        dialog.onConfirmClickListener = {
            downloadFirmware(firmwareData)
        }
        dialog.show()
    }

    private fun updateVersion() {
        lifecycleScope.launch {
            // TS004Repository functionality removed - show placeholder version
            itemSettingBottomText.text =
                getString(RCore.string.setting_firmware_update_version) + "V1.0.0"
        }
    }

    private fun restoreFactory() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(RCore.string.ts004_reset_tip1, "TS004"))
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
            XLog.i("[ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]")
            // TS004Repository functionality removed
            val isSuccess = false // TS004Repository.getResetAll()
            XLog.i("[ph][ph][ph][ph][ph][ph][ph][ph][ph][ph] ${if (isSuccess) "[ph][ph]" else "[ph][ph]"}")
            if (isSuccess) {
                TToast.shortToast(this@MoreActivity, RCore.string.ts004_reset_tip4)
                (application as BaseApplication).disconnectWebSocket()
                // EventBus.getDefault().post(TS004ResetEvent()) // Event removed
                NavigationManager.getInstance().build(RouterConfig.MAIN)
                    .navigation(this@MoreActivity)
                finish()
            } else {
                TToast.shortToast(this@MoreActivity, RCore.string.operation_failed_tips)
            }
            delay(500)
            dismissLoadingDialog()
        }
    }
}
