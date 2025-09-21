package com.mpdc4gsr.ble.core

import com.mpdc4gsr.ble.core.callback.RequestCallback
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