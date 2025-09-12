package com.shuyu.gsyvideoplayer.video.base

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Minimal stub implementation of GSYVideoPlayer
 * This is a temporary placeholder to resolve build dependencies
 * TODO: Replace with actual GSY Video Player library when available
 */
open class GSYVideoPlayer
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        // Properties needed by MyGSYVideoPlayer and IRVideoGSYActivity
        protected var mStartButton: android.view.View? = null
        protected var mCurrentState: Int = CURRENT_STATE_NORMAL

        // Properties needed by IRVideoGSYActivity
        var isNeedShowWifiTip: Boolean = false
        val titleTextView: android.view.View = android.view.View(context)
        val backButton: android.view.View = android.view.View(context)
        val fullscreenButton: android.view.View = android.view.View(context)
        var fullWindowPlayer: GSYVideoPlayer? = null

        open fun startPlayLogic() {
            // Stub implementation
        }

        open fun release() {
            // Stub implementation
        }

        open fun onBackFullscreen() {
            // Stub implementation
        }

        open fun getCurrentState(): Int = mCurrentState

        // Method that MyGSYVideoPlayer overrides
        open fun updateStartImage() {
            // Stub implementation
        }

        // Method that MyGSYVideoPlayer overrides
        open fun getLayoutId(): Int = 0

        // Methods needed by IRVideoGSYActivity
        open fun onVideoResume(isResume: Boolean) {
            // Stub implementation
        }

        open fun onVideoPause() {
            // Stub implementation
        }

        companion object {
            const val CURRENT_STATE_NORMAL = 0
            const val CURRENT_STATE_PLAYING = 1
            const val CURRENT_STATE_PAUSE = 2
            const val CURRENT_STATE_ERROR = 3
        }
    }
