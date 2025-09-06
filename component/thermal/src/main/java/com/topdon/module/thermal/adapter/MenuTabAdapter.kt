package com.topdon.module.thermal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.topdon.module.thermal.R

class MenuTabAdapter(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var listener: OnItemClickListener? = null
    private var type = 0
    private var datas = arrayListOf<Int>()
    private var dataStrList = arrayListOf<String>()
    private var selected = -1

    companion object {
        private const val TYPE_ITEM = 300
        private const val TYPE_ITEM_MORE = 301
    }

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    //拍摄
    private val firstMenus = arrayListOf<Int>(
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7001_svg,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7002_svg
    )

    //选框
    private val secondMenus = arrayListOf<Int>(
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6001,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6003,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7001,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7002,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7003,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7004
    )

    //选框
    private val secondMenusStr =
        arrayListOf(
            "点",
            "线",
            "面",
            "添加",
            "全图",
            "删除"
        )

    //选框
    private val fourthMenusStr =
        arrayListOf(
            "旋转",
            "增强",
            "画中画",
            "色带",
        )

    //色彩 - Using available resources as placeholders
    private val thirdMenus = arrayListOf<Int>(
        com.topdon.lib.ui.R.drawable.ic_menu_thermal5003,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6001,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6002,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6003,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7001,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7002,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7003,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7004,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal5003_selected_svg,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal6003_svg
    )

    //设置 - Using available resources as placeholders
    private val fourthMenus = arrayListOf<Int>(
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7001_svg,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7002_svg,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7003_svg,
        com.topdon.lib.ui.R.drawable.ic_menu_thermal7004_svg
    )

    fun initType(type: Int) {
        this.type = type
        datas = when (type) {
            1 -> firstMenus
            2 -> secondMenus
            3 -> thirdMenus
            4 -> fourthMenus
            else -> thirdMenus
        }
        dataStrList = when (type) {
            2 -> secondMenusStr
            4 -> fourthMenusStr
            else -> secondMenusStr
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_menu_tab_view, parent, false)
            ItemView(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_menu_tab_more_view, parent, false)
            ItemMoreView(view)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BaseItemView) {
            holder.img.setImageResource(datas[position])
            holder.lay.setOnClickListener {
                val index = type * 1000 + position + 1
                listener?.onClick(index)
                selected(position)
            }
            holder.img.isSelected = position == selected
            if (holder is ItemView) {
                holder.name.text = dataStrList[position]
                holder.name.isSelected = position == selected
                holder.name.setTextColor(
                    if (position == selected) ContextCompat.getColor(context, com.topdon.lib.core.R.color.white)
                    else ContextCompat.getColor(context, com.topdon.lib.core.R.color.font_third_color)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (type == 3) {
            TYPE_ITEM_MORE
        } else {
            TYPE_ITEM
        }
    }

    open class BaseItemView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var lay: View
        lateinit var img: ImageView
    }

    inner class ItemView(itemView: View) : BaseItemView(itemView) {
        var name: TextView

        init {
            lay = itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.item_menu_tab_lay)
            img = itemView.findViewById<ImageView>(R.id.item_menu_tab_img)
            name = itemView.findViewById<TextView>(R.id.item_menu_tab_text)
        }
    }

    inner class ItemMoreView(itemView: View) : BaseItemView(itemView) {
        init {
            lay = itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.item_menu_tab_more_lay)
            img = itemView.findViewById<ImageView>(R.id.item_menu_tab_more_img)
        }
    }

    interface OnItemClickListener {
        fun onClick(index: Int)
    }


}