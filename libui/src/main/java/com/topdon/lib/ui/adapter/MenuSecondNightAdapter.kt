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
import com.topdon.lib.ui.config.CameraHelp
import com.topdon.lib.ui.R as UiR
import com.topdon.menu.R as MenuR

/**
 * Custom Menu second night view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
@Deprecated("旧的high/low temperaturepointmenu，已重构过了")
/**
 * MenuSecondNightAdapter provides data binding between data source and UI components.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MenuSecondNightAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val curMultipleArray: HashMap<Int, Int> by lazy { hashMapOf() }

    var multipleListener: ((Int, Boolean) -> Unit)? = null

    /**
     * Clears data and resets internal state.
     */
    fun clearMultipleSelected() {
        curMultipleArray.clear()
        notifyDataSetChanged()
    }

    private val secondBean =
        arrayListOf(
            ColorBean(
                MenuR.drawable.selector_menu2_temp_point_1,
                context.getString(R.string.main_tab_second_high_temperature_point),
                CameraHelp.TYPE_SET_HIGHTEMP,
            ),
            ColorBean(
                MenuR.drawable.selector_menu2_temp_point_2,
                context.getString(R.string.main_tab_second_low_temperature_point),
                CameraHelp.TYPE_SET_LOWTEMP,
            ),
            ColorBean(MenuR.drawable.selector_menu2_del, context.getString(R.string.thermal_delete), CameraHelp.TYPE_SET_DETELE),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return ItemView(LayoutInflater.from(parent.context).inflate(UiR.layout.ui_item_menu_second_view, parent, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ItemView) {
            holder.img.setImageResource(secondBean[position].res)
            holder.name.text = secondBean[position].name

            holder.itemView.findViewById<View>(UiR.id.item_menu_tab_lay).setOnClickListener {
                multipleChoice(position)
            }

            holder.img.isSelected = curMultipleArray.contains(position)
            holder.name.isSelected = curMultipleArray.contains(position)
            holder.name.setTextColor(
                if (curMultipleArray.contains(position)) {
                    ContextCompat.getColor(context, UiR.color.white)
                } else {
                    ContextCompat.getColor(context, UiR.color.font_third_color)
                },
            )
        }
    }

    /**
     * Executes multiplechoice functionality.
     */
    private fun multipleChoice(position: Int) {
        // 1.calculationcurMultipleArray
        if (position == secondBean.size - 1) {
            curMultipleArray.clear()
            curMultipleArray[position] = secondBean[position].code
        } else {
            if (curMultipleArray.contains(position)) {
                curMultipleArray.remove(position)
            } else {
                curMultipleArray[position] = secondBean[position].code
            }
            if (curMultipleArray.contains(secondBean.size - 1)) {
                curMultipleArray.remove(secondBean.size - 1)
            }
        }
        // 2.执行listener
        multipleListener?.invoke(secondBean[position].code, curMultipleArray.contains(position))
        // 3.refreshdata
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return secondBean.size
    }

/**
 * Custom Item view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * ItemView implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class ItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lay: View = itemView.findViewById(UiR.id.item_menu_tab_lay)
        val img: ImageView = itemView.findViewById(UiR.id.item_menu_tab_img)
        val name: TextView = itemView.findViewById(UiR.id.item_menu_tab_text)
    }
}
