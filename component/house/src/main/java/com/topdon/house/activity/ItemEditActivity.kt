package com.topdon.house.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.house.event.DetectItemListEvent
import com.topdon.house.viewmodel.DetectViewModel
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.DirDetect
import com.topdon.lib.core.db.entity.ItemDetect
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.ui.widget.MyItemDecoration
import com.topdon.lms.sdk.weiget.TToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.util.Collections

/**
 * 房屋检测 - 项目编辑.
 *
 * 需要传递：
 * - [ExtraKeyConfig.DIR_ID] - 执行检测的房屋检测目录 Id
 *
 * Created by LCG on 2024/8/26.
 */
@SuppressLint("NotifyDataSetChanged")
class ItemEditActivity : BaseActivity(), View.OnClickListener {
    private val adapter = MyAdapter(this)
    private val itemTouchCallback = MyItemTouchCallback()

    private val viewModel: DetectViewModel by viewModels()
    
    // View references
    private lateinit var ivCopy: ImageView
    private lateinit var tvCopy: TextView
    private lateinit var ivDel: ImageView
    private lateinit var tvDel: TextView
    private lateinit var viewCopy: View
    private lateinit var viewDel: View
    private lateinit var ivExit: ImageView
    private lateinit var ivSave: ImageView
    private lateinit var clDir: View
    private lateinit var viewSelectAll: View
    private lateinit var etDirName: EditText
    private lateinit var ivSelectAll: ImageView
    private lateinit var tvSelectAll: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvGoodCount: TextView
    private lateinit var tvWarnCount: TextView
    private lateinit var tvDangerCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var ivTriangle: ImageView
    private lateinit var clEmpty: View
    private lateinit var clBottom: View

    override fun initContentView(): Int = R.layout.activity_item_edit

