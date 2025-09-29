package com.mpdc4gsr.module.thermalunified.report.activity

import android.os.Build
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.view.TitleView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ReportConditionBean
import com.mpdc4gsr.module.thermalunified.report.bean.ReportInfoBean
import com.mpdc4gsr.module.thermalunified.report.view.ReportInfoView
import com.mpdc4gsr.module.thermalunified.report.view.WatermarkView
import com.mpdc4gsr.libunified.R as UiR


class ReportPreviewFirstActivity : BaseActivity() {

    private lateinit var titleView: TitleView
    private lateinit var reportInfoView: ReportInfoView
    private lateinit var watermarkView: WatermarkView

    override fun initContentView() = R.layout.activity_report_preview_first

    override fun initView() {

        titleView = findViewById(R.id.title_view)
        reportInfoView = findViewById(R.id.report_info_view)
        watermarkView = findViewById(R.id.watermark_view)

        titleView.setLeftDrawable(UiR.drawable.svg_arrow_left_e8)
        titleView.setLeftClickListener {
            finish()
        }

        val reportInfoBean: ReportInfoBean? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO, ReportInfoBean::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<ReportInfoBean>(ExtraKeyConfig.REPORT_INFO)
            }
        reportInfoView.refreshInfo(reportInfoBean)
        reportInfoView.refreshCondition(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    ExtraKeyConfig.REPORT_CONDITION,
                    ReportConditionBean::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<ReportConditionBean>(ExtraKeyConfig.REPORT_CONDITION)
            }
        )

        if (reportInfoBean?.is_report_watermark == 1) {
            watermarkView.watermarkText = reportInfoBean.report_watermark
        }
    }

    override fun initData() {
    }
}
