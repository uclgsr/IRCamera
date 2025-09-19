package com.mpdc4gsr.lib.ui.adapter

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ScreenUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.lib.ui.bean.ColorBean
import com.mpdc4gsr.lib.ui.listener.SingleClickListener
import com.mpdc4gsr.lib.ui.R as UiR

@Deprecated("旧的dual lightmenu，已重构过了")

class MenuPANightAdapter(
    data: MutableList<ColorBean>,
    layoutId: Int,
    private val isDual: Boolean,
) : BaseQuickAdapter<ColorBean, BaseViewHolder>(layoutId, data) {
    var listener: ((index: Int) -> Unit)? = null

    override fun convert(
        holder: BaseViewHolder,
        item: ColorBean,
    ) {
        if (!isDual) {
            val with = (ScreenUtils.getScreenWidth() / 2)
            holder.itemView.layoutParams =
                ViewGroup.LayoutParams(with, ViewGroup.LayoutParams.WRAP_CONTENT)
            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
            val layoutParams =
                holder.itemView.findViewById<android.widget.ImageView>(UiR.id.item_menu_tab_img).layoutParams
            layoutParams.width = imageSize
            layoutParams.height = imageSize
            holder.itemView.findViewById<android.widget.ImageView>(UiR.id.item_menu_tab_img).layoutParams =
                layoutParams
        } else {
            holder.itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val imageSize = (ScreenUtils.getScreenWidth() * 62 / 375f).toInt()
            val layoutParams =
                holder.itemView.findViewById<android.widget.ImageView>(UiR.id.item_menu_tab_img).layoutParams
            layoutParams.width = imageSize
            layoutParams.height = imageSize
            holder.itemView.findViewById<android.widget.ImageView>(UiR.id.item_menu_tab_img).layoutParams =
                layoutParams
        }
        if (item.isSelect) {
            holder.setImageResource(UiR.id.item_menu_tab_img, item.res)
        } else {
            holder.setImageResource(UiR.id.item_menu_tab_img, item.n_res)
        }
        holder.setText(UiR.id.item_menu_tab_text, item.name)
        holder.itemView.setOnClickListener(
            object : SingleClickListener() {
                override fun onSingleClick() {
                    listener?.invoke(data.indexOf(item))
                }
            },
        )
        if (item.isSelect) {
            holder.setTextColor(
                UiR.id.item_menu_tab_text,
                ContextCompat.getColor(context, UiR.color.white)
            )
        } else {
            holder.setTextColor(
                UiR.id.item_menu_tab_text,
                ContextCompat.getColor(context, UiR.color.font_third_color)
            )
        }
    }
}
