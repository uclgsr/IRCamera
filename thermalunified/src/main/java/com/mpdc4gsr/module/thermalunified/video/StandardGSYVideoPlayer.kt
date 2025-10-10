package com.mpdc4gsr.module.thermalunified.video

import android.content.Context
import android.util.AttributeSet
import com.mpdc4gsr.module.thermalunified.video.base.GSYVideoPlayer

open class StandardGSYVideoPlayer
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : GSYVideoPlayer(context, attrs, defStyleAttr)
