package com.mpdc4gsr.module.thermal.ir.report.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.FileConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.tools.FileTools
import com.mpdc4gsr.lib.core.tools.GlideLoader
import com.mpdc4gsr.lib.core.view.TitleView
import com.mpdc4gsr.libcom.PDFHelp
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.report.bean.ReportBean
import com.mpdc4gsr.module.thermal.ir.report.view.ReportIRShowView
import com.mpdc4gsr.module.thermal.ir.report.view.ReportInfoView
import com.mpdc4gsr.module.thermal.ir.report.view.WatermarkView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.mpdc4gsr.lib.core.R as LibCoreR
import com.mpdc4gsr.lib.ui.R as UiR




class ReportDetailActivity : BaseActivity() {

    private lateinit var titleView: TitleView
    private lateinit var scrollView: ScrollView
    private lateinit var reportInfoView: ReportInfoView
    private lateinit var llContent: LinearLayout
    private lateinit var watermarkView: WatermarkView

    private var reportBean: ReportBean? = null

    private var pdfFilePath: String? = null

    override fun initContentView() = R.layout.activity_report_detail

    override fun initView() {

        titleView = findViewById(R.id.title_view)
        scrollView = findViewById(R.id.scroll_view)
        reportInfoView = findViewById(R.id.report_info_view)
        llContent = findViewById(R.id.ll_content)
        watermarkView = findViewById(R.id.watermark_view)

        reportBean = intent.getParcelableExtra(ExtraKeyConfig.REPORT_BEAN)

        titleView.setTitleText(R.string.album_edit_report)
        titleView.setLeftDrawable(UiR.drawable.svg_arrow_left_e8)
        titleView.setRightDrawable(R.drawable.ic_share_black_svg)
        titleView.setLeftClickListener {
            finish()
        }
        titleView.setRightClickListener {
            saveWithPDF()
        }

        reportInfoView.refreshInfo(reportBean?.report_info)
        reportInfoView.refreshCondition(reportBean?.detection_condition)

        if (reportBean?.report_info?.is_report_watermark == 1) {
            watermarkView.watermarkText = reportBean?.report_info?.report_watermark
        }

        val irList = reportBean?.infrared_data
        if (irList != null) {
            for (i in irList.indices) {
                val reportShowView = ReportIRShowView(this)
                reportShowView.refreshData(i == 0, i == irList.size - 1, irList[i])
                lifecycleScope.launch {
                    val drawable =
                        GlideLoader.getDrawable(this@ReportDetailActivity, irList[i].picture_url)
                    reportShowView.setImageDrawable(drawable)
                }
                llContent.addView(
                    reportShowView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun initData() {
    }

    private fun saveWithPDF() {
        if (TextUtils.isEmpty(pdfFilePath)) {
            showCameraLoading()
            lifecycleScope.launch(Dispatchers.IO) {
                val name = reportBean?.report_info?.report_number
                if (name != null) {
                    if (File(FileConfig.getPdfDir() + "/$name.pdf").exists() &&
                        !TextUtils.isEmpty(pdfFilePath)
                    ) {
                        lifecycleScope.launch {
                            dismissCameraLoading()
                            actionShare()
                        }
                        return@launch
                    }
                }
                pdfFilePath =
                    PDFHelp.savePdfFileByListView(
                        name ?: System.currentTimeMillis().toString(),
                        scrollView, getPrintViewList(), watermarkView,
                    )
                lifecycleScope.launch {
                    dismissCameraLoading()
                    actionShare()
                }
            }
        } else {
            actionShare()
        }
    }

    private fun actionShare() {
        val uri = FileTools.getUri(File(pdfFilePath!!))
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/pdf"
        startActivity(Intent.createChooser(shareIntent, getString(LibCoreR.string.battery_share)))
    }

    
    private fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(reportInfoView)
        val childCount = llContent.childCount
        for (i in 0 until childCount) {
            val childView = llContent.getChildAt(i)
            if (childView is ReportIRShowView) {
                result.addAll(childView.getPrintViewList())
            }
        }
        return result
    }
}
