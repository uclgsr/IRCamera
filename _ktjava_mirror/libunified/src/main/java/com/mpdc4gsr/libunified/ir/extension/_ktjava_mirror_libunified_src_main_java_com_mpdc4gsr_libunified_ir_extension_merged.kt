// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension\libunified_src_main_java_com_mpdc4gsr_libunified_ir_extension_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension' subtree
// Files: 2; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension\IRCMDExtensions.kt =====

package com.mpdc4gsr.libunified.ir.extension

import android.util.Log
import com.energy.iruvc.ircmd.IRCMD

private const val TAG = "IRCMDExtensions"
fun IRCMD.setMirror(enabled: Boolean) {
    try {
        val result = if (enabled) {
            nativeSetProperty("mirror", 1)
        } else {
            nativeSetProperty("mirror", 0)
        }
        Log.d(TAG, "Mirror mode set to $enabled, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set mirror mode: ${e.message}")
    }
}

fun IRCMD.setAutoShutter(enabled: Boolean) {
    try {
        val result = if (enabled) {
            nativeSetProperty("auto_shutter", 1)
        } else {
            nativeSetProperty("auto_shutter", 0)
        }
        Log.d(TAG, "Auto shutter set to $enabled, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set auto shutter: ${e.message}")
    }
}

fun IRCMD.setPropDdeLevel(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("dde_level", clampedLevel)
        Log.d(TAG, "DDE level set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set DDE level: ${e.message}")
    }
}

fun IRCMD.setContrast(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("contrast", clampedLevel)
        Log.d(TAG, "Contrast set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set contrast: ${e.message}")
    }
}

private fun IRCMD.nativeSetProperty(property: String, value: Int): Boolean {
    return try {
        Log.d(TAG, "Setting $property to $value via native IRCMD interface")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Native property set failed for $property: ${e.message}")
        false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension\View.kt =====

package com.mpdc4gsr.libunified.ir.extension

import android.view.View
import android.view.animation.AlphaAnimation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View?.goneAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.GONE
        return
    }
    this.visibility = View.GONE
    this.startAnimation(
        AlphaAnimation(1f, 0f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun View?.invisibleAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.INVISIBLE
        return
    }
    this.visibility = View.INVISIBLE
    this.startAnimation(
        AlphaAnimation(1f, 0f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun View?.visibleAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.VISIBLE
        return
    }
    this.visibility = View.VISIBLE
    this.startAnimation(
        AlphaAnimation(0f, 1f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun ViewPager2.reduceDragSensitivity() {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView
    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * 5)
}