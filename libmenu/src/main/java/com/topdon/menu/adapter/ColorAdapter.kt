package com.topdon.menu.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topdon.menu.util.PseudoColorConfig
import com.topdon.menu.view.ColorView

/**
 * Temperature measurement mode - menu 3 - pseudo color / observation mode - menu 4 - pseudo color adapter.
 * Supports single selection only.
 *
 * Created by LCG on 2024/11/12.
 */
@SuppressLint("NotifyDataSetChanged")
internal class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
    /**
     * Currently selected pseudo color code.
     */
    var selectCode = -1
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Selection change event listener.
     * @param index Selected pseudo color index in list (used by TC007)
     * @param code Pseudo color code (legacy - cannot be changed due to 2D editing data and saved settings)
     * @param size Preset pseudo color quantity (used by TC007)
     */
    var onColorListener: ((index: Int, code: Int, size: Int) -> Unit)? = null

    /**
     * The origin of this code is unclear. Cannot be changed due to legacy constraints 
     * (2D editing data and saved settings pseudo color switch are all saved according to this).
     * Color mappings: 1-White Hot, 3-Iron Red, 4-Rainbow 1, 5-Rainbow 2, 6-Rainbow 3, 
     * 7-Red Hot, 8-Hot Iron, 9-Rainbow 4, 10-Rainbow 5, 11-Black Hot
     */
    private val colorCodeArray: IntArray = intArrayOf(1, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        // According to UI design, width to screen width ratio is 62:375
        val width: Int = (parent.context.resources.displayMetrics.widthPixels * 62f / 375).toInt()
        val colorView = ColorView(parent.context)
        colorView.layoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        return ViewHolder(colorView)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val code: Int = colorCodeArray[position]
        holder.colorView.isSelected = code == selectCode
        holder.colorView.refreshColor(PseudoColorConfig.getColors(code), PseudoColorConfig.getPositions(code))
        holder.colorView.setOnClickListener {
            if (selectCode != code) {
                selectCode = code
                onColorListener?.invoke(position, code, colorCodeArray.size)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = colorCodeArray.size

    /**
     * ViewHolder(val class
     */
/**
 * Custom View holder view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * ViewHolder implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class ViewHolder(val colorView: ColorView) : RecyclerView.ViewHolder(colorView)
}
