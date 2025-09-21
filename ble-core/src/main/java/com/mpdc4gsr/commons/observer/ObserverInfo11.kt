package com.mpdc4gsr.commons.observer

import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal class ObserverInfo(observer: Observer?, val methodMap: MutableMap<String?, Method?>?) {
    val weakObserver: WeakReference<Observer?>

    init {
        weakObserver = WeakReference<Observer?>(observer)
    }
}
