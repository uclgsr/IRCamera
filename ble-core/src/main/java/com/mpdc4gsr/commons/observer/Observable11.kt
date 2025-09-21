package com.mpdc4gsr.commons.observer

import android.util.Log
import com.mpdc4gsr.commons.poster.MethodInfo
import com.mpdc4gsr.commons.poster.PosterDispatcher
import java.util.Objects

class Observable(val posterDispatcher: PosterDispatcher, isObserveAnnotationRequired: Boolean) {
    private val observerInfos: MutableList<ObserverInfo> = ArrayList<ObserverInfo>()
    private val helper: ObserverMethodHelper

    init {
        helper = ObserverMethodHelper(isObserveAnnotationRequired)
    }

    fun registerObserver(observer: Observer) {
        Objects.requireNonNull<Observer?>(observer, "observer can't be null")
        synchronized(observerInfos) {
            var registered = false
            val it = observerInfos.iterator()
            while (it.hasNext()) {
                val info = it.next()
                val o = info.weakObserver.get()
                if (o == null) {
                    it.remove()
                } else if (o === observer) {
                    registered = true
                }
            }
            if (registered) {
                Log.e("Observable", "", Error("Observer " + observer + " is already registered."))
                return
            }
            val methodMap = helper.findObserverMethod(observer)
            observerInfos.add(ObserverInfo(observer, methodMap))
        }
    }

    fun isRegistered(observer: Observer): Boolean {
        synchronized(observerInfos) {
            for (info in observerInfos) {
                if (info.weakObserver.get() === observer) {
                    return true
                }
            }
            return false
        }
    }

    fun unregisterObserver(observer: Observer) {
        synchronized(observerInfos) {
            val it = observerInfos.iterator()
            while (it.hasNext()) {
                val info = it.next()
                val o = info.weakObserver.get()
                if (o == null || observer === o) {
                    it.remove()
                }
            }
        }
    }

    fun unregisterAll() {
        synchronized(observerInfos) {
            observerInfos.clear()
        }
        helper.clearCache()
    }

    private fun getObserverInfos(): MutableList<ObserverInfo> {
        synchronized(observerInfos) {
            val infos = ArrayList<ObserverInfo>()
            for (info in observerInfos) {
                val observer = info.weakObserver.get()
                if (observer != null) {
                    infos.add(info)
                }
            }
            return infos
        }
    }

    fun notifyObservers(methodName: String, vararg parameters: MethodInfo.Parameter?) {
        notifyObservers(MethodInfo(methodName, *parameters))
    }

    fun notifyObservers(info: MethodInfo) {
        val infos = getObserverInfos()
        for (oi in infos) {
            val observer = oi.weakObserver.get()
            if (observer != null) {
                val key = helper.generateKey(info.tag, info.name, info.parameterTypes)
                val method = oi.methodMap?.get(key)
                if (method != null) {
                    val runnable = helper.generateRunnable(observer, method, info)
                    posterDispatcher.post(method, runnable)
                }
            }
        }
    }
}
