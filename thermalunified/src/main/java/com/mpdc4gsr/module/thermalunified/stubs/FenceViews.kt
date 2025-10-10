package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            endPoint: IntArray,
            srcRect: IntArray,
        )
    }
}

class FencePointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            srcRect: IntArray,
        )
    }
}

class FenceLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            endPoint: IntArray,
            srcRect: IntArray,
        )
    }
}