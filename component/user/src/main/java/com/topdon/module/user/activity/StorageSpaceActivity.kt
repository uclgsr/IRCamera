package com.topdon.module.user.activity

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lms.sdk.utils.TLog
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.user.R
import com.topdon.module.user.bean.ColorsBean
import com.topdon.module.user.view.ListItemView
import com.topdon.module.user.view.ProgressBarView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.topdon.lib.core.R as RCore

// Legacy ARouter route annotation - now using NavigationManager
class StorageSpaceActivity : BaseActivity(), View.OnClickListener {
    // View references - migrated from synthetic views
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
        // Initialize views - migrated from synthetic views
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
            val freeSpaceBean = TS004Repository.getFreeSpace()
            if (freeSpaceBean == null) {
                TToast.shortToast(this@StorageSpaceActivity, RCore.string.operation_failed_tips)
            } else {
                TLog.d("ts004", "║ response :$freeSpaceBean")

                tvProgressValue.text = "${(freeSpaceBean.hasUseSize() * 100.0 / freeSpaceBean.total).toInt().coerceAtLeast(1)}"

                tvUsedValue.text = formatFileSize(freeSpaceBean.hasUseSize())
                tvUsed.text = getUnit(freeSpaceBean.hasUseSize())
                tvTotalValue.text = " / " + formatFileSize(freeSpaceBean.total)
                tvTotal.text = getUnit(freeSpaceBean.total)

                listStoragePhoto.setRightText(formatFileSize(freeSpaceBean.image_size) + getUnit(freeSpaceBean.image_size))
                listStorageVideo.setRightText(formatFileSize(freeSpaceBean.video_size) + getUnit(freeSpaceBean.video_size))
                listStorageSystem.setRightText(formatFileSize(freeSpaceBean.system) + getUnit(freeSpaceBean.system))

                val systemPercent = (freeSpaceBean.system * 100.0 / freeSpaceBean.total).toInt().coerceAtLeast(1).coerceAtMost(98)
                val imagePercent = (freeSpaceBean.image_size * 100.0 / freeSpaceBean.total).toInt().coerceAtLeast(1).coerceAtMost(98)
                val videoPercent = (freeSpaceBean.video_size * 100.0 / freeSpaceBean.total).toInt().coerceAtLeast(1).coerceAtMost(98)
                val colorList = arrayListOf<ColorsBean>()
                colorList.add(ColorsBean(0, systemPercent, 0xff8d98a9.toInt()))
                colorList.add(ColorsBean(systemPercent, systemPercent + imagePercent, 0xff019dff.toInt()))
                colorList.add(ColorsBean(systemPercent + imagePercent, systemPercent + imagePercent + videoPercent, 0xff70e297.toInt()))
                customViewProgress.setSegmentPart(colorList)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            tvFormatStorage -> { // format化storage
                TipDialog.Builder(this@StorageSpaceActivity)
                    .setTitleMessage(getString(RCore.string.more_storage_reset))
                    .setMessage(getString(RCore.string.more_storage_reset1))
                    .setShowRestartTops(true)
                    .setPositiveListener(RCore.string.app_ok) {
                        showLoadingDialog()
                        lifecycleScope.launch {
                            val isSuccess = TS004Repository.getFormatStorage()
                            if (isSuccess) {
                                XLog.d("TS004 format化storagesuccess，即将disconnectconnection")
                                (application as BaseApplication).disconnectWebSocket()
                                NavigationManager.getInstance().build(RouterConfig.MAIN).navigation(this@StorageSpaceActivity)
                                finish()
                            } else {
                                delay(500)
                                dismissLoadingDialog()
                                TToast.shortToast(this@StorageSpaceActivity, RCore.string.operation_failed_tips)
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
