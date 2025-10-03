package com.mpdc4gsr.module.thermalunified.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.view.MyTextView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class OptionPickPopup(
    private val context: Context,
    private val strArray: Array<String>,
    private val resIdArray: Array<Int>? = null,
) : PopupWindow() {

    private val recyclerView: RecyclerView by lazy { contentView.findViewById(R.id.recycler_view) }

    companion object {

        private const val TEXT_SIZE_SP: Float = 14f

        private const val TEXT_PADDING: Float = 7f
    }

    var onPickListener: ((position: Int, str: String) -> Unit)? = null

    init {
        val textView = TextView(context)
        textView.textSize = TEXT_SIZE_SP

        val fontMetrics = textView.paint.fontMetricsInt

        val canSeeItem: Int = strArray.size.coerceAtMost(2)
        val itemHeight: Int =
            fontMetrics.bottom - fontMetrics.top + TEXT_PADDING.dpToPx(context).toInt() * 2
        val contentHeight = 14f.dpToPx(context).toInt() + itemHeight * canSeeItem
        val contentWidth = (contentHeight * 120f / 81f).toInt()

        contentView = LayoutInflater.from(context).inflate(R.layout.popup_option_pick, null)
        width = contentWidth
        height = contentHeight

        isOutsideTouchable = true

        val adapter = MyAdapter()
        adapter.onItemClickListener = {
            dismiss()
            onPickListener?.invoke(it, strArray[it])
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    fun show(anchor: View) {
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = locationArray[0] + anchor.width - width + 5f.dpToPx(context).toInt()

        if (context.resources.displayMetrics.heightPixels - locationArray[1] - anchor.height > height - 5f.dpToPx(context).toInt()
        ) {
            showAtLocation(
                anchor,
                Gravity.NO_GRAVITY,
                x,
                locationArray[1] + anchor.height - 5f.dpToPx(context).toInt()
            )
        } else {
            showAtLocation(
                anchor,
                Gravity.NO_GRAVITY,
                x,
                (locationArray[1] - height + 5f.dpToPx(context).toInt()).coerceAtLeast(0)
            )
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        var onItemClickListener: ((position: Int) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            val textView = MyTextView(context)
            textView.textSize = TEXT_SIZE_SP
            textView.setDrawableHeightPx(18f.spToPx(context).toInt())
            textView.setTextColor(0xffffffff.toInt())
            textView.setPadding(
                14f.dpToPx(context).toInt(),
                TEXT_PADDING.dpToPx(context).toInt(),
                14f.dpToPx(context).toInt(),
                TEXT_PADDING.dpToPx(context).toInt()
            )
            textView.compoundDrawablePadding = 10f.dpToPx(context).toInt()
            textView.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            holder.textView.text = strArray[position]
            if (resIdArray != null && position < resIdArray.size) {
                holder.textView.setOnlyDrawableStart(resIdArray[position])
            } else {
                holder.textView.setOnlyDrawableStart(0)
            }
        }

        override fun getItemCount(): Int = strArray.size

        inner class ViewHolder(val textView: MyTextView) : RecyclerView.ViewHolder(textView) {
            init {
                textView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener?.invoke(position)
                    }
                }
            }
        }
    }
}
