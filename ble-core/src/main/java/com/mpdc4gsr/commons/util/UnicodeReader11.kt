package com.mpdc4gsr.commons.util

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.Reader

class UnicodeReader internal constructor(`in`: InputStream?, var defaultEncoding: String?) : Reader() {
    var internalIn: PushbackInputStream
    var internalIn2: InputStreamReader? = null

    init {
        internalIn = PushbackInputStream(`in`, BOM_SIZE)
    }

    val encoding: String?
        get() {
            if (internalIn2 == null) return null
            return internalIn2!!.getEncoding()
        }

    @Throws(IOException::class)
    protected fun init() {
        if (internalIn2 != null) return

        val encoding: String?
        val bom: ByteArray? = ByteArray(BOM_SIZE)
        val n: Int
        val unread: Int
        n = internalIn.read(bom, 0, bom!!.size)

        if ((bom[0] == 0x00.toByte()) && (bom[1] == 0x00.toByte())
            && (bom[2] == 0xFE.toByte()) && (bom[3] == 0xFF.toByte())
        ) {
            encoding = "UTF-32BE"
            unread = n - 4
        } else if ((bom[0] == 0xFF.toByte()) && (bom[1] == 0xFE.toByte())
            && (bom[2] == 0x00.toByte()) && (bom[3] == 0x00.toByte())
        ) {
            encoding = "UTF-32LE"
            unread = n - 4
        } else if ((bom[0] == 0xEF.toByte()) && (bom[1] == 0xBB.toByte())
            && (bom[2] == 0xBF.toByte())
        ) {
            encoding = "UTF-8"
            unread = n - 3
        } else if ((bom[0] == 0xFE.toByte()) && (bom[1] == 0xFF.toByte())) {
            encoding = "UTF-16BE"
            unread = n - 2
        } else if ((bom[0] == 0xFF.toByte()) && (bom[1] == 0xFE.toByte())) {
            encoding = "UTF-16LE"
            unread = n - 2
        } else {
            encoding = this.defaultEncoding
            unread = n
        }


        if (unread > 0) internalIn.unread(bom, (n - unread), unread)

        if (encoding == null) {
            internalIn2 = InputStreamReader(internalIn)
        } else {
            internalIn2 = InputStreamReader(internalIn, encoding)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        init()
        internalIn2!!.close()
    }

    @Throws(IOException::class)
    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        init()
        return internalIn2!!.read(cbuf, off, len)
    }

    companion object {
        private const val BOM_SIZE = 4
    }
}
