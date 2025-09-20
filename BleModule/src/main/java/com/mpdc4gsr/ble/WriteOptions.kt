package com.mpdc4gsr.ble

import android.bluetooth.BluetoothGattCharacteristic

class WriteOptions private constructor(builder: Builder) {
    val packageWriteDelayMillis: Int
    val requestWriteDelayMillis: Int
    val isWaitWriteResult: Boolean
    val writeType: Int
    val useMtuAsPackageSize: Boolean
    var packageSize: Int

    init {
        packageWriteDelayMillis = builder.packageWriteDelayMillis
        requestWriteDelayMillis = builder.requestWriteDelayMillis
        packageSize = builder.packageSize
        isWaitWriteResult = builder.isWaitWriteResult
        writeType = builder.writeType
        useMtuAsPackageSize = builder.useMtuAsPackageSize
    }

    class Builder {
        private var packageWriteDelayMillis = 0
        private var requestWriteDelayMillis = -1
        private var packageSize = 20
        private var isWaitWriteResult = true
        private var writeType = -1
        private var useMtuAsPackageSize = false

        fun setPackageWriteDelayMillis(packageWriteDelayMillis: Int): Builder {
            this.packageWriteDelayMillis = packageWriteDelayMillis
            return this
        }

        fun setRequestWriteDelayMillis(requestWriteDelayMillis: Int): Builder {
            this.requestWriteDelayMillis = requestWriteDelayMillis
            return this
        }

        fun setPackageSize(packageSize: Int): Builder {
            if (packageSize > 0) {
                this.packageSize = packageSize
            }
            return this
        }

        fun setWaitWriteResult(waitWriteResult: Boolean): Builder {
            isWaitWriteResult = waitWriteResult
            return this
        }

        fun setWriteType(writeType: Int): Builder {
            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED) {
                this.writeType = writeType
            }
            return this
        }

        fun setMtuAsPackageSize(): Builder {
            useMtuAsPackageSize = true
            return this
        }

        fun build(): WriteOptions {
            return WriteOptions(this)
        }
    }
}
