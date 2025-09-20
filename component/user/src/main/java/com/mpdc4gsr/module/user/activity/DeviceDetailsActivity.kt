package com.mpdc4gsr.module.user.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.repository.ProductBean

// TS004Repository functionality removed
// import com.mpdc4gsr.lib.core.repository.TS004Repository
import com.mpdc4gsr.lms.sdk.utils.TLog
import com.mpdc4gsr.lms.sdk.weiget.TToast
import com.mpdc4gsr.module.user.R
import kotlinx.coroutines.launch
import com.mpdc4gsr.lib.core.R as RCore


class DeviceDetailsActivity : BaseActivity(), View.OnClickListener {

    private lateinit var clLayoutCopy: ConstraintLayout
    private lateinit var tvSnValue: TextView
    private lateinit var tvDeviceModelValue: TextView
    private lateinit var tvSn: TextView
    private lateinit var tvDeviceModel: TextView


    private var isTC007 = false

    override fun initContentView() = R.layout.activity_device_details

    override fun initView() {

        clLayoutCopy = findViewById(R.id.cl_layout_copy)
        tvSnValue = findViewById(R.id.tv_sn_value)
        tvDeviceModelValue = findViewById(R.id.tv_device_model_value)
        tvSn = findViewById(R.id.tv_sn)
        tvDeviceModel = findViewById(R.id.tv_device_model)

        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        clLayoutCopy.setOnClickListener(this)
    }

    override fun initData() {
        getDeviceDetails()
    }

    private fun getDeviceDetails() {
        lifecycleScope.launch {
            if (isTC007) {
                // TC007Repository functionality removed
                TToast.shortToast(
                    this@DeviceDetailsActivity,
                    RCore.string.operation_failed_tips
                )
            } else {
                // TS004Repository functionality removed
                TToast.shortToast(
                    this@DeviceDetailsActivity,
                    RCore.string.operation_failed_tips
                )
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            clLayoutCopy -> {
                val text =
                    "${tvSn.text}:${tvSnValue.text}  ${tvDeviceModel.text}:${tvDeviceModelValue.text}"
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                val mClipData = ClipData.newPlainText("text", text)
                cm!!.setPrimaryClip(mClipData)
                TToast.shortToast(this@DeviceDetailsActivity, RCore.string.ts004_copy_success)
            }
        }
    }
}
