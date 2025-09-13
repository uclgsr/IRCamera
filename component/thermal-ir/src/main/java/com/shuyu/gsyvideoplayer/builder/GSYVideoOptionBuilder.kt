package com.shuyu.gsyvideoplayer.builder

import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer

/**
 * G s y video option builder utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
class GSYVideoOptionBuilder {
    fun setVideoAllCallBack(callback: Any?): GSYVideoOptionBuilder = this

    fun setRotateViewAuto(auto: Boolean): GSYVideoOptionBuilder = this

    fun setLockLand(lock: Boolean): GSYVideoOptionBuilder = this

    fun setShowFullAnimation(show: Boolean): GSYVideoOptionBuilder = this

    fun setNeedLockFull(need: Boolean): GSYVideoOptionBuilder = this

    fun setCacheWithPlay(cache: Boolean): GSYVideoOptionBuilder = this

    fun setVideoTitle(title: String?): GSYVideoOptionBuilder = this

    fun setIsTouchWiget(touch: Boolean): GSYVideoOptionBuilder = this

    fun setUrl(url: String?): GSYVideoOptionBuilder = this

    fun build(player: GSYVideoPlayer): GSYVideoOptionBuilder = this

    companion object {
        @JvmStatic
        fun create(): GSYVideoOptionBuilder = GSYVideoOptionBuilder()
    }
}
