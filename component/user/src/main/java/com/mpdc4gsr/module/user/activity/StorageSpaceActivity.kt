package com.mpdc4gsr.module.user.activity

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.navigation.NavigationManager
// TS004Repository functionality removed
// import com.mpdc4gsr.lib.core.repository.TS004Repository
import com.mpdc4gsr.lms.sdk.utils.TLog
import com.mpdc4gsr.lms.sdk.weiget.TToast
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.module.user.bean.ColorsBean
import com.mpdc4gsr.module.user.view.ListItemView
import com.mpdc4gsr.module.user.view.ProgressBarView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.mpdc4gsr.lib.core.R as RCore

class StorageSpaceActivity : BaseActivity(), View.OnClickListener {

    private lateinit var tvFormatStorage: TextView
    private lateinit var tvProgressValue: TextView
    private lateinit var tvUsedValue: TextView
    private lateinit var tvUsed: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvTotal: TextView
    private lateinit var listStoragePhoto: ListItemView
    private lateinit var listStorageVideo: ListItemView
    private lateinit var listStorageSystem: ListItemView
    private lateinit var customViewProgress: ProgressBarView

    companion object {
        private fun formatFileSize(fileSize: Long): String =
            if (fileSize == 0L) {
                "0"
            } else if (fileSize < 1024) {
                DecimalFormat("#.0").format(fileSize.toDouble())
            } else if (fileSize < 1048576) {
                DecimalFormat("#.0").format(fileSize.toDouble() / 1024)
            } else if (fileSize < 1073741824) {
                DecimalFormat("#.0").format(fileSize.toDouble() / 1048576)
            } else {
                DecimalFormat("#.0").format(fileSize.toDouble() / 1073741824)
            }

        private fun getUnit(fileSize: Long): String =
            if (fileSize < 1024) {
                "B"
            } else if (fileSize < 1048576) {
                "KB"
            } else if (fileSize < 1073741824) {
                "MB"
            } else {
                "GB"
            }
    }

    override fun initContentView() = R.layout.activity_storage_space

    override fun initView() {

        tvFormatStorage = findViewById(R.id.tv_format_storage)
        tvProgressValue = findViewById(R.id.tv_progress_value)
        tvUsedValue = findViewById(R.id.tv_used_value)
        tvUsed = findViewById(R.id.tv_used)
        tvTotalValue = findViewById(R.id.tv_total_value)
        tvTotal = findViewById(R.id.tv_total)
        listStoragePhoto = findViewById(R.id.list_storage_photo)
        listStorageVideo = findViewById(R.id.list_storage_video)
        listStorageSystem = findViewById(R.id.list_storage_system)
        customViewProgress = findViewById(R.id.custom_view_progress)

        tvFormatStorage.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        lifecycleScope.launch {
            // TS004Repository functionality removed
            val freeSpaceBean = null // TS004Repository.getFreeSpace()
            if (freeSpaceBean == null) {
                TToast.shortToast(this@StorageSpaceActivity, RCore.string.operation_failed_tips)
                // Set default values since TS004Repository functionality is removed
                tvProgressValue.text = "0%"
                tvUsedValue.text = formatFileSize(0L)
                tvUsed.text = getUnit(0L)
                tvTotalValue.text = " / " + formatFileSize(0L)
                tvTotal.text = getUnit(0L)
                
                listStoragePhoto.setRightText(formatFileSize(0L) + getUnit(0L))
                listStorageVideo.setRightText(formatFileSize(0L) + getUnit(0L))
                listStorageSystem.setRightText(formatFileSize(0L) + getUnit(0L))
                
                val colorList = arrayListOf<ColorsBean>()
                colorList.add(ColorsBean(0, 1, 0xff8d98a9.toInt()))
                // customViewProgress.setData(colorList) // Method signature may be different
            } 
            // else block removed - freeSpaceBean is always null since TS004Repository functionality is removed
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            tvFormatStorage -> { 
                TipDialog.Builder(this@StorageSpaceActivity)
                    .setTitleMessage(getString(RCore.string.more_storage_reset))
                    .setMessage(getString(RCore.string.more_storage_reset1))
                    .setShowRestartTops(true)
                    .setPositiveListener(RCore.string.app_ok) {
                        showLoadingDialog()
                        lifecycleScope.launch {
                            // TS004Repository functionality removed
                            val isSuccess = false // TS004Repository.getFormatStorage()
                            if (isSuccess) {
                                XLog.d("TS004 格式化存储成功，即将断开连接")
                                (application as BaseApplication).disconnectWebSocket()
                                NavigationManager.getInstance().build(RouterConfig.MAIN)
                                    .navigation(this@StorageSpaceActivity)
                                finish()
                            } else {
                                delay(500)
                                dismissLoadingDialog()
                                TToast.shortToast(
                                    this@StorageSpaceActivity,
                                    RCore.string.operation_failed_tips
                                )
                            }
                        }
                    }
                    .setCancelListener(RCore.string.app_cancel) {
                    }
                    .setCanceled(true)
                    .create().show()
            }
        }
    }
}
