package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.menu.R as MenuR

/**
 * Observation mode - menu 2 - high/low temperature source menu adapter.
 * Single selection with option to have all unselected.
 *
 * Options: dynamic recognition, high temperature source, low temperature source
 *
 * Created by LCG on 2024/11/29.
 */
@SuppressLint("NotifyDataSetChanged")
internal class TempSourceAdapter : BaseMenuAdapter() {
    /**
     * Currently selected option code.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Nothing selected: -1
     * - Dynamic recognition: 0
     * - High temperature source: 1
     * - Low temperature source: 2
     */
    var selectCode: Int = -1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Observation mode - Menu 2 - High/Low temperature source click event listener. Single selection.
     */
    var onTempSourceListener: ((code: Int) -> Unit)? = null

    private val dataArray: Array<Data> =
        arrayOf(
            Data(R.string.main_tab_second_dynamic_recognition, MenuR.drawable.selector_menu2_source_1_auto, 0),
            Data(R.string.main_tab_second_high_temperature_source, MenuR.drawable.selector_menu2_source_2_high, 1),
            Data(R.string.main_tab_second_low_temperature_source, MenuR.drawable.selector_menu2_source_3_low, 2),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val data: Data = dataArray[position]
        holder.binding.ivIcon.setImageResource(data.drawableId)
        holder.binding.tvText.setText(data.stringId)
        holder.binding.ivIcon.isSelected = data.code == selectCode
        holder.binding.tvText.isSelected = data.code == selectCode
        holder.binding.clRoot.setOnClickListener {
            selectCode = if (data.code == selectCode) -1 else data.code
            onTempSourceListener?.invoke(selectCode)
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
        val code: Int,
    )
}
