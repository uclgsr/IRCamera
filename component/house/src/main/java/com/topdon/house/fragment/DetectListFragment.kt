package com.topdon.house.fragment

import android.content.Intent
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.house.activity.DetectAddActivity
import com.topdon.house.activity.ReportAddActivity
import com.topdon.house.adapter.HouseAdapter
import com.topdon.house.event.HouseDetectAddEvent
import com.topdon.house.event.HouseDetectEditEvent
import com.topdon.house.popup.ThreePickPopup
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.house.viewmodel.TabViewModel
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.HouseDetect
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lms.sdk.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 检测列表.
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007（不使用，透传）
 *
 * Created by LCG on 2024/8/20.
 */
internal class DetectListFragment : BaseFragment(), View.OnClickListener {
    private lateinit var adapter: HouseAdapter

    private val tabViewModel: TabViewModel by activityViewModels()

    private val viewModel: DetectViewModel by activityViewModels()

    override fun initContentView(): Int = R.layout.fragment_detect_list

    override fun initView() {
        val clDel = requireView().findViewById<android.view.View>(R.id.cl_del)
        val ivDel = requireView().findViewById<android.widget.ImageView>(R.id.iv_del)
        val tvDel = requireView().findViewById<android.widget.TextView>(R.id.tv_del)
        val recyclerView = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        val tvAdd = requireView().findViewById<android.widget.TextView>(R.id.tv_add)
        
        clDel.isEnabled = false
        ivDel.isEnabled = false
        tvDel.isEnabled = false

        adapter = HouseAdapter(requireContext(), true)
        adapter.onItemClickListener = {
            val intent = Intent(context, ReportAddActivity::class.java)
            intent.putExtra(ExtraKeyConfig.DETECT_ID, adapter.dataList[it].id)
            intent.putExtra(ExtraKeyConfig.IS_TC007, arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false)
            startActivity(intent)
        }
        adapter.onMoreClickListener = { position, v ->
            ThreePickPopup(requireContext(), arrayListOf(LibR.string.app_edit, LibR.string.paste, LibR.string.report_delete)) {
                when (it) {
                    0 -> {//编辑
                        val intent = Intent(requireContext(), DetectAddActivity::class.java)
                        intent.putExtra(ExtraKeyConfig.DETECT_ID, adapter.dataList[position].id)
                        startActivity(intent)
                    }
                    1 -> {//复制
                        viewModel.copyDetect(position, adapter.dataList[position] as HouseDetect)
                    }
                    2 -> {//删除
                        TipDialog.Builder(requireContext())
                            .setTitleMessage(getString(LibR.string.monitor_report_delete))
                            .setMessage(LibR.string.report_delete_tips)
                            .setCancelListener(LibR.string.app_cancel)
                            .setPositiveListener(LibR.string.thermal_delete) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val houseDetect: HouseDetect = adapter.dataList[position] as HouseDetect
                                    AppDatabase.getInstance().houseDetectDao().deleteDetect(houseDetect)
                                    withContext(Dispatchers.Main) {
                                        adapter.dataList.removeAt(position)
                                        adapter.notifyItemRemoved(position)
                                        if (adapter.dataList.isEmpty()) {
                                            viewModel.queryAll()
                                        }
                                        TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                                    }
                                }
                            }
                            .create().show()
                    }
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


        tabViewModel.isEditModeLD.observe(viewLifecycleOwner) {
            adapter.isEditMode = it
            clDel.isVisible = it
        }
        tabViewModel.selectSizeLD.observe(viewLifecycleOwner) {
            clDel.isEnabled = it > 0
            ivDel.isEnabled = it > 0
            tvDel.isEnabled = it > 0
        }

        val groupEmpty = requireView().findViewById<android.view.View>(R.id.group_empty)
        
        viewModel.detectListLD.observe(viewLifecycleOwner) {
            groupEmpty.isVisible = it.isEmpty()
            recyclerView.isVisible = it.isNotEmpty()
            adapter.refresh(it)
        }
        viewModel.detectLD.observe(viewLifecycleOwner) {
            if (it != null) {
                for (i in adapter.dataList.indices) {
                    if (adapter.dataList[i].id == it.id) {
                        adapter.dataList[i] = it
                        adapter.notifyItemChanged(i)
                        break
                    }
                }
            }
        }
        viewModel.copyDetectLD.observe(viewLifecycleOwner) {
            TToast.shortToast(requireContext(), LibR.string.ts004_copy_success)
            adapter.dataList.add(it.first + 1, it.second)
            adapter.notifyItemInserted(it.first + 1)
        }
        viewModel.queryAll()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDetectCreate(event: HouseDetectAddEvent) {
        viewModel.queryAll()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDetectUpdate(event: HouseDetectEditEvent) {
        viewModel.queryById(event.id)
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
                            val resultArray: Array<HouseDetect> = Array(adapter.selectIndexList.size) {
                                adapter.dataList[adapter.selectIndexList[it]] as HouseDetect
                            }
                            viewModel.deleteMore(*resultArray)
                            tabViewModel.isEditModeLD.value = false
                            TToast.shortToast(requireContext(), R.string.test_results_delete_success)
                        }
                        .create().show()
                }
            }
        }
    }
}