package com.mpdc4gsr.component.shared.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    var listener: CallBack? = null

    fun clear() {
        // Clear fence view state
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            endPoint: IntArray,
            srcRect: IntArray,
        )
    }
}


