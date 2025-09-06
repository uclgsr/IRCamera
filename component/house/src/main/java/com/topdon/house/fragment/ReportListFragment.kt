package com.topdon.house.fragment

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.topdon.lib.core.navigation.NavigationManager
import com.blankj.utilcode.util.FileUtils
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.house.activity.DetectAddActivity
import com.topdon.house.adapter.HouseAdapter
import com.topdon.house.dialog.InputTextDialog
import com.topdon.house.event.HouseReportAddEvent
import com.topdon.house.popup.ThreePickPopup
import com.topdon.house.util.PDFUtil
import com.topdon.house.viewmodel.ReportViewModel
import com.topdon.house.viewmodel.TabViewModel
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseReport
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lms.sdk.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * 报告列表.
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007（不使用，透传）
 *
 * Created by LCG on 2024/8/20.
 */
internal class ReportListFragment : BaseFragment(), View.OnClickListener {
    private lateinit var adapter: HouseAdapter

    private val tabViewModel: TabViewModel by activityViewModels()

    private val viewModel: ReportViewModel by activityViewModels()

    override fun initContentView(): Int = R.layout.fragment_report_list

    override fun initView() {
        val clDel = requireView().findViewById<android.view.View>(R.id.cl_del)
        val ivDel = requireView().findViewById<android.widget.ImageView>(R.id.iv_del)
        val tvDel = requireView().findViewById<android.widget.TextView>(R.id.tv_del)
        val recyclerView = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        val tvAdd = requireView().findViewById<android.widget.TextView>(R.id.tv_add)
        
        clDel.isEnabled = false
        ivDel.isEnabled = false
        tvDel.isEnabled = false

        adapter = HouseAdapter(requireContext(), false)
        adapter.onItemClickListener = {
            NavigationManager.getInstance().build(RouterConfig.REPORT_PREVIEW)
                .withBoolean(ExtraKeyConfig.IS_REPORT, true)
                .withLong(ExtraKeyConfig.LONG_ID, adapter.dataList[it].id)
                .navigation(requireContext())
        }
        adapter.onShareClickListener = {
            lifecycleScope.launch {
                showLoadingDialog()
                PDFUtil.delAllPDF(requireContext())
                val pdfUri: Uri? = PDFUtil.savePDF(requireContext(), adapter.dataList[it] as HouseReport)
                dismissLoadingDialog()
                if (pdfUri != null) {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
                    shareIntent.type = "application/pdf"
                    startActivity(Intent.createChooser(shareIntent, getString(LibR.string.battery_share)))
                }
            }
        }
        adapter.onMoreClickListener = { position, v ->
            ThreePickPopup(requireContext(), arrayListOf(LibR.string.app_rename, LibR.string.report_delete)) {
                if (it == 0) {//重命名
                    val houseReport: HouseReport = adapter.dataList[position] as HouseReport
                    InputTextDialog(requireContext(), houseReport.name) { newName ->
                        if (houseReport.name != newName) {
                            FileUtils.delete(File(FileConfig.documentsDir, houseReport.getPdfFileName()))
                            houseReport.name = newName
                            viewModel.update(houseReport)
                            adapter.notifyItemChanged(position)
                        }
                    }.show()
                } else {//删除
                    TipDialog.Builder(requireContext())
                        .setTitleMessage(getString(LibR.string.monitor_report_delete))
                        .setMessage(LibR.string.report_delete_tips)
                        .setCancelListener(LibR.string.app_cancel)
                        .setPositiveListener(LibR.string.thermal_delete) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val houseReport: HouseReport = adapter.dataList[position] as HouseReport
                                AppDatabase.getInstance().houseReportDao().deleteReport(houseReport)
                                PDFUtil.delPDF(requireContext(), houseReport)
                                withContext(Dispatchers.Main) {
                                    adapter.dataList.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                    if (adapter.dataList.isEmpty()) {
                                        viewModel.queryAll()
                                    }
                                    TToast.shortToast(requireContext(), LibR.string.test_results_delete_success)
                                }
                            }
                        }
                        .create().show()
                }
            }.show(v, false)
        }
        adapter.onSelectChangeListener = {
            tabViewModel.selectSizeLD.value = it
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        tvAdd.setOnClickListener(this)
        clDel.setOnClickListener(this)

        val groupEmpty = requireView().findViewById<android.view.View>(R.id.group_empty)

        tabViewModel.isEditModeLD.observe(viewLifecycleOwner) {
            adapter.isEditMode = it
            clDel.isVisible = it
        }
        tabViewModel.selectSizeLD.observe(viewLifecycleOwner) {
            clDel.isEnabled = it > 0
            ivDel.isEnabled = it > 0
            tvDel.isEnabled = it > 0
        }

        viewModel.reportListLD.observe(viewLifecycleOwner) {
            groupEmpty.isVisible = it.isEmpty()
            recyclerView.isVisible = it.isNotEmpty()
            adapter.refresh(it)
        }
        viewModel.queryAll()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDetectCreate(event: HouseReportAddEvent) {
        viewModel.queryAll()
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        val tvAdd = requireView().findViewById<android.widget.TextView>(R.id.tv_add)
        val clDel = requireView().findViewById<android.view.View>(R.id.cl_del)
        
        when (v) {
            tvAdd -> {//添加
                val intent = Intent(requireContext(), DetectAddActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_TC007, arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false)
                startActivity(intent)
            }
            clDel -> {//批量删除
                if (adapter.selectIndexList.isNotEmpty()) {
                    TipDialog.Builder(requireContext())
                        .setTitleMessage(getString(LibR.string.monitor_report_delete))
                        .setMessage(LibR.string.report_delete_tips)
                        .setCancelListener(LibR.string.app_cancel)
                        .setPositiveListener(LibR.string.thermal_delete) {
                            val resultArray: Array<HouseReport> = Array(adapter.selectIndexList.size) {
                                adapter.dataList[adapter.selectIndexList[it]] as HouseReport
                            }
                            viewModel.deleteMore(*resultArray)
                            tabViewModel.isEditModeLD.value = false
                            TToast.shortToast(requireContext(), LibR.string.test_results_delete_success)
                        }
                        .create().show()
                }
            }
        }
    }
}