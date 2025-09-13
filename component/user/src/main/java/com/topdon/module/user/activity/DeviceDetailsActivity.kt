package com.topdon.module.user.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.repository.ProductBean
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lms.sdk.utils.TLog
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.user.R
import kotlinx.coroutines.launch
import com.topdon.lib.core.R as RCore

/**
TS004、TC007 deviceinfo
 *
需要传递parameter：
- [ExtraKeyConfig.IS_TC007] - 当前device是否为 TC007
 */
// Legacy ARouter route annotation - now using NavigationManager
class DeviceDetailsActivity : BaseActivity(), View.OnClickListener {
    // View references - migrated from synthetic views
    private lateinit var clLayoutCopy: ConstraintLayout
    private lateinit var tvSnValue: TextView
    private lateinit var tvDeviceModelValue: TextView
    private lateinit var tvSn: TextView
    private lateinit var tvDeviceModel: TextView

    /**
从上一interface传递过来的，当前是否为 TC007 devicetype.
true-TC007 false-其他插件式device
     */
    private var isTC007 = false

    override fun initContentView() = R.layout.activity_device_details

    override fun initView() {
        // Initialize views - migrated from synthetic views
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
                val productBean: ProductBean? = TC007Repository.getProductInfo()
                if (productBean == null) {
                    TToast.shortToast(this@DeviceDetailsActivity, RCore.string.operation_failed_tips)
                } else {
                    tvSnValue.text = productBean.ProductSN
                    tvDeviceModelValue.text = productBean.ProductName
                }
            } else {
                val deviceDetailsBean = TS004Repository.getDeviceInfo()
                if (deviceDetailsBean?.isSuccess()!!) {
                    TLog.d("ts004-->response", "${deviceDetailsBean.data}")
                    tvSnValue.text = deviceDetailsBean.data!!.sn
                    tvDeviceModelValue.text = deviceDetailsBean.data!!.model
                } else {
                    TToast.shortToast(this@DeviceDetailsActivity, RCore.string.operation_failed_tips)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            clLayoutCopy -> { // copyinfo
                val text = "${tvSn.text}:${tvSnValue.text}  ${tvDeviceModel.text}:${tvDeviceModelValue.text}"
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                val mClipData = ClipData.newPlainText("text", text)
                cm!!.setPrimaryClip(mClipData)
                TToast.shortToast(this@DeviceDetailsActivity, RCore.string.ts004_copy_success)
            }
        }
    }
}
