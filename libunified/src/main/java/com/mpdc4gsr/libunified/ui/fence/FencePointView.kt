package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FencePointView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    var listener: CallBack? = null

    fun clear() {
        // Clear fence point view state
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            srcRect: IntArray,
        )
    }
}
