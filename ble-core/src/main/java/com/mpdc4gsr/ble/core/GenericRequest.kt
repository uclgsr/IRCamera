package com.mpdc4gsr.ble.core

import com.mpdc4gsr.ble.core.RequestCallback
import java.util.Queue
import java.util.UUID

internal class GenericRequest(builder: GenericRequestBuilder<*>) : Request, Comparable<GenericRequest?> {
    private val tag: String?
    var device: Device? = null
    var type: RequestType
    var service: UUID?
    var characteristic: UUID?
    var descriptor: UUID?
    var value: Any?
    var priority: Int
    var callback: RequestCallback?
    var writeOptions: WriteOptions?
    var descriptorTemp: ByteArray?

    var remainQueue: Queue<ByteArray?>? = null
    var sendingBytes: ByteArray?


    init {
        tag = builder.tag
        type = builder.type
        service = builder.service
        characteristic = builder.characteristic
        descriptor = builder.descriptor
        priority = builder.priority
        value = builder.value
        callback = builder.callback
        writeOptions = builder.writeOptions
    }

    override fun compareTo(other: GenericRequest): Int {
        return Integer.compare(other.priority, priority)
    }

    override fun getDevice(): Device {
        return device!!
    }

    override fun getType(): RequestType {
        return type
    }

    override fun getTag(): String? {
        return tag
    }

    override fun getService(): UUID? {
        return service
    }

    override fun getCharacteristic(): UUID? {
        return characteristic
    }

    override fun getDescriptor(): UUID? {
        return descriptor
    }

    override fun execute(connection: Connection?) {
        if (connection != null) {
            connection.execute(this)
        }
    }
}
