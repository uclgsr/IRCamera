package com.topdon.lib.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.R
import com.topdon.lib.ui.bean.ColorBean
import com.topdon.lib.ui.listener.SingleClickListener
import com.topdon.lib.ui.R as UiR
import com.topdon.menu.R as MenuR

/**
 * Custom Menu six view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
@Deprecated("看起来是旧版 2D 编辑的menu，根本没使用了")
/**
 * MenuSixAdapter provides data binding between data source and UI components.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MenuSixAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var listener: ((index: Int, code: Int) -> Unit)? = null
    private var type = 0
    private var selected = -1
    private var colorEnable = false // pseudo color条
    private var contrastEnable = false 
    private var ddeEnable = false 
    /**
     * Executes selected functionality.
     */
    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    /**
     * Executes encolor functionality.
     */
    fun enColor(colorEnable: Boolean) {
        this.colorEnable = colorEnable
        notifyDataSetChanged()
    }

    /**
     * Executes encontrast functionality.
     */
    fun enContrast(param: Boolean) {
        this.contrastEnable = param
        notifyDataSetChanged()
    }

    /**
     * Executes endde functionality.
     */
    fun enDde(param: Boolean) {
        this.ddeEnable = param
        notifyDataSetChanged()
    }

    private val fourBean =
        arrayListOf(
            ColorBean(MenuR.drawable.selector_menu2_setting_1, context.getString(R.string.thermal_pseudo), 1),
            ColorBean(MenuR.drawable.selector_menu2_setting_2, context.getString(R.string.thermal_contrast), 2),
            ColorBean(MenuR.drawable.selector_menu2_setting_3, context.getString(R.string.thermal_sharpen), 3),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(UiR.layout.ui_item_menu_four_view, parent, false)
        return ItemView(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            val bean = fourBean[position]
            holder.name.text = bean.name
            holder.img.setImageResource(bean.res)
            holder.lay.setOnClickListener(
                object : SingleClickListener() {
                    override fun onSingleClick() {
                        val adapterPosition = holder.adapterPosition
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            listener?.invoke(adapterPosition, bean.code)
                            selected(bean.code)
                        }
                    }
                },
            )
            when (bean.code) {
                1 -> {
                    iconUI(colorEnable, holder.img, holder.name)
                }
                2 -> {
                    iconUI(contrastEnable, holder.img, holder.name)
                }
                3 -> {
                    iconUI(ddeEnable, holder.img, holder.name)
                }
                else -> {
                    iconUI(bean.code == selected, holder.img, holder.name)
                }
            }
        }
    }

    
    private fun iconUI(
        isActive: Boolean,
        img: ImageView,
        nameText: TextView,
    ) {
        img.isSelected = isActive
        if (isActive) {
            nameText.setTextColor(ContextCompat.getColor(context, UiR.color.white))
        } else {
            nameText.setTextColor(ContextCompat.getColor(context, UiR.color.font_third_color))
        }
    }

    override fun getItemCount(): Int {
        return fourBean.size
    }

    inner class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay: View = itemView.findViewById(UiR.id.item_menu_tab_lay)
        val img: ImageView = itemView.findViewById(UiR.id.item_menu_tab_img)
        val name: TextView = itemView.findViewById(UiR.id.item_menu_tab_text)
    }
}
