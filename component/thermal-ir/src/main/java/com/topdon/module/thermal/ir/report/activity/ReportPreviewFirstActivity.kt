package com.topdon.module.thermal.ir.report.activity

import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.view.TitleView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportInfoBean
import com.topdon.module.thermal.ir.report.view.ReportInfoView
import com.topdon.module.thermal.ir.report.view.WatermarkView
import com.topdon.lib.ui.R as UiR




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

        val reportInfoBean: ReportInfoBean? = intent.getParcelableExtra(ExtraKeyConfig.REPORT_INFO)
        reportInfoView.refreshInfo(reportInfoBean)
        reportInfoView.refreshCondition(intent.getParcelableExtra(ExtraKeyConfig.REPORT_CONDITION))

        if (reportInfoBean?.is_report_watermark == 1) {
            watermarkView.watermarkText = reportInfoBean.report_watermark
        }
    }

    override fun initData() {
    }
}
