package com.topdon.lib.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.R
import com.topdon.lib.core.bean.ObserveBean
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.lib.ui.databinding.UiItemMenuSecondViewBinding
import com.topdon.lib.ui.R as UiR
import com.topdon.menu.R as MenuR

@Deprecated("旧的高低温源菜单，已重构过了")
class MenuAIAdapter(val context: Context) : RecyclerView.Adapter<MenuAIAdapter.ItemView>() {
    /**
     * 当前选中的选项 code.
     *
     * 由于历史遗留（已保存在 SharedPreferences 中），这里 code 取值为
     * - 什么都未选中：-1
     * - 动态识别：0
     * - 高温源：1
     * - 低温源：2
     */
    var selectCode: Int = -1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * 观测模式-菜单2-高低温源 点击事件监听，单选。
     */
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
        val binding = UiItemMenuSecondViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class ItemView(val binding: UiItemMenuSecondViewBinding) : RecyclerView.ViewHolder(binding.root)
}
