package com.topdon.lib.ui.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView

/**
 * @author: CaiSongL
 * @date: 2023/4/1 14:44
 */
/**
 * RecyclerViewProxy(val class
 */
/**
 * Custom Recycler view proxy view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * RecyclerViewProxy implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class RecyclerViewProxy(val layoutManager: RecyclerView.LayoutManager) {
    /**
     * Executes attachview functionality.
     */
    fun attachView(view: View?) {
        layoutManager.attachView(view!!)
    }

    /**
     * Executes detachview functionality.
     */
    fun detachView(view: View?) {
        layoutManager.detachView(view!!)
    }

    /**
     * Executes detachandscrapview functionality.
     */
    fun detachAndScrapView(
        view: View?,
        recycler: RecyclerView.Recycler?,
    ) {
        layoutManager.detachAndScrapView(view!!, recycler!!)
    }

    /**
     * Executes detachandscrapattachedviews functionality.
     */
    fun detachAndScrapAttachedViews(recycler: RecyclerView.Recycler?) {
        layoutManager.detachAndScrapAttachedViews(recycler!!)
    }

    /**
     * Executes recycleview functionality.
     */
    fun recycleView(
        view: View?,
        recycler: RecyclerView.Recycler,
    ) {
        recycler.recycleView(view!!)
    }

    /**
     * Removes the specified andrecycleallviews from the system.
     */
    fun removeAndRecycleAllViews(recycler: RecyclerView.Recycler?) {
        layoutManager.removeAndRecycleAllViews(recycler!!)
    }

    val childCount: Int
        get() = layoutManager.childCount
    val itemCount: Int
        get() = layoutManager.itemCount

    fun getMeasuredChildForAdapterPosition(
        position: Int,
        recycler: RecyclerView.Recycler,
    ): View {
        val view = recycler.getViewForPosition(position)
        layoutManager.addView(view)
        layoutManager.measureChildWithMargins(view, 0, 0)
        return view
    }

    /**
     * Executes layoutdecoratedwithmargins functionality.
     */
    fun layoutDecoratedWithMargins(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        layoutManager.layoutDecoratedWithMargins(v!!, left, top, right, bottom)
    }

    fun getChildAt(index: Int): View? {
        return layoutManager.getChildAt(index)
    }

    fun getPosition(view: View?): Int {
        return layoutManager.getPosition(view!!)
    }

    fun getMeasuredWidthWithMargin(child: View): Int {
        val lp = child.layoutParams as MarginLayoutParams
        return layoutManager.getDecoratedMeasuredWidth(child) + lp.leftMargin + lp.rightMargin
    }

    fun getMeasuredHeightWithMargin(child: View): Int {
        val lp = child.layoutParams as MarginLayoutParams
        return layoutManager.getDecoratedMeasuredHeight(child) + lp.topMargin + lp.bottomMargin
    }

    val width: Int
        get() = layoutManager.width
    val height: Int
        get() = layoutManager.height

    /**
     * Executes offsetchildrenhorizontal functionality.
     */
    fun offsetChildrenHorizontal(amount: Int) {
        layoutManager.offsetChildrenHorizontal(amount)
    }

    /**
     * Executes offsetchildrenvertical functionality.
     */
    fun offsetChildrenVertical(amount: Int) {
        layoutManager.offsetChildrenVertical(amount)
    }

    /**
     * Executes requestlayout functionality.
     */
    fun requestLayout() {
        layoutManager.requestLayout()
    }

    /**
     * Initiates the operation or service.
     */
    fun startSmoothScroll(smoothScroller: RecyclerView.SmoothScroller?) {
        layoutManager.startSmoothScroll(smoothScroller)
    }

    /**
     * Removes the specified allviews from the system.
     */
    fun removeAllViews() {
        layoutManager.removeAllViews()
    }
}
