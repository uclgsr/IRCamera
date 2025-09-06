package com.topdon.module.user.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.core.R as LibAppR
import com.topdon.module.user.R
import com.topdon.lib.core.R as RCore
import java.text.DecimalFormat

/**
 * 下载进度提示弹框.
 * Created by LCG on 2024/3/5.
 */
class DownloadProDialog(context: Context) : Dialog(context, LibAppR.style.InfoDialog) {

    private val rootView: View = LayoutInflater.from(context).inflate(R.layout.dialog_download_pro, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(rootView)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    /**
     * 刷新进度值
     */
    fun refreshProgress(current: Long, total: Long) {
        val progress = (current * 100f / total).toInt()
        val tvSize = rootView.findViewById<android.widget.TextView>(R.id.tv_size)
        val progressBar = rootView.findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        val tvProgress = rootView.findViewById<android.widget.TextView>(R.id.tv_progress)
        
        tvSize.text = "${context.getString(RCore.string.detail_len)}: ${getFileSizeStr(current)}/${getFileSizeStr(total)}"
        progressBar.progress = progress
        tvProgress.text = "${progress}%"
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
}