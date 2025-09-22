package com.mpdc4gsr.ble.core

import com.mpdc4gsr.ble.core.RequestCallback
import java.util.Queue
import java.util.UUID
internal class GenericRequest(builder: GenericRequestBuilder<*>) : Request, Comparable<GenericRequest> {
    override val tag: String? = builder.tag
    private var _device: Device? = null
    override val device: Device get() = _device!!
    override val type: RequestType = builder.type
    override val service: UUID? = builder.service
    override val characteristic: UUID? = builder.characteristic
    override val descriptor: UUID? = builder.descriptor
    var value: Any? = builder.value
    var priority: Int = builder.priority
    var callback: RequestCallback? = builder.callback
    var writeOptions: WriteOptions? = builder.writeOptions
    var descriptorTemp: ByteArray? = null

    var remainQueue: Queue<ByteArray?>? = null
    var sendingBytes: ByteArray? = null

    init {
        // Properties are initialized in declarations above
        // Additional initialization can be added here if needed
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
