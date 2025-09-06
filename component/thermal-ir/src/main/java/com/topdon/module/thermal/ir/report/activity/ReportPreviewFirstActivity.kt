package com.topdon.module.thermal.ir.report.activity

import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.view.TitleView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportInfoBean
import com.topdon.module.thermal.ir.report.view.ReportInfoView
import com.topdon.module.thermal.ir.report.view.WatermarkView
import com.topdon.lib.core.R as LibCoreR
import com.topdon.lib.ui.R as UiR

/**
 * 生成报告第1步的预览界面.
 *
 * 需要传递
 * - 必选：报告信息 [ExtraKeyConfig.REPORT_INFO]
 * - 可选：检测条件 [ExtraKeyConfig.REPORT_CONDITION]
 */
// Legacy ARouter route annotation - now using NavigationManager
class ReportPreviewFirstActivity: BaseActivity() {

    // View declarations
    private lateinit var titleView: TitleView
    private lateinit var reportInfoView: ReportInfoView
    private lateinit var watermarkView: WatermarkView

    override fun initContentView() = R.layout.activity_report_preview_first

    override fun initView() {
        // Initialize views
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