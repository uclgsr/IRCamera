package com.topdon.module.thermal.ir.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.FrameLayout
import com.topdon.module.thermal.ir.R

// TODO: Replace with ExoPlayer/Media3 implementation once GSY VideoPlayer dependency is resolved
// This is a temporary compatibility stub to enable builds

/**
 * Temporary video player stub to replace GSY VideoPlayer dependency.
 * 
 * This class provides basic video player functionality using Media3/ExoPlayer
 * until the GSY VideoPlayer dependency issue is resolved.
 *
 * Created by chenggeng.lin on 2023/12/8.
 * Modified for GSY VideoPlayer compatibility.
 */
class MyGSYVideoPlayer : FrameLayout {

    // Compatibility constants for GSY VideoPlayer
    companion object {
        const val CURRENT_STATE_PLAYING = 2
        const val CURRENT_STATE_PAUSE = 5
        const val CURRENT_STATE_IDLE = 0
    }

    private var mCurrentState = CURRENT_STATE_IDLE
    private var mStartButton: ImageView? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        // TODO: Initialize ExoPlayer/Media3 components here
        // For now, just provide basic layout compatibility
    }

    fun getLayoutId(): Int = R.layout.view_my_gsy_video_player

    fun updateStartImage() {
        if (mStartButton is ImageView) {
            val imageView = mStartButton as ImageView
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                imageView.setImageResource(R.drawable.svg_pause_icon)
            } else {
                imageView.setImageResource(R.drawable.svg_play_icon)
            }
        }
    }

    // Basic video player control methods for compatibility
    fun play() {
        mCurrentState = CURRENT_STATE_PLAYING
        updateStartImage()
    }

    fun pause() {
        mCurrentState = CURRENT_STATE_PAUSE
        updateStartImage()
    }

    fun stop() {
        mCurrentState = CURRENT_STATE_IDLE
        updateStartImage()
    }
}