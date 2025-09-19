package com.mpdc4gsr.lib.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.ui.bean.ColorSelectBean
import com.topdon.lib.ui.databinding.UiItemColorSelectBinding
import com.topdon.lib.ui.R as UiR


class ColorSelectAdapter(val context: Context) :
    RecyclerView.Adapter<ColorSelectAdapter.ItemView>() {
    var listener: ((code: Int, color: Int) -> Unit)? = null
    private var type = 0
    private var selected = -1

    fun selected(index: Int) {
        selected = index
        notifyDataSetChanged()
    }

    private val colorBean =
        arrayListOf(
            ColorSelectBean(UiR.color.color_select1, "#FF000000", 1),
            ColorSelectBean(UiR.color.color_select2, "#FFFFFFFF", 2),
            ColorSelectBean(UiR.color.color_select3, "#FF2B79D8", 3),
            ColorSelectBean(UiR.color.color_select4, "#FFFF0000", 4),
            ColorSelectBean(UiR.color.color_select5, "#FF0FA752", 5),
            ColorSelectBean(UiR.color.color_select6, "#FF808080", 6),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemView {
        val binding =
            UiItemColorSelectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ItemView(binding)
    }

    override fun onBindViewHolder(
        holder: ItemView,
        position: Int,
    ) {
        with(holder.binding) {
            itemColorImg.setImageResource(colorBean[position].colorRes)
            itemColorLay.setOnClickListener {
                listener?.invoke(position, Color.parseColor(colorBean[position].color))
                selected(position)
            }
            itemColorImg.isSelected = position == selected
            itemColorCheck.visibility = if (position == selected) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int {
        return colorBean.size
    }

    inner class ItemView(val binding: UiItemColorSelectBinding) :
        RecyclerView.ViewHolder(binding.root)
}
