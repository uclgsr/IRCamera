package com.mpdc4gsr.module.thermalunified.video.base

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

open class GSYVideoPlayer
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    protected var mStartButton: android.view.View? = null
    protected var mCurrentState: Int = CURRENT_STATE_NORMAL
    var isNeedShowWifiTip: Boolean = false
    val titleTextView: android.view.View = android.view.View(context)
    val backButton: android.view.View = android.view.View(context)
    val fullscreenButton: android.view.View = android.view.View(context)
    var fullWindowPlayer: GSYVideoPlayer? = null
    open fun startPlayLogic() {
    }

    open fun release() {
    }

    open fun onBackFullscreen() {
    }

    open fun getCurrentState(): Int = mCurrentState
    open fun updateStartImage() {
    }

    open fun getLayoutId(): Int = 0
    open fun onVideoResume(isResume: Boolean) {
    }

    open fun onVideoPause() {
    }

    companion object {
        const val CURRENT_STATE_NORMAL = 0
        const val CURRENT_STATE_PLAYING = 1
        const val CURRENT_STATE_PAUSE = 2
        const val CURRENT_STATE_ERROR = 3
    }
}
