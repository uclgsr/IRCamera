package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.mpdc4gsr.module.thermalunified.R

class MyGSYVideoPlayer : FrameLayout {
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
