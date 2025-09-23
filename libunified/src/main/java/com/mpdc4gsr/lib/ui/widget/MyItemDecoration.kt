package com.mpdc4gsr.lib.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MyItemDecoration(private val context: Context) : RecyclerView.ItemDecoration() {
    var wholeBottom: Float = 0f
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        if (wholeBottom > 0) {
            outRect.bottom = wholeBottom.toInt()
        }
    }
}