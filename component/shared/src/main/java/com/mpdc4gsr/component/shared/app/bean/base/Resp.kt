package com.mpdc4gsr.component.shared.app.bean.base

import android.text.TextUtils

class Resp<T> {
    var code: String = ""
    var msg: String = ""
    var data: T? = null

    fun isSuccess(): Boolean = TextUtils.equals(code, "0")

    override fun toString(): String = "Resp(code='$code', msg='$msg', data=$data)"
}


