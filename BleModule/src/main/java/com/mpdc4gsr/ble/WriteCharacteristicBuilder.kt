package com.mpdc4gsr.ble

import com.mpdc4gsr.ble.callback.WriteCharacteristicCallback

class WriteCharacteristicBuilder internal constructor() :
    RequestBuilder<WriteCharacteristicCallback?>(RequestType.WRITE_CHARACTERISTIC) {
    override fun setTag(tag: String?): WriteCharacteristicBuilder {
        super.setTag(tag)
        return this
    }

    override fun setPriority(priority: Int): WriteCharacteristicBuilder {
        super.setPriority(priority)
        return this
    }

    override fun setCallback(callback: WriteCharacteristicCallback?): WriteCharacteristicBuilder {
        super.setCallback(callback)
        return this
    }

    fun setWriteOptions(writeOptions: WriteOptions?): WriteCharacteristicBuilder {
        this.writeOptions = writeOptions
        return this
    }
}
