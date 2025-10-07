// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\NoBodyEntity.kt =====

package com.mpdc4gsr.libunified.app.bean.base

class NoBodyEntity


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\base\Resp.kt =====

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


