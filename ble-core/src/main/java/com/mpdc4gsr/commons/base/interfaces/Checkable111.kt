package com.mpdc4gsr.commons.base.interfaces

interface Checkable<T> {
    val isChecked: Boolean

    fun setChecked(isChecked: Boolean): T?
}