    override fun initView() {
        // Initialize view references
        ivCopy = findViewById(R.id.iv_copy)
        tvCopy = findViewById(R.id.tv_copy)
        ivDel = findViewById(R.id.iv_del)
        tvDel = findViewById(R.id.tv_del)
        viewCopy = findViewById(R.id.view_copy)
        viewDel = findViewById(R.id.view_del)
        ivExit = findViewById(R.id.iv_exit)
        ivSave = findViewById(R.id.iv_save)
        clDir = findViewById(R.id.cl_dir)
        viewSelectAll = findViewById(R.id.view_select_all)
        etDirName = findViewById(R.id.et_dir_name)
        ivSelectAll = findViewById(R.id.iv_select_all)
        tvSelectAll = findViewById(R.id.tv_select_all)
        tvTitle = findViewById(R.id.tv_title)
        val tvGoodCount = findViewById<TextView>(R.id.tv_good_count)
        val tvWarnCount = findViewById<TextView>(R.id.tv_warn_count)
        val tvDangerCount = findViewById<TextView>(R.id.tv_danger_count)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        ivTriangle = findViewById(R.id.iv_triangle)
        clEmpty = findViewById(R.id.cl_empty)
        clBottom = findViewById(R.id.cl_bottom)
        
        ivCopy.isEnabled = false
        tvCopy.isEnabled = false
        ivDel.isEnabled = false
        tvDel.isEnabled = false
        viewCopy.isEnabled = false
        viewDel.isEnabled = false

        ivExit.setOnClickListener(this)
        ivSave.setOnClickListener(this)
        clDir.setOnClickListener(this)
        viewSelectAll.setOnClickListener(this)
        viewCopy.setOnClickListener(this)
        viewDel.setOnClickListener(this)

        etDirName.addTextChangedListener {
            val dirDetect: DirDetect? = viewModel.dirLD.value
            if (dirDetect != null) {
                dirDetect.dirName = it?.toString() ?: ""
            }
        }

        
        adapter.onSelectChangeListener = {
            ivCopy.isEnabled = it > 0
            tvCopy.isEnabled = it > 0
            ivDel.isEnabled = it > 0
            tvDel.isEnabled = it > 0
            viewCopy.isEnabled = it > 0
            viewDel.isEnabled = it > 0
            ivSelectAll.isSelected = adapter.isSelectAll
            tvSelectAll.setText(if (adapter.isSelectAll) R.string.app_cancel_select_all else R.string.report_select_all)
            tvTitle.text = if (it > 0) getString(R.string.chosen_item, it) else getString(R.string.not_selected)
        }
        adapter.onStateChangeListener = { oldState, newState ->
            val dirDetect: DirDetect? = viewModel.dirLD.value
            if (dirDetect != null) {
                when (oldState) {
                    1 -> dirDetect.goodCount--
                    2 -> dirDetect.warnCount--
                    3 -> dirDetect.dangerCount--
                }
                when (newState) {
                    1 -> dirDetect.goodCount++
                    2 -> dirDetect.warnCount++
                    3 -> dirDetect.dangerCount++
                }
                tvGoodCount.text = dirDetect.getGoodCountStr()
                tvWarnCount.text = dirDetect.getWarnCountStr()
                tvDangerCount.text = dirDetect.getDangerCountStr()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(MyItemDecoration(this).apply { wholeBottom = 16f })
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerView)

        viewModel.dirLD.observe(this) {
            if (it != null) {
                etDirName.setText(it.dirName)
                tvGoodCount.text = it.getGoodCountStr()
                tvWarnCount.text = it.getWarnCountStr()
                tvDangerCount.text = it.getDangerCountStr()
                adapter.refresh(it.itemList)
                itemTouchCallback.refresh(it.itemList)
            }
        }
        viewModel.queryDirById(intent.getLongExtra(ExtraKeyConfig.DIR_ID, 0))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitTipsDialog()
            }
        })
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            ivExit -> showExitTipsDialog()
            ivSave -> {//保存
                val dirDetect: DirDetect = viewModel.dirLD.value ?: return
                showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getInstance().houseDetectDao().refreshDir(dirDetect)
                    withContext(Dispatchers.Main) {
                        TToast.shortToast(this@ItemEditActivity, R.string.tip_save_success)
                        dismissLoadingDialog()
                        EventBus.getDefault().post(DetectItemListEvent(dirDetect.id))
                        delay(100)
                        finish()
                    }
                }
            }
            clDir -> {//展开收起切换
                adapter.isExpand = !adapter.isExpand
                if (adapter.isExpand) {//切换到展开
                    ivTriangle.setImageResource(R.drawable.svg_house_triangle_up)
                    clDir.setBackgroundResource(R.drawable.bg_corners10_top_solid_23202e)
                } else {
                    ivTriangle.setImageResource(R.drawable.svg_house_triangle_down)
                    clDir.setBackgroundResource(R.drawable.bg_corners10_solid_23202e)
                }
                adapter.notifyDataSetChanged()
            }
            viewSelectAll -> {//全选、取消全选
                adapter.isSelectAll = !adapter.isSelectAll
            }
            viewCopy -> {//复制
                adapter.copySelect()
                TToast.shortToast(this@ItemEditActivity, R.string.ts004_copy_success)
            }
            viewDel -> {//删除
                TipDialog.Builder(this)
                    .setTitleMessage(getString(R.string.tips_del_item_title))
                    .setMessage(R.string.tips_del_item_content)
                    .setCancelListener(R.string.app_cancel) {
                    }
                    .setPositiveListener(R.string.report_delete) {
                        clEmpty.isVisible = adapter.isSelectAll
                        clBottom.isVisible = !adapter.isSelectAll
                        clDir.isVisible = !adapter.isSelectAll
                        adapter.delSelect()
                        TToast.shortToast(this@ItemEditActivity, R.string.test_results_delete_success)
                    }
                    .create().show()
            }
        }
    }

    /**
     * 显示退出不保存提示弹框
     */
    private fun showExitTipsDialog() {
        TipDialog.Builder(this)
            .setMessage(LibR.string.diy_tip_save)
            .setPositiveListener(LibR.string.app_exit) {
                finish()
            }
            .setCancelListener(R.string.app_cancel)
            .create().show()
    }

    private class MyItemTouchCallback : ItemTouchHelper.Callback() {
        var dataList: ArrayList<ItemDetect> = ArrayList(0)

        fun refresh(newList: ArrayList<ItemDetect>) {
            dataList = newList
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition

            //刷新 lastItem
            if (fromPosition == dataList.size - 1 || toPosition == dataList.size - 1) {
                if (viewHolder is MyAdapter.ViewHolder) {
                    viewHolder.refreshIsLast(toPosition == dataList.size - 1)
                }
                if (target is MyAdapter.ViewHolder) {
                    target.refreshIsLast(fromPosition == dataList.size - 1)
                }
            }

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(dataList, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(dataList, i, i - 1)
                }
            }
            recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }
    }

    private class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        private var dataList: ArrayList<ItemDetect> = ArrayList(0)

        var isExpand = true
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        /**
         * 当前已选中的数量.
         */
        private var selectCount = 0
        /**
         * 当前是否已全选 true-已全选 false-未全选
         */
        var isSelectAll: Boolean
            get() = selectCount == dataList.size && dataList.size > 0
            set(value) {
                if (value) {//->全选
                    selectCount = dataList.size
                    for (item in dataList) {
                        item.hasSelect = true
                    }
                } else {//全选->取消全选
                    selectCount = 0
                    for (item in dataList) {
                        item.hasSelect = false
                    }
                }
                onSelectChangeListener?.invoke(selectCount)
                notifyItemRangeChanged(0, itemCount)
            }

        /**
         * 一个 item 选中或取消选中事件监听.
         */
        var onSelectChangeListener: ((selectSize: Int) -> Unit)? = null
        /**
         * 一个 item 状态变更事件监听.
         */
        var onStateChangeListener: ((oldState: Int, newState: Int) -> Unit)? = null

        fun refresh(newList: ArrayList<ItemDetect>) {
            dataList = newList
            notifyDataSetChanged()
        }

        /**
         * 删除选中的目录.
         */
        fun delSelect() {
            selectCount = 0
            if (isSelectAll) {
                dataList.clear()
                notifyItemRangeRemoved(0, itemCount)
            } else {
                val isDelLast = dataList.last().hasSelect
                for (i in dataList.size - 1 downTo 0) {
                    if (dataList[i].hasSelect) {
                        val itemDetect: ItemDetect = dataList.removeAt(i)
                        notifyItemRemoved(i)
                        onStateChangeListener?.invoke(itemDetect.state, 0)
                    }
                }
                if (isDelLast) {//最后一个被删除时，旧最后一个需要刷新
                    notifyItemChanged(dataList.size - 1)
                }
            }
            onSelectChangeListener?.invoke(0)
        }

        /**
         * 复制选中的目录.
         */
        fun copySelect() {
            selectCount *= 2
            val selectIndexList: ArrayList<Int> = ArrayList()
            for (i in dataList.indices) {
                if (dataList[i].hasSelect) {
                    selectIndexList.add(i)
                }
            }
            val isCopyLast = dataList.last().hasSelect
            for ((hasAddCount, oldIndex) in selectIndexList.withIndex()) {
                val oldItem: ItemDetect = dataList[oldIndex + hasAddCount]
                val newItem: ItemDetect = oldItem.copyOne(itemName = oldItem.copyName())
                dataList.add(oldIndex + hasAddCount + 1, newItem)
                notifyItemInserted(oldIndex + hasAddCount + 1)
                if (oldItem.state > 0) {
                    onStateChangeListener?.invoke(0, oldItem.state)
                }
            }
            if (isCopyLast) {//复制的内容包含最后一个时，旧的最后一个需要刷新
                notifyItemChanged(dataList.size - 2)
            }
            onSelectChangeListener?.invoke(selectCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_edit_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val itemDetect: ItemDetect = dataList[position]
            holder.ivSelect.isSelected = itemDetect.hasSelect
            holder.etInputName.setText(itemDetect.itemName)
            holder.tvGood.isSelected = itemDetect.state == 1
            holder.tvWarn.isSelected = itemDetect.state == 2
            holder.tvDanger.isSelected = itemDetect.state == 3
            holder.tvState.text = itemDetect.getStateStr(context)
            holder.refreshIsLast(position == dataList.size - 1)
        }

        override fun getItemCount(): Int = if (isExpand) dataList.size else 0

        inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
            val ivSelect: ImageView = rootView.findViewById(R.id.iv_select)
            val etInputName: EditText = rootView.findViewById(R.id.et_input_name)
            val tvGood: TextView = rootView.findViewById(R.id.tv_good)
            val tvWarn: TextView = rootView.findViewById(R.id.tv_warn)
            val tvDanger: TextView = rootView.findViewById(R.id.tv_danger)
            val tvState: TextView = rootView.findViewById(R.id.tv_state)
            
            init {
                rootView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item: ItemDetect = dataList[position]
                        item.hasSelect = !item.hasSelect
                        selectCount += if (item.hasSelect) 1 else -1
                        ivSelect.isSelected = item.hasSelect
                        onSelectChangeListener?.invoke(selectCount)
                    }
                }
                etInputName.addTextChangedListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        dataList[position].itemName = it?.toString() ?: ""
                    }
                }
                tvGood.setOnClickListener {
                    handleStateChange(1)
                }
                tvWarn.setOnClickListener {
                    handleStateChange(2)
                }
                tvDanger.setOnClickListener {
                    handleStateChange(3)
                }
            }

            fun refreshIsLast(isLastItem: Boolean) {
                if (isLastItem) {
                    itemView.setBackgroundResource(R.drawable.bg_corners10_bottom_solid_23202e)
                    itemView.setPadding(0, SizeUtils.dp2px(6f), 0, SizeUtils.dp2px(16f))
                } else {
                    itemView.setBackgroundColor(0xff23202e.toInt())
                    itemView.setPadding(0, SizeUtils.dp2px(6f), 0, SizeUtils.dp2px(6f))
                }
            }

            private fun handleStateChange(newState: Int) {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return
                }
                val itemDetect: ItemDetect = dataList[position]
                if (itemDetect.state == newState) {
                    return
                }
                onStateChangeListener?.invoke(itemDetect.state, newState)
                itemDetect.state = newState

                tvGood.isSelected = itemDetect.state == 1
                tvWarn.isSelected = itemDetect.state == 2
                tvDanger.isSelected = itemDetect.state == 3
                tvState.text = itemDetect.getStateStr(context)
            }
        }
    }
}