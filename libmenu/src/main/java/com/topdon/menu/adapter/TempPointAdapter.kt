package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.menu.R as MenuR
import com.topdon.menu.constant.TempPointType

/**
 * Observation mode - menu 5 - high/low temperature point menu adapter. Following old logic, there exists a state where all are unselected.
 *
 * - High temperature point and low temperature point are independent, support multi-selection
 * - {High temperature point, low temperature point} mutually exclusive with delete
 *
 * Created by LCG on 2024/11/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class TempPointAdapter : BaseMenuAdapter() {
    /**
     * Observation mode - Menu 5 - High/Low temperature points click event listener.
     */
    var onTempPointListener: ((type: TempPointType, isSelected: Boolean) -> Unit)? = null

    /**
     * Settings high temperature point or low temperature point selected state.
     */
    fun setSelected(
        tempPointType: TempPointType,
        isSelected: Boolean,
    ) {
        for (i in dataArray.indices) {
            if (dataArray[i].tempPointType == tempPointType) {
                dataArray[i].isSelected = isSelected
                notifyItemChanged(i)
                break
            }
        }
    }

    /**
     * Clear all menu selected state.
     * Maintain original logic here, consider whether to directly delete selected items later.
     */
    fun clearAllSelect() {
        for (data in dataArray) {
            data.isSelected = false
        }
        notifyDataSetChanged()
    }

    private val dataArray: Array<Data> =
        arrayOf(
            Data(R.string.main_tab_second_high_temperature_point, MenuR.drawable.selector_menu2_temp_point_1, TempPointType.HIGH),
            Data(R.string.main_tab_second_low_temperature_point, MenuR.drawable.selector_menu2_temp_point_2, TempPointType.LOW),
            Data(R.string.thermal_delete, MenuR.drawable.selector_menu2_del, TempPointType.DELETE),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val data: Data = dataArray[position]
        holder.binding.ivIcon.setImageResource(data.drawableId)
        holder.binding.tvText.setText(data.stringId)
        holder.binding.ivIcon.isSelected = data.isSelected
        holder.binding.tvText.isSelected = data.isSelected
        holder.binding.clRoot.setOnClickListener {
            if (data.tempPointType == TempPointType.DELETE) {
                if (!data.isSelected) { // deleting when already selected is useless, only process when unselected
                    for (temp in dataArray) {
                        temp.isSelected = temp.tempPointType == TempPointType.DELETE
                    }
                    notifyDataSetChanged()
                    onTempPointListener?.invoke(TempPointType.DELETE, true)
                }
            } else {
                data.isSelected = !data.isSelected
                holder.binding.ivIcon.isSelected = data.isSelected
                holder.binding.tvText.isSelected = data.isSelected
                if (data.isSelected) { // when high/low temperature points are selected, set "delete" to unselected; when canceling selection, do not couple with delete
                    for (i in dataArray.indices) {
                        if (dataArray[i].tempPointType == TempPointType.DELETE && dataArray[i].isSelected) {
                            dataArray[i].isSelected = false
                            notifyItemChanged(i)
                        }
                    }
                }
                onTempPointListener?.invoke(data.tempPointType, data.isSelected)
            }
        }
    }

    override fun getItemCount(): Int = dataArray.size

/**
 * Custom Data view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes val drawableId: Int,
        val tempPointType: TempPointType,
        var isSelected: Boolean = false,
    )
}
