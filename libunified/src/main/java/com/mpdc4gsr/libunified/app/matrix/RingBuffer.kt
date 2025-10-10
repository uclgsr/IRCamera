package com.mpdc4gsr.libunified.app.matrix

class RingBuffer {
    private lateinit var byteArray: ByteArray
    private var mReadPositon = 0
    private var mUnReadLength = 0

    constructor(size: Int) {
        byteArray = ByteArray(size)
    }

    constructor(buffer: ByteArray) {
        byteArray = buffer
    }

    constructor(buffer: ByteArray, tail: Int, length: Int) {
        byteArray = buffer
        mReadPositon = tail
        mUnReadLength = length
    }

    fun write(
        buffer: ByteArray?,
        offset: Int,
        length: Int,
    ): Int {
        var head: Int
        var toEnd: Int
        var toWrite: Int
        synchronized(this) {
            head = (mReadPositon + mUnReadLength) % byteArray.size
            toEnd = byteArray.size - head
            // if the request exceeds the free space, write as much as possible
            toWrite = Math.min(length, byteArray.size - mUnReadLength)
        }
        if (toWrite > 0) {
            if (toWrite > toEnd) {
                // write from the head to the end
                System.arraycopy(buffer!!, offset, byteArray, head, toEnd)
                // write the remainder from the beginning
                System.arraycopy(buffer!!, offset + toEnd, byteArray, 0, toWrite - toEnd)
            } else {
                // write the whole thing at once
                System.arraycopy(buffer!!, offset, byteArray, head, toWrite)
            }
            // writing increases the length
            synchronized(this) { mUnReadLength += toWrite }
        }
        return toWrite
    }

    fun read(
        buffer: ByteArray?,
        offset: Int,
        length: Int,
    ): Int {
        if (buffer == null) return 0
        var toEnd: Int
        var toRead: Int
        synchronized(this) {
            toEnd = byteArray.size - mReadPositon
            // if the request exceeds the available data, read as much as is available
            toRead = Math.min(length, mUnReadLength)
        }
        if (toRead > toEnd) {
            // read from the tail to the end
            System.arraycopy(byteArray, mReadPositon, buffer, offset, toEnd)
            // read the requested remainder from the beginning
            System.arraycopy(byteArray, 0, buffer, offset + toEnd, toRead - toEnd)
        } else {
            // read the whole requested thing at once
            System.arraycopy(byteArray, mReadPositon, buffer, offset, toRead)
        }
        // reading moves the tail and decreases the length
        synchronized(this) {
            mReadPositon = (mReadPositon + toRead) % byteArray.size
            mUnReadLength -= toRead
        }
        return toRead
    }

    fun moveForward(length: Int): Int {
        synchronized(this) {
            mReadPositon = (mReadPositon + length) % byteArray.size
            mUnReadLength -= length
        }
        return length
    }

    fun moveBack(length: Int): Int {
        synchronized(this) {
            if (mReadPositon > length) {
                mReadPositon -= length
            } else {
                mReadPositon = mReadPositon - length + byteArray.size
            }
            mUnReadLength += length
        }
        return length
    }

    fun getUnReadLength(): Int = mUnReadLength

    fun getMaxLength(): Int = byteArray.size

    fun getFreeSpace(): Int = byteArray.size - mUnReadLength

    fun getByteArray(): ByteArray? = byteArray

    fun getReadPositon(): Int = mReadPositon

    override fun toString(): String =
        "RingBuffer(byteArray=${byteArray.contentToString()}, mReadPositon=$mReadPositon, mUnReadLength=$mUnReadLength)"
}
