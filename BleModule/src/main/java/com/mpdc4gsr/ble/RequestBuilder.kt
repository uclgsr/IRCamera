package com.mpdc4gsr.ble

import com.mpdc4gsr.ble.callback.RequestCallback
import java.util.UUID

open class RequestBuilder<T : RequestCallback?> internal constructor(var type: RequestType?) {
    var tag: String? = null
    var service: UUID? = null
    var characteristic: UUID? = null
    var descriptor: UUID? = null
    var value: Any? = null
    var priority: Int = 0
    var callback: RequestCallback? = null
    var writeOptions: WriteOptions? = null

    open fun setTag(tag: String?): RequestBuilder<T?>? {
        this.tag = tag
        return this
    }

    open fun setPriority(priority: Int): RequestBuilder<T?>? {
        this.priority = priority
        return this
    }

    open fun setCallback(callback: T?): RequestBuilder<T?>? {
        this.callback = callback
        return this
    }

    fun build(): Request {
        return GenericRequest(this)
    }
}
