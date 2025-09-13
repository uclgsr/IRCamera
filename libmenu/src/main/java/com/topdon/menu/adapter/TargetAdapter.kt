package com.topdon.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.menu.R as MenuR
import com.topdon.menu.constant.TargetType

/**
 * observationmode-menu4-target menuAdapter used for.
 *
 * Measurement mode (MODE), target (STYLE), target color (COLOR), delete (DELETE), help (HELP)
 *
 * - Measurement mode (MODE) and target (STYLE) are bundled - either all selected or all unselected, mutually exclusive with delete (DELETE)
 * - Delete (DELETE) is mutually exclusive with {measurement mode (MODE), target (STYLE), target color (COLOR)}
 * - Target color (COLOR) effective and not in delete highlight, color is default green or not highlighted when delete. Left to upper layer to maintain this state.
 * - Help (HELP) show/display dialog highlighted, close dialog not highlighted. Left to upper layer to maintain this state.
 *
 * Created by LCG on 2024/11/28.
 */
@SuppressLint("NotifyDataSetChanged")
internal class TargetAdapter : BaseMenuAdapter() {
    /**
     * Observation mode - Menu 4 - Target click event listener.
     */
    var onTargetListener: ((targetType: TargetType) -> Unit)? = null

    /**
     * Settings specified option selected state.
     * For some mutually exclusive select/cancel select operations, not changed for now due to legacy. Left to upper layer to maintain this mutually exclusive state.
     */
    fun setSelected(
        targetType: TargetType,
        isSelected: Boolean,
    ) {
        for (i in dataArray.indices) {
            if (dataArray[i].targetType == targetType) {
                dataArray[i].isSelected = isSelected
                notifyItemChanged(i)
                break
            }
        }
    }

    /**
     * Set icon type for Observation mode - Menu 4 - Target - Measurement mode.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Human: 10
     * - Sheep: 11
     * - Dog: 12
     * - Bird: 13
     */
    fun setTargetMode(modeCode: Int) {
        for (i in dataArray.indices) {
            if (dataArray[i].targetType == TargetType.MODE) {
                dataArray[i].drawableId =
                    when (modeCode) {
                        11 -> MenuR.drawable.selector_menu2_target_1_sheep
                        12 -> MenuR.drawable.selector_menu2_target_1_dog
                        13 -> MenuR.drawable.selector_menu2_target_1_bird
                        else -> MenuR.drawable.selector_menu2_target_1_person
                    }
                notifyItemChanged(i)
                break
            }
        }
    }

    private val dataArray: Array<Data> =
        arrayOf(
            Data(R.string.main_tab_second_measure_mode, MenuR.drawable.selector_menu2_target_1_person, TargetType.MODE),
            Data(R.string.main_tab_first_target, MenuR.drawable.selector_menu2_target_2_style, TargetType.STYLE),
            Data(R.string.main_tab_second_target_color, MenuR.drawable.selector_menu2_target_3_color, TargetType.COLOR),
            Data(R.string.thermal_delete, MenuR.drawable.selector_menu2_del, TargetType.DELETE),
            Data(R.string.main_tab_second_target_help, MenuR.drawable.selector_menu2_target_4_help, TargetType.HELP),
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
            // Target color is only considered highlight selected when effective. Maintain original code logic here.
            // Menu selection refresh left to upper-layer listener to handle. Consider changes later when time permits.
//            data.isSelected = !data.isSelected
//            holder.binding.ivIcon.isSelected = data.isSelected
//            holder.binding.tvText.isSelected = data.isSelected
            onTargetListener?.invoke(data.targetType)
        }
    }

    override fun getItemCount(): Int = dataArray.size

/**
 * Custom Data view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes var drawableId: Int,
        val targetType: TargetType,
        var isSelected: Boolean = false,
    )
}
