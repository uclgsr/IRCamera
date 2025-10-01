package com.mpdc4gsr.libunified.app.menu.adapter

/*
 * DEPRECATED: This file has been deprecated as part of migration to Jetpack Compose.
 * This code is commented out to avoid compilation errors when dataBinding is disabled.
 * See COMPOSE_MIGRATION.md for alternatives.
 */

/*

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.R

@SuppressLint("NotifyDataSetChanged")
internal class TempSourceAdapter : BaseMenuAdapter() {

    var selectCode: Int = -1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var onTempSourceListener: ((code: Int) -> Unit)? = null

    private val dataArray: Array<Data> =
        arrayOf(
            Data(
                R.string.main_tab_second_dynamic_recognition,
                R.drawable.selector_menu2_source_1_auto,
                0
            ),
            Data(
                R.string.main_tab_second_high_temperature_source,
                R.drawable.selector_menu2_source_2_high,
                1
            ),
            Data(
                R.string.main_tab_second_low_temperature_source,
                R.drawable.selector_menu2_source_3_low,
                2
            ),
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

    data class Data(
        @StringRes val stringId: Int,
        @DrawableRes val drawableId: Int,
        val code: Int,
    )
}
*/
