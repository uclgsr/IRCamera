package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.menu.R as MenuR
import com.topdon.menu.constant.MenuType

/**
 * Temperature measurement mode - menu 6 - high/low temperature level menu adapter. Single selection with one item required to be selected.
 *
 * Low temperature level (high gain), high temperature level (low gain), auto switch
 *
 * Created by LCG on 2024/11/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class TempLevelAdapter(menuType: MenuType) : BaseMenuAdapter() {
    /**
     * Whether to use Fahrenheit as unit
     *
     * true-Fahrenheit false-Celsius
     */
    var isUnitF = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Currently selected level code.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Auto switch: -1
     * - High temperature (low gain): 0
     * - Normal temperature (high gain): 1
     */
    var selectCode: Int = 1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Menu click event listener. Single selection.
     */
    var onTempLevelListener: ((code: Int) -> Unit)? = null

    private val dataList: ArrayList<Data> = ArrayList(6)

    init {
        dataList.add(Data(R.string.thermal_normal_temperature, MenuR.drawable.selector_menu2_temp_level_1, IntRange(-20, 150), 1))
        if (menuType == MenuType.Lite) {
            dataList.add(Data(R.string.thermal_high_temperature, MenuR.drawable.selector_menu2_temp_level_1, IntRange(150, 450), 0))
        } else {
            dataList.add(Data(R.string.thermal_high_temperature, MenuR.drawable.selector_menu2_temp_level_1, IntRange(150, 550), 0))
        }
        dataList.add(Data(R.string.thermal_automatic, MenuR.drawable.selector_menu2_temp_level_2, code = -1))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val data: Data = dataList[position]
        holder.binding.ivIcon.setImageResource(data.drawableId)
        holder.binding.tvText.setText(data.stringId)
        holder.binding.tvIconText.text = data.range?.getTempStr(isUnitF) ?: ""
        holder.binding.ivIcon.isSelected = data.code == selectCode
        holder.binding.tvText.isSelected = data.code == selectCode
        holder.binding.tvIconText.isSelected = data.code == selectCode
        holder.binding.clRoot.setOnClickListener {
            if (data.code != selectCode) {
                selectCode = data.code
                onTempLevelListener?.invoke(data.code)
            }
        }
    }

    /**
     * Executes intrange functionality.
     */
    private fun IntRange.getTempStr(isUnitF: Boolean): String =
        if (isUnitF) {
            "${c2f(start)}\n~\n${c2f(endInclusive)}°F"
        } else {
            "${start}\n~\n$endInclusive°C"
        }

    /**
     * Convert specified Celsius°C to Fahrenheit°F
     */
    private fun c2f(cValue: Int): Int = (cValue * 1.8f + 32).toInt()

    override fun getItemCount(): Int = dataList.size

/**
 * Custom Data view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes val drawableId: Int,
        val range: IntRange? = null,
        val code: Int,
    )
}
