package com.mpdc4gsr.module.thermal.ir.report.activity

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.libunified.app.comm.view.CommLoadMoreView
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstant
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtil
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
import com.mpdc4gsr.libunified.app.view.TitleView
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.adapter.PDFAdapter
import com.mpdc4gsr.module.thermal.ir.report.viewmodel.PdfViewModel
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class PDFListActivity : BaseViewModelActivity<PdfViewModel>() {

    private val titleView: TitleView by lazy { findViewById(R.id.title_view) }
    private val fragmentPdfRecyclerLay: SmartRefreshLayout by lazy { findViewById(R.id.fragment_pdf_recycler_lay) }
    private val fragmentPdfRecycler: RecyclerView by lazy { findViewById(R.id.fragment_pdf_recycler) }


    private var isTC007 = false

    var page = 1

    override fun providerVMClass() = PdfViewModel::class.java

    var reportAdapter = PDFAdapter(R.layout.item_pdf)

    override fun initContentView(): Int {
        return R.layout.activity_pdf_list
    }

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        viewModel.listData.observe(this) {
            dismissLoadingDialog()
            if (!reportAdapter.hasEmptyView()) {
                reportAdapter.setEmptyView(R.layout.layout_empty)
            }
            if (it == null) {
                if (page == 1) {
                    fragmentPdfRecyclerLay.finishRefresh(false)
                } else {
                    reportAdapter.loadMoreModule.loadMoreComplete()
                }
            }
            it?.let { data ->
                if (page == 1) {

                    if (data.code == LMS.SUCCESS) {
                        reportAdapter.loadMoreModule.isEnableLoadMore =
                            !data.data?.records.isNullOrEmpty()
                        fragmentPdfRecyclerLay.finishRefresh()
                    } else {
                        fragmentPdfRecyclerLay.finishRefresh(false)
                    }
                    reportAdapter.setNewInstance(data.data?.records)
                } else {
                    data.data?.records?.let { it1 -> reportAdapter.addData(it1) }
                    if (data.code == LMS.SUCCESS) {
                        if (data.data?.records.isNullOrEmpty()) {
                            reportAdapter.loadMoreModule.loadMoreEnd()
                        } else {
                            reportAdapter.loadMoreModule.loadMoreComplete()
                        }
                    } else {
                        reportAdapter.loadMoreModule.loadMoreFail()
                    }
                }
            }
        }
        if (WebSocketProxy.getInstance().isConnected()) {
            NetWorkUtils.switchNetwork(false)
        } else {
            NetWorkUtils.connectivityManager.bindProcessToNetwork(null)
        }
        initRecycler()
    }

    override fun initData() {
    }

    private fun initRecycler() {
        fragmentPdfRecycler.layoutManager = LinearLayoutManager(this)
        fragmentPdfRecyclerLay.setOnRefreshListener {

            page = 1
            viewModel.getReportData(isTC007, page)
        }
        fragmentPdfRecyclerLay.setEnableLoadMore(false)
        reportAdapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        fragmentPdfRecyclerLay.autoRefresh()
        reportAdapter.loadMoreModule.setOnLoadMoreListener {

            viewModel.getReportData(isTC007, ++page)
        }
        reportAdapter.jumpDetailListener = { item, position ->
            reportAdapter.data[position]?.reportContent?.let { reportContent ->
                NavigationManager.getInstance().build(RouterConfig.REPORT_DETAIL)
                    .withParcelable(ExtraKeyConfig.REPORT_BEAN, reportContent)
                    .navigation(this)
            }
        }
        reportAdapter.isUseEmpty = true
        reportAdapter.delListener = { item, position ->
            val reportBean = item.reportContent
            TipDialog.Builder(this)
                .setMessage(
                    getString(
                        R.string.tip_config_delete,
                        reportBean?.report_info?.report_name ?: ""
                    )
                )
                .setPositiveListener(R.string.app_confirm) {
                    lifecycleScope.launch {
                        showLoadingDialog()
                        withContext(Dispatchers.IO) {
                            val url =
                                UrlConstant.BASE_URL + "api/v1/outProduce/testReport/delTestReport"
                            val params = RequestParams()
                            params.addBodyParameter(
                                "modelId",
                                if (isTC007) 1783 else 950
                            )
                            params.addBodyParameter("testReportIds", arrayOf(item.testReportId))
                            params.addBodyParameter("status", 1)
                            params.addBodyParameter(
                                "languageId",
                                LanguageUtil.getLanguageId(Utils.getApp())
                            )
                            params.addBodyParameter("reportType", 2)
                            HttpProxy.instant.post(
                                url, params,
                                object :
                                    IResponseCallback {
                                    override fun onResponse(response: String?) {
                                        val reportNumber =
                                            item.reportContent?.report_info?.report_number ?: ""
                                        val file =
                                            File(FileConfig.getPdfDir() + "/$reportNumber.pdf")
                                        if (file.exists()) {
                                            file.delete()
                                        }
                                        Log.w("删除成功", response.toString())
                                    }

                                    override fun onFail(exception: Exception?) {
                                    }

                                    override fun onFail(
                                        failMsg: String?,
                                        errorCode: String,
                                    ) {
                                        super.onFail(failMsg, errorCode)
                                        try {
                                            StringUtils.getResString(
                                                LMS.mContext,
                                                if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                                            ).let {
                                                TToast.shortToast(LMS.mContext, it)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                            )
                        }
                        dismissLoadingDialog()
                        if (item.isShowTitleTime) {
                            reportAdapter.remove(item)
                            reportAdapter.setNewInstance(reportAdapter.data)
                            reportAdapter.notifyDataSetChanged()
                        } else {
                            reportAdapter.data.removeAt(position)
                            reportAdapter.notifyItemRemoved(position)
                        }
                    }
                }
                .setCancelListener(R.string.app_cancel) {
                }
                .create().show()
        }

        fragmentPdfRecycler.adapter = reportAdapter

    }
}
