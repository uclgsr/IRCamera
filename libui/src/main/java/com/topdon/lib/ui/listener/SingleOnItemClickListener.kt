package com.topdon.lib.ui.listener

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener

/**
 * @author: CaiSongL
 * @date: 2023/4/18 10:12
 */
/**
 * Custom Single on item click listener view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
abstract class SingleOnItemClickListener : OnItemClickListener {
    private var mLastClickTime: Long = 0
    private var timeInterval = 500L

    constructor() {}
    constructor(interval: Long) {
        timeInterval = interval
    }

    override fun onItemClick(
        adapter: BaseQuickAdapter<*, *>,
        view: View,
        position: Int,
    ) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - mLastClickTime > timeInterval) {
            onSingleItemClick(adapter, view, position)
            mLastClickTime = nowTime
        }
    }

    protected abstract fun onSingleItemClick(
        adapter: BaseQuickAdapter<*, *>,
        view: View,
        position: Int,
    )
}
