package com.mpdc4gsr.lib.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.lib.core.R
import com.mpdc4gsr.lib.core.bean.ObserveBean
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.lib.ui.databinding.UiItemMenuSecondViewBinding
import com.topdon.lib.ui.R as UiR
import com.mpdc4gsr.lib.core.R as MenuR

@Deprecated("旧的high/low temperature源menu，已重构过了")

class MenuAIAdapter(val context: Context) : RecyclerView.Adapter<MenuAIAdapter.ItemView>() {

    var selectCode: Int = -1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var onTempSourceListener: ((code: Int) -> Unit)? = null

    private val secondBean =
        arrayListOf(
            ColorBean(
                MenuR.drawable.selector_menu2_source_1_auto,
                context.getString(R.string.main_tab_second_dynamic_recognition),
                ObserveBean.TYPE_DYN_R,
            ),
            ColorBean(
                MenuR.drawable.selector_menu2_source_2_high,
                context.getString(R.string.main_tab_second_high_temperature_source),
                ObserveBean.TYPE_TMP_H_S,
            ),
            ColorBean(
                MenuR.drawable.selector_menu2_source_3_low,
                context.getString(R.string.main_tab_second_low_temperature_source),
                ObserveBean.TYPE_TMP_L_S,
            ),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemView {
        val binding =
            UiItemMenuSecondViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemView(binding)
    }

    override fun onBindViewHolder(
        holder: ItemView,
        position: Int,
    ) {
        with(holder.binding) {
            itemMenuTabImg.setImageResource(secondBean[position].res)
            itemMenuTabLay.setOnClickListener {
                selectCode = secondBean[position].code
                onTempSourceListener?.invoke(secondBean[position].code)
            }
            itemMenuTabImg.isSelected = secondBean[position].code == selectCode
            itemMenuTabText.text = secondBean[position].name
            itemMenuTabText.isSelected = secondBean[position].code == selectCode
            itemMenuTabText.setTextColor(
                if (secondBean[position].code == selectCode) {
                    ContextCompat.getColor(context, UiR.color.white)
                } else {
                    ContextCompat.getColor(context, UiR.color.font_third_color)
                },
            )
        }
    }

    override fun getItemCount(): Int {
        return secondBean.size
    }

    inner class ItemView(val binding: UiItemMenuSecondViewBinding) :
        RecyclerView.ViewHolder(binding.root)
}
