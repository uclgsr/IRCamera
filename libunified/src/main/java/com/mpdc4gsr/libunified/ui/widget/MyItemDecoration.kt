package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * RecyclerView ，.
 *
 * Created by LCG on 2023/9/20.
 */
class MyItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    /**
     *  RecyclerView ， dp.
     *  item ， [itemLeft] （）.
     */
    var wholeLeft: Float? = null

    /**
     *  RecyclerView ， dp.
     *  item ， [itemRight] （）.
     */
    var wholeRight: Float? = null

    /**
     *  RecyclerView ， dp.
     *  item ， [itemTop] （）.
     */
    var wholeTop: Float? = null

    /**
     *  RecyclerView ， dp.
     *  item ， [itemBottom] （）.
     */
    var wholeBottom: Float? = null


    /**
     *  item ， dp.
     */
    var itemLeft: Float? = null

    /**
     *  item ， dp.
     */
    var itemRight: Float? = null

    /**
     *  item ， dp.
     */
    var itemTop: Float? = null

    /**
     *  item ， dp.
     */
    var itemBottom: Float? = null


    /**
     * ， dp  px
     */
    private val density: Float = context.resources.displayMetrics.density

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemCount = parent.adapter?.itemCount ?: return
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager

        when (layoutManager) {
            is GridLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMulti(outRect, position, itemCount, layoutManager.spanCount)
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }

            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalOne(outRect, position, itemCount)
                } else {
                    setHorizontalOne(outRect, position, itemCount)
                }
            }

            is StaggeredGridLayoutManager -> {
                val layoutParams = view.layoutParams
                val spanIndex =
                    if (layoutParams is StaggeredGridLayoutManager.LayoutParams) layoutParams.spanIndex else 0
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMultiStaggered(
                        outRect,
                        position,
                        itemCount,
                        layoutManager.spanCount,
                        spanIndex
                    )
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }
        }
    }

    /**
     *  RecyclerView  1 ，.
     * @param itemCount
     */
    private fun setVerticalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int = dp2px(wholeLeft ?: ((itemLeft ?: 0f) * 2))
        val right: Int = dp2px(wholeRight ?: ((itemRight ?: 0f) * 2))
        val top: Int =
            dp2px(if (position == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (position == itemCount - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    /**
     *  RecyclerView  1 ，.
     * @param itemCount
     */
    private fun setHorizontalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int =
            dp2px(if (position == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (position == itemCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(wholeTop ?: ((itemTop ?: 0f) * 2))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }

    /**
     *  RecyclerView ，.
     * @param itemCount
     * @param spanCount ()
     */
    private fun setVerticalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount    // position [0, totalRow)
        val columnPosition = position % spanCount // position [0, spanCount)

        val left: Int = dp2px(
            if (columnPosition == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f)
        )
        val right: Int =
            dp2px(
                if (columnPosition == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    /**
     *  RecyclerView ，.
     * @param itemCount
     * @param spanCount ()
     * @param spanIndex index[0, spanCount)，
     */
    private fun setVerticalMultiStaggered(
        outRect: Rect,
        position: Int,
        itemCount: Int,
        spanCount: Int,
        spanIndex: Int
    ) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount //position[0, totalRow)

        val left: Int =
            dp2px(if (spanIndex == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (spanIndex == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    /**
     *  RecyclerView ，.
     * @param itemCount
     * @param spanCount ()
     */
    private fun setHorizontalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        // MVP implementation: Basic horizontal multi-row spacing
        // Can be enhanced when horizontal multi-row requirements are clarified
        val column = position % spanCount
        val row = position / spanCount

        val left: Int =
            dp2px(if (column == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (column == spanCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(if (row == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }


    private fun dp2px(dpValue: Float): Int = (dpValue * density + 0.5f).toInt()
}