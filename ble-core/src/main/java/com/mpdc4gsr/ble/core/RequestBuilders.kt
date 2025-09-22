package com.mpdc4gsr.ble.core

import java.util.UUID

/**
 * Request builder for BLE operations
 */
interface RequestBuilder<T : RequestBuilder<T>> {
    val tag: String?
    val type: RequestType
    val service: UUID?
    val characteristic: UUID?
    val descriptor: UUID?
    val priority: Int
    val value: Any?
    val callback: RequestCallback?
    val writeOptions: WriteOptions?
    
    fun build(): Request
    fun setCallback(callback: Any): T
    fun setTimeout(timeoutMillis: Long): T
}

/**
 * Generic request builder implementation
 */
open class GenericRequestBuilder<T : GenericRequestBuilder<T>> : RequestBuilder<T> {
    override var tag: String? = null
    override var type: RequestType = RequestType.READ_CHARACTERISTIC
    override var service: UUID? = null
    override var characteristic: UUID? = null
    override var descriptor: UUID? = null
    override var priority: Int = 0
    override var value: Any? = null
    override var callback: RequestCallback? = null
    override var writeOptions: WriteOptions? = null

    @Suppress("UNCHECKED_CAST")
    fun setTag(tag: String?): T {
        this.tag = tag
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setType(type: RequestType): T {
        this.type = type
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setService(service: UUID?): T {
        this.service = service
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setCharacteristic(characteristic: UUID?): T {
        this.characteristic = characteristic
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setDescriptor(descriptor: UUID?): T {
        this.descriptor = descriptor
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setPriority(priority: Int): T {
        this.priority = priority
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setValue(value: Any?): T {
        this.value = value
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setRequestCallback(callback: RequestCallback?): T {
        this.callback = callback
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun setWriteOptions(writeOptions: WriteOptions?): T {
        this.writeOptions = writeOptions
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setCallback(callback: Any): T {
        if (callback is RequestCallback) {
            this.callback = callback
        }
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun setTimeout(timeoutMillis: Long): T {
        return this as T
    }

    override fun build(): Request {
        return GenericRequest(this)
    }
}

/**
 * Request builder factory
 */
interface RequestBuilderFactory {
    fun createReadCharacteristicBuilder(): RequestBuilder<*>
    fun createWriteCharacteristicBuilder(): RequestBuilder<*>
    fun createNotificationBuilder(): RequestBuilder<*>
}

/**
 * Write characteristic builder
 */
interface WriteCharacteristicBuilder {
    fun setData(data: ByteArray): WriteCharacteristicBuilder
    fun setWriteOptions(options: WriteOptions): WriteCharacteristicBuilder
    fun build(): Request
}