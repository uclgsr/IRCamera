package com.topdon.module.thermal.ir.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView as AndroidRecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.db.dao.ThermalDao
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.libcom.view.CommLoadMoreView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.activity.IRLogMPChartActivity
import com.topdon.module.thermal.ir.event.MonitorCreateEvent
import com.topdon.module.thermal.ir.viewmodel.IRMonitorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar
import androidx.recyclerview.widget.RecyclerView

class IRMonitorHistoryFragment : Fragment() {

    private val adapter = MyAdapter(ArrayList())

    private val viewModel: IRMonitorViewModel by viewModels()

    // findViewById declarations 
    private lateinit var recyclerView: AndroidRecyclerView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EventBus.getDefault().register(this)
        return inflater.inflate(R.layout.fragment_ir_monitor_history, container)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize findViewById
        recyclerView = view.findViewById(R.id.recycler_view)
        
        adapter.loadMoreModule.loadMoreView = CommLoadMoreView()
        adapter.onItemClickListener = {
            val record: ThermalDao.Record = adapter.data[it]
            val intent = Intent(context, IRLogMPChartActivity::class.java)
            intent.putExtra(ExtraKeyConfig.TIME_MILLIS, record.startTime)
            intent.putExtra(ExtraKeyConfig.MONITOR_TYPE, record.type)
            context?.startActivity(intent)
        }
        adapter.onItemLongClickListener = {
            TipDialog.Builder(requireContext())
                .setMessage(getString(R.string.tip_config_delete, ""))
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.delDetail(adapter.data[it].startTime)
                    adapter.removeAt(it)
                }
                .setCancelListener(R.string.app_cancel) {

                }
                .create().show()
        }
        adapter.loadMoreModule.setOnLoadMoreListener {
            adapter.loadMoreModule.loadMoreEnd()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        adapter.isUseEmpty = true
        viewModel.recordListLD.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                if (!adapter.hasEmptyView()){
                    adapter.setEmptyView(R.layout.layout_empty)
                }
                withContext(Dispatchers.IO){
                    var lastTime = 0L
                    val nowCalendar = Calendar.getInstance()
                    val lastCalendar = Calendar.getInstance()
                    for (tmp in it){
                        if (lastTime == 0L){
                            tmp.showTitle = true
                        }
                        nowCalendar.timeInMillis = tmp.startTime
                        lastCalendar.timeInMillis = lastTime
                        if (nowCalendar.get(Calendar.MONTH) != lastCalendar.get(Calendar.MONTH)){
                            tmp.showTitle = true
                        }
                        lastTime = tmp.startTime
                    }
                }
                adapter.setNewInstance(it as MutableList<ThermalDao.Record>?)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.queryRecordList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorCreateEvent) {
        viewModel.queryRecordList()
    }

    private class MyAdapter(dataList: MutableList<ThermalDao.Record>?) : BaseQuickAdapter<ThermalDao.Record,
            BaseViewHolder>(R.layout.item_monitory_history, dataList), LoadMoreModule {

        /**
         * item 点击事件监听.
         */
        var onItemClickListener: ((position: Int) -> Unit)? = null
        /**
         * item 长按事件监听.
         */
        var onItemLongClickListener: ((position: Int) -> Unit)? = null


        override fun convert(holder: BaseViewHolder, item: ThermalDao.Record) {
            val position = data.indexOf(item)
            val record: ThermalDao.Record = data[position]
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = record.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day =  calendar.get(Calendar.DAY_OF_MONTH)

            // Use findViewById for views
            val groupTitle = holder.itemView.findViewById<View>(R.id.group_title)
            val viewLineTop = holder.itemView.findViewById<View>(R.id.view_line_top)
            val tvDate = holder.itemView.findViewById<View>(R.id.tv_date)
            val tvTime = holder.itemView.findViewById<View>(R.id.tv_time)
            val tvDuration = holder.itemView.findViewById<View>(R.id.tv_duration)
            val tvType = holder.itemView.findViewById<View>(R.id.tv_type)
            val viewContentBg = holder.itemView.findViewById<View>(R.id.view_content_bg)

            if (item.showTitle || position == 0 || data.size == 1) {
                groupTitle?.isVisible = true
                viewLineTop?.isVisible = false
            } else {
                val beforeCalendar = Calendar.getInstance()
                beforeCalendar.timeInMillis = data[position - 1].startTime
                val beforeYear = beforeCalendar.get(Calendar.YEAR)
                val beforeMonth = beforeCalendar.get(Calendar.MONTH) + 1
                groupTitle?.isVisible = beforeMonth != month && beforeYear != year
                viewLineTop?.isVisible = beforeMonth != month && beforeYear != year
            }

            (tvDate as? android.widget.TextView)?.text = "$year-$month"
            (tvTime as? android.widget.TextView)?.text = "$month-$day"
            (tvDuration as? android.widget.TextView)?.text = TimeTool.showVideoTime(record.duration * 1000L)
            when (record.type) {
                "point" -> (tvType as? android.widget.TextView)?.setText(R.string.thermal_point)
                "line" -> (tvType as? android.widget.TextView)?.setText(R.string.thermal_line)
                "fence" -> (tvType as? android.widget.TextView)?.setText(R.string.thermal_rect)
            }

            viewContentBg?.setOnClickListener {
                if (position != AndroidRecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(position)
                }
            }
            viewContentBg?.setOnLongClickListener {
                if (position != AndroidRecyclerView.NO_POSITION) {
                    onItemLongClickListener?.invoke(position)
                }
                return@setOnLongClickListener true
            }
        }
    }
}