package com.mpdc4gsr.commons.poster

internal interface Poster {
    fun enqueue(runnable: Runnable)

    fun clear()
}
