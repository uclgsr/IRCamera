package com.mpdc4gsr.ble.core

/**
 * Request builder for BLE operations
 */
interface RequestBuilder {
    fun build(): Request
    fun setCallback(callback: Any): RequestBuilder
    fun setTimeout(timeoutMillis: Long): RequestBuilder
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