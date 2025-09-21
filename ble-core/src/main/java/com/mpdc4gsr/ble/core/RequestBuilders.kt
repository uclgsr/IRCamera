package com.mpdc4gsr.ble.core

import java.util.UUID

/**
 * Request builder for BLE operations
 */
interface RequestBuilder {
    fun build(): Request
    fun setCallback(callback: Any): RequestBuilder
    fun setTimeout(timeoutMillis: Long): RequestBuilder
}

/**
 * Generic request builder implementation
 */
open class GenericRequestBuilder<T : GenericRequestBuilder<T>> : RequestBuilder {
    var tag: String? = null
    var type: RequestType = RequestType.READ_CHARACTERISTIC
    var service: UUID? = null
    var characteristic: UUID? = null
    var descriptor: UUID? = null
    var priority: Int = 0
    var value: Any? = null
    var callback: RequestCallback? = null
    var writeOptions: WriteOptions? = null

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

    override fun setCallback(callback: Any): RequestBuilder {
        if (callback is RequestCallback) {
            this.callback = callback
        }
        return this
    }

    override fun setTimeout(timeoutMillis: Long): RequestBuilder {
        return this
    }

    override fun build(): Request {
        return GenericRequest(this)
    }
}

/**
 * Request builder factory
 */
interface RequestBuilderFactory {
    fun createReadCharacteristicBuilder(): RequestBuilder
    fun createWriteCharacteristicBuilder(): RequestBuilder
    fun createNotificationBuilder(): RequestBuilder
}

/**
 * Write characteristic builder
 */
interface WriteCharacteristicBuilder {
    fun setData(data: ByteArray): WriteCharacteristicBuilder
    fun setWriteOptions(options: WriteOptions): WriteCharacteristicBuilder
    fun build(): Request
}