package com.mpdc4gsr.component.thermal.builder

import com.mpdc4gsr.component.thermal.video.base.GSYVideoPlayer

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

