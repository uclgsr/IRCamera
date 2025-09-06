package com.topdon.module.thermal.ir.report.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.bean.event.ReportCreateEvent
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseViewModelActivity
import com.topdon.lib.core.tools.FileTools
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.utils.NetWorkUtils
import com.topdon.lib.core.view.TitleView
import com.topdon.libcom.PDFHelp
import com.topdon.lms.sdk.LMS
import com.topdon.lms.sdk.utils.StringUtils
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.report.view.ReportIRShowView
import com.topdon.module.thermal.ir.report.view.ReportInfoView
import com.topdon.module.thermal.ir.report.view.WatermarkView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportBean
import com.topdon.module.thermal.ir.report.viewmodel.UpReportViewModel
import com.topdon.lib.core.R as LibCoreR
import com.topdon.lib.ui.R as UiR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * 生成报告第2步的预览界面.
 *
 * 需要传递
 * - 是否 TC007: [ExtraKeyConfig.IS_TC007]
 * - 一份报告所有信息 [ExtraKeyConfig.REPORT_BEAN]
 */
// Legacy ARouter route annotation - now using NavigationManager
class ReportPreviewSecondActivity: BaseViewModelActivity<UpReportViewModel>(), View.OnClickListener {

    /**
     * 从上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false

    /**
     * 从上一界面传递过来的，报告所有信息.
     */
    private var reportBean: ReportBean? = null

    /**
     * 当前预览页面已生成的 PDF 文件绝对路径
     */
    private var pdfFilePath: String? = null

    // View references - migrated from synthetic views
    private lateinit var titleView: TitleView
    private lateinit var reportInfoView: ReportInfoView
    private lateinit var llContent: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var watermarkView: WatermarkView
    private lateinit var tvToPdf: TextView
    private lateinit var tvComplete: TextView


    override fun initContentView() = R.layout.activity_report_preview_second

    override fun providerVMClass() = UpReportViewModel::class.java

    override fun initView() {
        // Initialize views - migrated from synthetic views
        titleView = findViewById(R.id.title_view)
        reportInfoView = findViewById(R.id.report_info_view)
        llContent = findViewById(R.id.ll_content)
        scrollView = findViewById(R.id.scroll_view)
        watermarkView = findViewById(R.id.watermark_view)
        tvToPdf = findViewById(R.id.tv_to_pdf)
        tvComplete = findViewById(R.id.tv_complete)
        
        reportBean = intent.getParcelableExtra(ExtraKeyConfig.REPORT_BEAN)
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        titleView.setTitleText(LibCoreR.string.album_edit_preview)
        titleView.setLeftDrawable(UiR.drawable.svg_arrow_left_e8)
        titleView.setRightDrawable(R.drawable.ic_report_exit_svg)
        titleView.setLeftClickListener {
            finish()
        }
        titleView.setRightClickListener {
            TipDialog.Builder(this)
                .setMessage(LibCoreR.string.album_report_exit_tips)
                .setPositiveListener(UiR.string.app_ok){
                    EventBus.getDefault().post(ReportCreateEvent())
                    finish()
                }
                .setCancelListener(UiR.string.app_cancel){
                }
                .setCanceled(false)
                .create().show()
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
                    val drawable = GlideLoader.getDrawable(this@ReportPreviewSecondActivity, irList[i].picture_url)
                    reportShowView.setImageDrawable(drawable)
                }
                llContent.addView(reportShowView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        tvToPdf.setOnClickListener(this)
        tvComplete.setOnClickListener(this)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // 要是当前已连接 TS004、TC007，切到流量上，不然登录注册意见反馈那些没网
                if (WebSocketProxy.getInstance().isConnected()) {
                    NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
                }
            }
        })
    }

    override fun initData() {
        viewModel.commonBeanLD.observe(this) {
            dismissCameraLoading()
            if (it.code == LMS.SUCCESS) {
                EventBus.getDefault().post(ReportCreateEvent())
                NavigationManager.getInstance().build(RouterConfig.REPORT_LIST)
                    .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                    .navigation(this)
                finish()
            } else {
                ToastUtils.showShort(StringUtils.getResString(this, it.code.toString()))
            }
        }
        viewModel.exceptionLD.observe(this) {
            dismissCameraLoading()
            requestError(it)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            tvToPdf -> {//生成PDF
                saveWithPDF()
            }
            tvComplete -> {//完成

                if (LMS.getInstance().isLogin) {
                    if (!NetworkUtils.isConnected()) {
                        TToast.shortToast(this, LibCoreR.string.http_code_z5004)
                        return
                    }
                    showCameraLoading()
                    viewModel.upload(isTC007, reportBean)
                } else {
                    LMS.getInstance().activityLogin()
                }
            }
        }
    }

    private fun saveWithPDF() {
        if (TextUtils.isEmpty(pdfFilePath)) {
            showCameraLoading()
            lifecycleScope.launch(Dispatchers.IO) {
                val name = reportBean?.report_info?.report_number
                if (name != null) {
                    if (File(FileConfig.getPdfDir() + "/$name.pdf").exists() &&
                        !TextUtils.isEmpty(pdfFilePath)) {
                        lifecycleScope.launch {
                            dismissCameraLoading()
                            actionShare()
                        }
                        return@launch
                    }
                }
                pdfFilePath = PDFHelp.savePdfFileByListView(name?:System.currentTimeMillis().toString(),
                    scrollView, getPrintViewList(),watermarkView)
                lifecycleScope.launch {
                    tvToPdf.text = getString(LibCoreR.string.battery_share)
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

    /**
     * 获取需要转为 PDF 的所有 View 列表.
     * 注意：水印 View 不在列表内，需要自行处理.
     */
    private fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(reportInfoView)
        val childCount = llContent.childCount
        for (i in 0 until  childCount) {
            val childView = llContent.getChildAt(i)
            if (childView is ReportIRShowView) {
                result.addAll(childView.getPrintViewList())
            }
        }
        return result
    }
}