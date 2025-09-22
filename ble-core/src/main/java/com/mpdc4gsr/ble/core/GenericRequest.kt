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
    var descriptorTemp: ByteArray? = null

    var remainQueue: Queue<ByteArray?>? = null
    var sendingBytes: ByteArray? = null


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
    
    fun setDevice(device: Device) {
        _device = device
    }

    override fun execute(connection: Connection?) {
        if (connection != null) {
            connection.execute(this)
        }
    }
}
