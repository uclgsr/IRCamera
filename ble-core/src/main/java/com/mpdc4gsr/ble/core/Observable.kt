package com.mpdc4gsr.ble.core

/**
 * Observable pattern interface for BLE events
 */
interface Observable {
    fun notifyObservers(event: Any)
    fun addObserver(observer: Any)
    fun removeObserver(observer: Any)
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

    override fun addObserver(observer: Any) {
        observers.add(observer)
    }

    override fun removeObserver(observer: Any) {
        observers.remove(observer)
    }
}