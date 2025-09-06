package com.topdon.house.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.lib.core.db.entity.HouseBase
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.lib.core.tools.TimeTool

/**
 * 检测 及 报告 列表所用 Adapter.
 *
 * Created by LCG on 2024/8/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class HouseAdapter(val context: Context, val isDetect: Boolean) : RecyclerView.Adapter<HouseAdapter.ViewHolder>() {
    var dataList: ArrayList<HouseBase> = ArrayList()

    /**
     * 当前是否处于编辑模式.
     */
    var isEditMode: Boolean = false
        set(value) {
            field = value
            selectIndexList.clear()
            onSelectChangeListener?.invoke(0)
            notifyItemRangeChanged(0, itemCount)
        }
    /**
     * 仅当处于编辑模式时，当前选中的 item index 列表.
     */
    var selectIndexList: ArrayList<Int> = ArrayList()

    /**
     * 更多被点击事件监听.
     */
    var onMoreClickListener: ((position: Int, v: View) -> Unit)? = null
    /**
     * 仅报告列表时，分享被点击事件监听.
     */
    var onShareClickListener: ((position: Int) -> Unit)? = null
    /**
     * item 点击事件监听.
     */
    var onItemClickListener: ((position: Int) -> Unit)? = null
    /**
     * 一个 item 选中或取消选中事件监听.
     */
    var onSelectChangeListener: ((selectSize: Int) -> Unit)? = null

    /**
     * 使用指定的检测数据刷新整个列表.
     */
    fun refresh(newList: List<HouseBase>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_house_list, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val houseBase: HouseBase = dataList[position]

        holder.ivMenuMore.isVisible = !isEditMode
        holder.ivSelect.isVisible = isEditMode
        holder.ivSelect.isSelected = selectIndexList.contains(position)

        holder.tvAddress.text = houseBase.address
        holder.tvName.text = houseBase.name
        holder.tvDetectTime.text = TimeTool.formatDetectTime(houseBase.detectTime)

        holder.viewSpaceYear.isVisible = houseBase.year != null || houseBase.houseSpace.isNotEmpty()

        holder.tvHouseYear.isVisible = houseBase.year != null
        holder.tvHouseYear.text = houseBase.year.toString()

        holder.tvHouseSpace.isVisible = houseBase.houseSpace.isNotEmpty()
        holder.tvHouseSpace.text = "${houseBase.houseSpace}${houseBase.getSpaceUnitStr()}"

        holder.tvCost.isVisible = houseBase.cost.isNotEmpty()
        holder.tvCostUnit.isVisible = houseBase.cost.isNotEmpty()
        holder.tvCost.text = houseBase.cost
        holder.tvCostUnit.text = houseBase.getCostUnitStr()

        holder.tvDetectShare.setText(if (isDetect) LibR.string.app_detection else LibR.string.battery_share)

        GlideLoader.load(holder.ivHouseImage, houseBase.imagePath)
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val ivMenuMore: ImageView = rootView.findViewById(R.id.iv_menu_more)
        val ivSelect: ImageView = rootView.findViewById(R.id.iv_select)
        val tvAddress: TextView = rootView.findViewById(R.id.tv_address)
        val tvName: TextView = rootView.findViewById(R.id.tv_name)
        val tvDetectTime: TextView = rootView.findViewById(R.id.tv_detect_time)
        val viewSpaceYear: View = rootView.findViewById(R.id.view_space_year)
        val tvHouseYear: TextView = rootView.findViewById(R.id.tv_house_year)
        val tvHouseSpace: TextView = rootView.findViewById(R.id.tv_house_space)
        val tvCost: TextView = rootView.findViewById(R.id.tv_cost)
        val tvCostUnit: TextView = rootView.findViewById(R.id.tv_cost_unit)
        val tvDetectShare: TextView = rootView.findViewById(R.id.tv_detect_share)
        val ivHouseImage: ImageView = rootView.findViewById(R.id.iv_house_image)
        
        init {
            ivMenuMore.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClickListener?.invoke(position, it)
                }
            }
            rootView.setOnClickListener {
                if (isEditMode) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        if (selectIndexList.contains(position)) {//选中->未选中
                            selectIndexList.remove(position)
                            ivSelect.isSelected = false
                        } else {//未选中->选中
                            selectIndexList.add(position)
                            ivSelect.isSelected = true
                        }
                        onSelectChangeListener?.invoke(selectIndexList.size)
                    }
                } else {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(position)
                    }
                }
            }
            if (!isDetect) {
                tvDetectShare.setOnClickListener {
                    if (!isEditMode) {//编辑模式不响应分享事件
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onShareClickListener?.invoke(position)
                        }
                    }
                }
            }
        }
    }
}