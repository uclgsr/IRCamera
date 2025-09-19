package com.mpdc4gsr.lib.core.menu.adapter

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mpdc4gsr.lib.core.R
import com.mpdc4gsr.menu.constant.TargetType
import com.mpdc4gsr.menu.R as MenuR

@SuppressLint("NotifyDataSetChanged")
internal class TargetAdapter : BaseMenuAdapter() {

    var onTargetListener: ((targetType: TargetType) -> Unit)? = null

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
            Data(
                R.string.main_tab_second_measure_mode,
                MenuR.drawable.selector_menu2_target_1_person,
                TargetType.MODE
            ),
            Data(
                R.string.main_tab_first_target,
                MenuR.drawable.selector_menu2_target_2_style,
                TargetType.STYLE
            ),
            Data(
                R.string.main_tab_second_target_color,
                MenuR.drawable.selector_menu2_target_3_color,
                TargetType.COLOR
            ),
            Data(R.string.thermal_delete, MenuR.drawable.selector_menu2_del, TargetType.DELETE),
            Data(
                R.string.main_tab_second_target_help,
                MenuR.drawable.selector_menu2_target_4_help,
                TargetType.HELP
            ),
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


            onTargetListener?.invoke(data.targetType)
        }
    }

    override fun getItemCount(): Int = dataArray.size

    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes var drawableId: Int,
        val targetType: TargetType,
        var isSelected: Boolean = false,
    )
}
