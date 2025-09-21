package com.mpdc4gsr.commons.observer

interface Observer {
    @Observe
    fun onChanged(o: Any?) {
    }
}
