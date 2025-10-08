// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\libunified_src_main_java_com_mpdc4gsr_libunified_ui_listener_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\SingleClickListener.kt =====

package com.mpdc4gsr.libunified.ui.listener

import android.view.View

abstract class SingleClickListener : View.OnClickListener {
    private var lastClickTime: Long = 0
    private val minInterval: Long = 500 // Minimum interval between clicks in milliseconds
    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= minInterval) {
            lastClickTime = currentTime
            onSingleClick()
        }
    }

    abstract fun onSingleClick()
}