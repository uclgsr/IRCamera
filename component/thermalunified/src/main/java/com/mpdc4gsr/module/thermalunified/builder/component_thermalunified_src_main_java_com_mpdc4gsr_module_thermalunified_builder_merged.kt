// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:35


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder\GSYVideoOptionBuilder.kt =====

package com.mpdc4gsr.module.thermalunified.builder

import com.mpdc4gsr.module.thermalunified.video.base.GSYVideoPlayer

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