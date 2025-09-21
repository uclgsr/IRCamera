package com.mpdc4gsr.ble.core

import com.mpdc4gsr.ble.core.callback.RequestCallback
import java.util.Queue
import java.util.UUID

internal class GenericRequest(builder: RequestBuilder<*>) : Request, Comparable<GenericRequest> {
    override val tag: String?
    override val device: Device
        get() = _device ?: throw IllegalStateException("Device not set")
    private var _device: Device? = null
    override var type: RequestType
    override var service: UUID?
    override var characteristic: UUID?
    override var descriptor: UUID?
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
