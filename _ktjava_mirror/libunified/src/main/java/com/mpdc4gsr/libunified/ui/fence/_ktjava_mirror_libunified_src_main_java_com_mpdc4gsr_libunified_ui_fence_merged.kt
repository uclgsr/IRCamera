// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\libunified_src_main_java_com_mpdc4gsr_libunified_ui_fence_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence' subtree
// Files: 3; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FenceLineView.kt =====

package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceLineView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence line view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FencePointView.kt =====

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
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence point view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, srcRect: IntArray)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FenceView.kt =====

package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray)
    }
}