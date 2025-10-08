// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:42


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_builder_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder' subtree
// Files: 1; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\builder\GSYVideoOptionBuilder.kt =====

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