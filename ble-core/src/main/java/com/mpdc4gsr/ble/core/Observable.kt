package com.mpdc4gsr.ble.core

/**
 * Observable pattern interface for BLE events
 */
interface Observable {
    fun notifyObservers(event: Any)
    fun notifyObservers(methodInfo: com.mpdc4gsr.commons.poster.MethodInfo)
    fun addObserver(observer: Any)
    fun removeObserver(observer: Any)
    fun unregisterAll()
    fun registerObserver(observer: Any)
    fun isRegistered(observer: Any): Boolean
    fun unregisterObserver(observer: Any)
}

/**
 * Default implementation of Observable
 */
class DefaultObservable : Observable {
    private val observers = mutableListOf<Any>()

    override fun notifyObservers(event: Any) {
        observers.forEach { observer ->
            // Event notification logic
        }
    }
    
    override fun notifyObservers(methodInfo: com.mpdc4gsr.commons.poster.MethodInfo) {
        observers.forEach { observer ->
            if (observer is EventObserver) {
                when (methodInfo.name) {
                    "onConnectFailed" -> {
                        val device = methodInfo.parameters?.get(0)?.value as Device
                        val reason = methodInfo.parameters?.get(1)?.value as Int
                        observer.onConnectFailed(device, reason)
                    }
                    "onBluetoothAdapterStateChanged" -> {
                        val state = methodInfo.parameters?.get(0)?.value as Int
                        observer.onBluetoothAdapterStateChanged(state)
                    }
                    "onBluetoothOff" -> {
                        observer.onBluetoothOff()
                    }
                }
            }
        }
    }

    override fun addObserver(observer: Any) {
        observers.add(observer)
    }

    override fun removeObserver(observer: Any) {
        observers.remove(observer)
    }
    
    override fun unregisterAll() {
        observers.clear()
    }
    
    override fun registerObserver(observer: Any) {
        addObserver(observer)
    }
    
    override fun isRegistered(observer: Any): Boolean {
        return observers.contains(observer)
    }
    
    override fun unregisterObserver(observer: Any) {
        removeObserver(observer)
    }
}