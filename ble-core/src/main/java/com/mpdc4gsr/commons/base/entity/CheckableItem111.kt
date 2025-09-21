package com.mpdc4gsr.commons.base.entity

import com.mpdc4gsr.commons.base.interfaces.Checkable

open class CheckableItem<T> : Checkable<CheckableItem<T?>?> {
    var data: T? = null
    private var _isChecked = false

    constructor()

    constructor(data: T?) {
        this.data = data
    }

    constructor(data: T?, isChecked: Boolean) {
        this.data = data
        this._isChecked = isChecked
    }

    override val isChecked: Boolean
        get() = _isChecked

    override fun setChecked(isChecked: Boolean): CheckableItem<T?>? {
        this._isChecked = isChecked
        return this
    }
}
