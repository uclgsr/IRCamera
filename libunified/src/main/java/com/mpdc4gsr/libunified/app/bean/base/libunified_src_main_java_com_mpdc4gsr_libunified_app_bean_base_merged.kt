// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\NoBodyEntity.kt =====

package com.mpdc4gsr.libunified.app.bean.base

class NoBodyEntity


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\Resp.kt =====

package com.mpdc4gsr.libunified.app.bean.base

import android.text.TextUtils

class Resp<T> {
    var code: String = ""
    var msg: String = ""
    var data: T? = null
    fun isSuccess(): Boolean {
        return TextUtils.equals(code, "0")
    }

    override fun toString(): String {
        return "Resp(code='$code', msg='$msg', data=$data)"
    }
}