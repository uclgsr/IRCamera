// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix' subtree
// Files: 10; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\FirmwareUpgradeResultCode.kt =====

package com.mpdc4gsr.libunified.app.matrix

enum class FirmwareUpgradeResultCode {
    SUCCESS("Success", 0),
    FILE_ERROR("File path is null", 1),
    FILE_NOT_EXISTS("File does not exists", 3),
    USB_DEVICE_ERROR("USB device is invalid", 4),
    FILE_READ_ERROR("Read upgrade file error", 5),
    PAGE_ERROR("Upgrade page error", 6),
    RENDER_DATA_ERROR("Render data is not available", 7),
    INVALID_FILE_ERROR("Upgrade file is invalid", 8),
    FILE_WRITE_ERROR("Write upgrade file error", 9);

    private var msg: String? = null
    private var code = 0

    constructor (msg: String, code: Int) {
        this.msg = msg
        this.code = code
    }

    open fun getMsg(): String? {
        return msg
    }

    open fun setMsg(msg: String?) {
        this.msg = msg
    }

    open fun getCode(): Int {
        return code
    }

    open fun setCode(code: Int) {
        this.code = code
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\GuideInterface.kt =====

package com.mpdc4gsr.libunified.app.matrix

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.mpdc4gsr.libunified.app.matrix.utils.HexDump
import com.mpdc4gsr.libunified.app.utils.FileUtils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

class GuideInterface {
    private val TAG = "guidecore"
    private val IR_WIDTH = 256
    private val IR_HEIGHT = 192
    private val HEAD_SIZE = 64
    private val IR_SIZE = IR_WIDTH * IR_HEIGHT //49152
    private val YUV_SIZE = IR_SIZE * 2
    private val PARAM_SIZE = 512
    private val TEMP_MATRIX_SIZE = IR_SIZE * 4
    private val FRAME_SIZE = HEAD_SIZE + YUV_SIZE + PARAM_SIZE + TEMP_MATRIX_SIZE //295488
    private val MAX_BULK_TRANSFER_SIZE = 16384
    private var mGuideUsbManager: GuideUsbManager? = null
    private var mUsbBuffer: UsbBuffer? = null
    private var mNativeGuideCore: NativeGuideCore? = null
    private val mUsbReadbuffer = ByteArray(MAX_BULK_TRANSFER_SIZE)
    private val mFrame = ByteArray(FRAME_SIZE)
    private val mYuv = ByteArray(YUV_SIZE)
    private val mParam = ByteArray(PARAM_SIZE)
    private val mTempMatrixByte = ByteArray(TEMP_MATRIX_SIZE)
    private val mTempMatrixFloat = FloatArray(IR_SIZE)
    private var mIrDataCallback: IrDataCallback? = null
    private var mUsbBufferWriteThread: Thread? = null
    private var mUsbBufferReadThread: Thread? = null

    @Volatile
    private var mWriteThreadFlag = false

    @Volatile
    private var mReadThreadFlag = false
    private val mLock = Any()

    interface IrDataCallback {
        fun processIrData(yuv: ByteArray, temp: FloatArray)
    }

    private fun startUsbBufferWriteThread() {
        mWriteThreadFlag = true
        mUsbBufferWriteThread = Thread {
            Logger.d(TAG, "write thread start")
            while (mWriteThreadFlag) {
                val length: Int = mGuideUsbManager!!.read(mUsbReadbuffer)
                if (length > 0) {
                    mUsbBuffer!!.write(mUsbReadbuffer, 0, length)
                } else {
//                        Logger.d(TAG, "length < 0");
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
            Logger.d(TAG, "write thread exit")
        }
        mUsbBufferWriteThread!!.start()
    }

    var startTime = 0L
    private fun startUsbBufferReadThread() {
        mReadThreadFlag = true
        mUsbBufferReadThread = Thread {
            Logger.d(TAG, "read thread start")
            while (mReadThreadFlag) {
                val ret = mUsbBuffer!!.readFrame(mFrame) //mFrame len: 295488
                if (ret) {
                    System.arraycopy(mFrame, HEAD_SIZE, mYuv, 0, mYuv.size)
                    synchronized(mLock) {
                        System.arraycopy(
                            mFrame,
                            HEAD_SIZE + YUV_SIZE,
                            mParam,
                            0,
                            mParam.size
                        )
                        System.arraycopy(
                            mFrame,
                            HEAD_SIZE + YUV_SIZE + PARAM_SIZE,
                            mTempMatrixByte,
                            0,
                            mTempMatrixByte.size
                        )
                    }
                    mNativeGuideCore!!.toFloatTempMatrix(mTempMatrixFloat, mTempMatrixByte)
                    if (mIrDataCallback != null) {
                        mIrDataCallback!!.processIrData(mYuv, mTempMatrixFloat)
                    }
                } else {
                }
            }
            Logger.d(TAG, "read thread exit")
        }
        mUsbBufferReadThread!!.start()
    }

    private fun stopUsbBufferWriteThread() {
        if (mUsbBufferWriteThread != null) {
            mWriteThreadFlag = false
            try {
                mUsbBufferWriteThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mUsbBufferWriteThread = null
        }
    }

    private fun stopUsbBufferReadThread() {
        if (mUsbBufferReadThread != null) {
            mReadThreadFlag = false
            try {
                mUsbBufferReadThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mUsbBufferReadThread = null
        }
    }

    private fun getParam(offset: Int, len: Int, index: Int): Byte {
        val param = ByteArray(len)
        synchronized(mLock) { System.arraycopy(mParam, offset, param, 0, len) }
        return param[index]
    }

    private fun getParam(offset: Int, len: Int): ByteArray {
        val param = ByteArray(len)
        synchronized(mLock) { System.arraycopy(mParam, offset, param, 0, len) }
        return param
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun init(context: Context?, irDataCallback: IrDataCallback?): Int {
        mNativeGuideCore = NativeGuideCore()
        mGuideUsbManager = GuideUsbManager(context, mNativeGuideCore)
        mIrDataCallback = irDataCallback
        val ret: Int = mGuideUsbManager!!.connectUsbDevice()
        if (ret != 5) {
            return ret
        }
        Logger.d(TAG, "connectUsbDevice ret = $ret")
        mUsbBuffer = UsbBuffer(FRAME_SIZE, HEAD_SIZE, 8)
        mUsbBuffer!!.setFrameMark(0xBB66)
        startUsbBufferReadThread()
        startUsbBufferWriteThread()
        return ret
    }

    fun exit() {
        stopUsbBufferWriteThread()
        stopUsbBufferReadThread()
        if (mGuideUsbManager != null) {
            mGuideUsbManager!!.disconnectUsbDevice()
            mGuideUsbManager = null
        }
        if (mNativeGuideCore != null) {
            mNativeGuideCore = null
        }
    }

    fun shutter() {
        if (mGuideUsbManager == null) {
            return
        }
        mGuideUsbManager!!.shutter()
    }

    fun nuc() {
        if (mGuideUsbManager == null) {
            return
        }
        mGuideUsbManager!!.nuc()
    }

    fun changePalette(i: Int) {
        Log.d(TAG, "changePalette() called with: i = [$i]")
        if (mGuideUsbManager == null) {
            return
        }
        if (i < 0 || i > 9) {
            return
        }
        mGuideUsbManager!!.changePalette(i)
    }

    fun setDistance(distance: Float) {
        if (mGuideUsbManager == null) {
            return
        }
        mGuideUsbManager!!.setDistance(distance)
    }

    fun getDistance(): Float {
        if (mNativeGuideCore == null) {
            return (-1).toFloat()
        }
        val PARAM_INDEX_DISTANCE = 163
        return getParam(PARAM_INDEX_DISTANCE * 2, 1, 0) * 1.0f / 10
    }

    fun setBright(bright: Int) {
        if (mGuideUsbManager == null) {
            return
        }
        if (bright < 0 || bright > 100) {
            return
        }
        mGuideUsbManager!!.setBright(bright)
    }

    fun getBright(): Int {
        if (mNativeGuideCore == null) {
            return -1
        }
        val PARAM_INDEX_BRIGHT = 164
        return getParam(PARAM_INDEX_BRIGHT * 2, 1, 0).toInt()
    }

    fun setContrast(contrast: Int) {
        if (mGuideUsbManager == null) {
            return
        }
        if (contrast < 0 || contrast > 100) {
            return
        }
        mGuideUsbManager!!.setContrast(contrast)
    }

    fun getContrast(): Int {
        if (mNativeGuideCore == null) {
            return -1
        }
        val PARAM_INDEX_CONTRAST = 164
        return getParam(PARAM_INDEX_CONTRAST * 2, 2, 1).toInt()
    }

    fun yuv2Bitmap(bitmap: Bitmap?, yuv: ByteArray?) {
        if (mNativeGuideCore == null) {
            return
        }
        mNativeGuideCore!!.yuv2Bitmap(bitmap!!, yuv!!)
    }

    fun saveTempMatrix(path: String?) {
        synchronized(mLock) {
            FileUtils.saveFile(path ?: "", mTempMatrixByte ?: ByteArray(0))
        }
    }

    fun setRange(range: Int) {
        if (mGuideUsbManager == null) {
            return
        }
        mGuideUsbManager!!.setRange(range)
    }

    fun setEmiss(emiss: Int) {
        if (mGuideUsbManager == null) {
            return
        }
        if (emiss < 1 || emiss > 99) {
            return
        }
        mGuideUsbManager!!.setEmiss(emiss)
    }

    fun getEmiss(): Int {
        if (mNativeGuideCore == null) {
            return -1
        }
        val PARAM_INDEX_EMISS = 162
        return getParam(PARAM_INDEX_EMISS * 2, 1, 0).toInt()
    }

    fun getFirmwareVersion(): String? {
        val PARAM_INDEX_ASIC_MAIN_VERSION = 32
        val DOT = "."
        val bytes = getParam(PARAM_INDEX_ASIC_MAIN_VERSION * 2, 6)
        val mainVersion =
            (bytes[1] and 0xFF.toByte()).toInt().shl(8) or ((bytes[0] and 0xFF.toByte()).toInt())
        val mainVersion1: Int = (mainVersion and 0xFFFF).shr(12)
        val mainVersion2: Int = (mainVersion and 0x0FC0).shr(6)
        val mainVersion3: Int = mainVersion and 0x003F
        val asicVersion = StringBuilder()
        1.shr(2)
        asicVersion.append(mainVersion1)
            .append(DOT)
            .append(mainVersion2)
            .append(DOT)
            .append(mainVersion3)
            .append(DOT)
            .append(HexDump.toHexString(bytes[3]))
            .append(HexDump.toHexString(bytes[2]))
            .append(HexDump.toHexString(bytes[5]))
            .append(HexDump.toHexString(bytes[4]))
        return asicVersion.toString()
    }

    fun getSN(): String {
        val PARAM_INDEX_SN = 39
        val bytes = getParam(PARAM_INDEX_SN * 2, 15)
        return String(bytes, StandardCharsets.US_ASCII)
    }

    fun getId(): String {
        val PARAM_INDEX_ID = 192
        val bytes = getParam(PARAM_INDEX_ID * 2, 17)
        return String(bytes, StandardCharsets.US_ASCII)
    }

    fun getShutterStatus(): Int {
        val PARAM_INDEX_SHUTTER_STATUS = 12
        return getParam(PARAM_INDEX_SHUTTER_STATUS * 2, 1, 0).toInt()
    }

    fun getImageStatus(): Int {
        val PARAM_INDEX_IMAGE_STATUS = 13
        return getParam(PARAM_INDEX_IMAGE_STATUS * 2, 1, 0).toInt()
    }

    fun upgrade(path: String?): FirmwareUpgradeResultCode? {
        if (mGuideUsbManager == null) {
            return FirmwareUpgradeResultCode.USB_DEVICE_ERROR
        }
        if (path.isNullOrEmpty()) {
            return FirmwareUpgradeResultCode.FILE_ERROR
        }
        val file = File(path)
        if (!file.exists()) {
            return FirmwareUpgradeResultCode.FILE_NOT_EXISTS
        }
        if (!mGuideUsbManager!!.isUsbValid()) {
            return FirmwareUpgradeResultCode.USB_DEVICE_ERROR
        }
        val bos = ByteArrayOutputStream(file.length().toInt())
        var `in`: BufferedInputStream? = null
        try {
            `in` = BufferedInputStream(FileInputStream(file))
            val bufSize = 1024
            val buffer = ByteArray(bufSize)
            var len = 0
            while (-1 != `in`.read(buffer, 0, bufSize).also { len = it }) {
                bos.write(buffer, 0, len)
            }
        } catch (e: Exception) {
            try {
                `in`!!.close()
                bos.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return FirmwareUpgradeResultCode.FILE_READ_ERROR
        }
        val allData = bos.toByteArray()
        return if (mGuideUsbManager!!.upgrade(allData)) {
            FirmwareUpgradeResultCode.SUCCESS
        } else {
            FirmwareUpgradeResultCode.FILE_WRITE_ERROR
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\GuideUsbManager.kt =====

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package com.mpdc4gsr.libunified.app.matrix

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.mpdc4gsr.libunified.app.matrix.ResultCode.ERROR_CONNECT_DEVICE_FAILD
import com.mpdc4gsr.libunified.app.matrix.ResultCode.SUCC_CONNECT_INTERFACE
import com.mpdc4gsr.libunified.app.matrix.utils.HexDump
import java.util.*

class GuideUsbManager {
    private var mContext: Context? = null
    private val mPermissionIntent: PendingIntent? = null
    private var mUsbManager: UsbManager? = null
    private var mUsbDevice: UsbDevice? = null
    private var mConnection: UsbDeviceConnection? = null
    private var mUsbInterface: UsbInterface? = null
    private var mEndpointDataIn: UsbEndpoint? = null
    private var mEndpointControlOut: UsbEndpoint? = null
    private var mEndpointControlIn: UsbEndpoint? = null

    companion object {
        val ADDRESS_ENDPOINT_DATA_IN = 129
        val ADDRESS_ENDPOINT_CONTROL_OUT = 2
        val ADDRESS_ENDPOINT_CONTROL_IN = 131
        val VENDOR_ID = 0x4206
        val PRODUCT_ID = 0x3702
    }

    private var mConnectCode: Int = ResultCode.READY_CONNECT_DEVICE
    private val TAG = "guidecore"
    private var mNativeGuideCore: NativeGuideCore? = null

    constructor(context: Context?, nativeGuideCore: NativeGuideCore?) {
        mContext = context
        mNativeGuideCore = nativeGuideCore
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun connectUsbDevice(): Int {
        if (mConnectCode == ResultCode.READY_CONNECT_DEVICE) {
            getUsbDevice()
            findInterface()
            val ret = openDevice()
            if (ret != ResultCode.SUCC_CONNECT_INTERFACE) {
                return ret
            }
            assignEndpoint()
        }
        if (mConnectCode != ResultCode.SUCC_FIND_ENDPOINT) {
            resetUsbDevice()
        }
        return mConnectCode
    }

    fun disconnectUsbDevice() {
        resetUsbDevice()
        mConnectCode = ResultCode.READY_CONNECT_DEVICE
    }

    fun isUsbValid(): Boolean {
        return true
    }

    private fun resetUsbDevice() {
        if (mConnection != null) {
            mConnection!!.releaseInterface(mUsbInterface)
            mConnection!!.close()
        }
        mUsbManager = null
        mUsbDevice = null
        mConnection = null
        mUsbInterface = null
        mEndpointDataIn = null
        mEndpointControlOut = null
        mEndpointControlIn = null
    }

    private fun getUsbDevice() {
        mUsbManager = mContext!!.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = mUsbManager!!.deviceList
        if (!deviceList.isEmpty()) {
            for (device in deviceList.values) {
                if (device.vendorId == VENDOR_ID && device.productId == PRODUCT_ID) {
                    mUsbDevice = device
                    mConnectCode = ResultCode.SUCC_FIND_MATCHED_DEVICE
                    break
                }
            }
            if (mUsbDevice == null) {
                mConnectCode = ResultCode.ERROR_FIND_DEVICE_NOT_MATCH
            }
        } else {
            mConnectCode = ResultCode.ERROR_NOT_FIND_DEVICE
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun findInterface() {
        if (mUsbDevice != null) {
            val count = mUsbDevice!!.interfaceCount
            if (count == 1) {
                mUsbInterface = mUsbDevice!!.getInterface(0)
            } else {
                for (i in 0 until count) {
                    val usbInterface = mUsbDevice!!.getInterface(i)
                    if (usbInterface.endpointCount == 3 && usbInterface.alternateSetting == 0) {
                        mUsbInterface = usbInterface
                        mConnectCode = ResultCode.SUCC_FIND_DEVICE_INTERFACE
                        break
                    }
                }
                if (mUsbInterface == null) {
                    mConnectCode = ResultCode.ERROR_NOT_FIND_INTERFACE
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun openDevice(): Int {
        if (mUsbInterface != null) {
            mConnection = mUsbManager!!.openDevice(mUsbDevice)
            return if (mConnection != null) {
                Logger.d(TAG, "setInterface")
                mConnection!!.setInterface(mUsbInterface)
                if (mConnection!!.claimInterface(mUsbInterface, true)) {
                    Logger.d(TAG, "claimInterface true")
                    SUCC_CONNECT_INTERFACE.also { mConnectCode = it }
                } else {
                    Logger.d(TAG, "claimInterface false")
                    mConnection!!.close()
                    ERROR_CONNECT_DEVICE_FAILD.also { mConnectCode = it }
                }
            } else {
                ResultCode.ERROR_OPEN_DEVICE_FAILD.also { mConnectCode = it }
            }
        }
        return mConnectCode
    }

    private fun assignEndpoint() {
        if (mUsbInterface != null) {
            val endpointCount = mUsbInterface!!.endpointCount
            var usbEndpoint: UsbEndpoint
            for (i in 0 until endpointCount) {
                usbEndpoint = mUsbInterface!!.getEndpoint(i)
                val address = usbEndpoint.address
                when (address) {
                    ADDRESS_ENDPOINT_DATA_IN -> mEndpointDataIn = usbEndpoint
                    ADDRESS_ENDPOINT_CONTROL_OUT -> mEndpointControlOut = usbEndpoint
                    ADDRESS_ENDPOINT_CONTROL_IN -> mEndpointControlIn = usbEndpoint
                    else -> {
                    }
                }
            }
            mConnectCode = if (true) {
                ResultCode.SUCC_FIND_ENDPOINT
            } else {
                ResultCode.ERROR_FIND_ENDPOINT_FAILD
            }
        }
    }

    fun read(buffer: ByteArray): Int {
        return if (!isUsbValid()) {
            ResultCode.ERROR_USE_USB_ISVALID
        } else mConnection!!.bulkTransfer(mEndpointDataIn, buffer, buffer.size, 1000)
    }

    fun changePalette(i: Int) {
        val cmd = byteArrayOf(0x11, 0x00)
        sendUsbCmd(cmd, toByteArray(i))
    }

    fun shutter() {
        val cmd = byteArrayOf(0x15, 0x00)
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        sendUsbCmd(cmd, data)
    }

    fun nuc() {
        val cmd = byteArrayOf(0x16, 0x00)
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        sendUsbCmd(cmd, data)
    }

    fun upgrade(data: ByteArray): Boolean {
        val PAGE_SIZE = 3000
        val header = byteArrayOf(0x02)
        val cmd = byteArrayOf(0x07, 0x00)
        val reserve = byteArrayOf(0x00)
        val len = toByteArray(data.size)
        val check = toByteArray(mNativeGuideCore!!.crc(data))
        val upgradeHead = ByteArray(header.size + cmd.size + reserve.size + len.size + check.size)
        var destPos = 0
        System.arraycopy(header, 0, upgradeHead, destPos, header.size)
        destPos += header.size
        System.arraycopy(cmd, 0, upgradeHead, destPos, cmd.size)
        destPos += cmd.size
        System.arraycopy(reserve, 0, upgradeHead, destPos, reserve.size)
        destPos += reserve.size
        System.arraycopy(len, 0, upgradeHead, destPos, len.size)
        destPos += len.size
        System.arraycopy(check, 0, upgradeHead, destPos, check.size)
        if (!send(upgradeHead)) {
            return false
        }
        if (data.size <= PAGE_SIZE) {
            if (!send(data)) {
                return false
            }
        } else {
            var total = 0
            var sendBuf = ByteArray(PAGE_SIZE)
            while (total < data.size) {
                val sendLen = Math.min(PAGE_SIZE, data.size - total)
                if (sendLen != PAGE_SIZE) {
                    sendBuf = ByteArray(sendLen)
                }
                System.arraycopy(data, total, sendBuf, 0, sendLen)
                total += if (!send(sendBuf)) {
                    Logger.d(TAG, "upgrade senBuf failed")
                    return false
                } else {
                    sendLen
                }
            }
        }
        val tail = byteArrayOf(0x03)
        if (!send(tail)) {
            return false
        }
        val upgradeResultCmd = byteArrayOf(0x08, 0x00)
        return receive(upgradeResultCmd)
    }

    fun setRange(range: Int) {
        val cmd = byteArrayOf(0x20, 0x01)
        sendUsbCmd(cmd, toByteArray(range))
    }

    fun setEmiss(emiss: Int) {
        val cmd = byteArrayOf(0x21, 0x01)
        sendUsbCmd(cmd, toByteArray(emiss))
    }

    fun setDistance(value: Float) {
        val cmd = byteArrayOf(0x23, 0x01)
        val distance = (value * 10).toInt()
        sendUsbCmd(cmd, toByteArray(distance))
    }

    fun setBright(bright: Int) {
        val cmd = byteArrayOf(0x00, 0x02)
        sendUsbCmd(cmd, toByteArray(bright))
    }

    fun setContrast(contrast: Int) {
        val cmd = byteArrayOf(0x01, 0x02)
        sendUsbCmd(cmd, toByteArray(contrast))
    }

    private fun toByteArray(i: Int): ByteArray {
        val data = ByteArray(4)
        data[0] = (i and 0xFF).toByte()
        data[1] = (i shr 8 and 0xFF).toByte()
        data[2] = (i shr 16 and 0xFF).toByte()
        data[3] = (i shr 24 and 0xFF).toByte()
        return data
    }

    private fun sendUsbCmd(cmd: ByteArray, data: ByteArray): Int {
        val header = byteArrayOf(0x02)
        val reserve = byteArrayOf(0x00)
        val len = toByteArray(data.size)
        val check = toByteArray(mNativeGuideCore!!.crc(data))
        Log.w("123", "check: ${check.toHexString()}")
        val tail = byteArrayOf(0x03)
        val buffer =
            ByteArray(header.size + cmd.size + reserve.size + len.size + check.size + data.size + tail.size)
        var destPos = 0
        System.arraycopy(header, 0, buffer, destPos, header.size)
        destPos += header.size
        System.arraycopy(cmd, 0, buffer, destPos, cmd.size)
        destPos += cmd.size
        System.arraycopy(reserve, 0, buffer, destPos, reserve.size)
        destPos += reserve.size
        System.arraycopy(len, 0, buffer, destPos, len.size)
        destPos += len.size
        System.arraycopy(check, 0, buffer, destPos, check.size)
        destPos += check.size
        System.arraycopy(data, 0, buffer, destPos, data.size)
        destPos += data.size
        System.arraycopy(tail, 0, buffer, destPos, tail.size)
        val length = mConnection!!.bulkTransfer(mEndpointControlOut, buffer, buffer.size, 1000)
        Log.w("123", "sendUsbCmd: ${buffer.toHexString()}")
        Logger.d(TAG, "sendUsbCmd >> ${HexDump.dumpHexString(buffer)}".trimIndent())
        Logger.d(TAG, "<< end (length = $length)")
        return length
    }

    private fun send(buffer: ByteArray): Boolean {
        val length = mConnection!!.bulkTransfer(mEndpointControlOut, buffer, buffer.size, 1000)
        Logger.d(
            TAG,
            "send " + (length == buffer.size) + ": request len = " + buffer.size + " response len = " + length
        )
        return length == buffer.size
    }

    private fun receive(cmd: ByteArray): Boolean {
        val SUCCESS = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val buffer = ByteArray(17)
        var length = -1
        while (length < 0) {
            length = mConnection!!.bulkTransfer(mEndpointControlIn, buffer, buffer.size, 1000)
        }
        Logger.d(
            TAG, """receive length = $length
 data = ${HexDump.dumpHexString(buffer)}"""
        )
        val headReceive = ByteArray(1)
        val cmdReceive = ByteArray(2)
        val reserveReceive = ByteArray(1)
        val lenReceive = ByteArray(4)
        val checkReceive = ByteArray(4)
        val dataReceive = ByteArray(4)
        val tailReceive = ByteArray(1)
        var destPos = 0
        System.arraycopy(buffer, destPos, headReceive, 0, headReceive.size)
        Logger.d(TAG, "receive headReceive = " + HexDump.dumpHexString(headReceive))
        destPos += headReceive.size
        System.arraycopy(buffer, destPos, cmdReceive, 0, cmdReceive.size)
        Logger.d(TAG, "receive cmdReceive = " + HexDump.dumpHexString(cmdReceive))
        destPos += cmdReceive.size
        System.arraycopy(buffer, destPos, reserveReceive, 0, reserveReceive.size)
        Logger.d(TAG, "receive reserveReceive = " + HexDump.dumpHexString(reserveReceive))
        destPos += reserveReceive.size
        System.arraycopy(buffer, destPos, lenReceive, 0, lenReceive.size)
        Logger.d(TAG, "receive lenReceive = " + HexDump.dumpHexString(lenReceive))
        destPos += lenReceive.size
        System.arraycopy(buffer, destPos, checkReceive, 0, checkReceive.size)
        Logger.d(TAG, "receive checkReceive = " + HexDump.dumpHexString(checkReceive))
        destPos += checkReceive.size
        System.arraycopy(buffer, destPos, dataReceive, 0, dataReceive.size)
        Logger.d(TAG, "receive dataReceive = " + HexDump.dumpHexString(dataReceive))
        destPos += dataReceive.size
        System.arraycopy(buffer, destPos, tailReceive, 0, tailReceive.size)
        Logger.d(TAG, "receive tailReceive = " + HexDump.dumpHexString(tailReceive))
        return Arrays.equals(cmd, cmdReceive) && Arrays.equals(SUCCESS, dataReceive)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\IrSurfaceView.kt =====

package com.mpdc4gsr.libunified.app.matrix

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class IrSurfaceView : SurfaceView, SurfaceHolder.Callback {
    private var mHolder: SurfaceHolder? = null
    private var mCanvas: Canvas? = null
    private val p: Paint by lazy { Paint() }
    private val mMatrix: Matrix by lazy { Matrix() }
    private var openLut = false
    private val mBeforeRotateMatrixValues = FloatArray(9)
    private val mScaleMatrixValues = FloatArray(9)
    private val mRotateMatrixValues = FloatArray(9)

    @Volatile
    private var isPrepare = false

    @Volatile
    private var isLockImage = false
    private var callback: IfrCamOpenOverCallback? = null
    private var mCtx: Context? = null;

    constructor(context: Context) : super(context) {
        mCtx = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mCtx = context
        init()
    }

    private fun init() {
        mHolder = holder
        mHolder?.addCallback(this)
        mHolder?.setFormat(PixelFormat.TRANSPARENT)
        p.alpha = 0xff
        mMatrix.setScale(1.0f, 1.0f)
    }

    fun setIsLockImage(isLock: Boolean) {
        isLockImage = isLock
    }

    //    fun setMatrix(scale: Float, x: Float, y: Float) {
//        mMatrix.reset()
//        mMatrix.setScale(scale, scale)
//        mMatrix.postTranslate(x, y)
//        mMatrix.getValues(mBeforeRotateMatrixValues)
//    }
    fun setMatrix(rotate: Float, w: Float, h: Float) {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        mMatrix.reset()
        when (rotate) {
            90f -> {
                val sca = screenWidth / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(h, 0f)
                mMatrix.postScale(sca, sca)
            }

            180f -> {
                val sca = screenWidth / w
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(w, h)
                mMatrix.postScale(sca, sca)
            }

            270f -> {
                val sca = screenWidth / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(0f, w)
                mMatrix.postScale(sca, sca)
            }

            else -> {
                val sca = screenWidth / w
                mMatrix.postScale(sca, sca)
            }
        }
    }

    fun doDraw(bitmap: Bitmap?, shutterFlag: Int) {
        synchronized(this) {
            if (isLockImage || !isPrepare || null == bitmap || shutterFlag == 1) {
                return@doDraw
            }
            mCanvas = mHolder?.lockCanvas()
            try {
                mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//                mCanvas?.drawBitmap(bitmap, mMatrix, p)
                if (openLut) {
                    mColorMatrixEnhance.setSaturation(saturation * 0.01f * 2.5f + 1f)
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrixEnhance)
                } else {
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrix)
                }
                mCanvas?.drawBitmap(bitmap, mMatrix, p)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val surface = mHolder!!.surface
                if (mCanvas != null && mHolder != null && surface != null && surface.isValid) {
                    try {
                        mHolder?.unlockCanvasAndPost(mCanvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private var mColorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 01f, 0f
        )
    )
    private var mColorMatrixLut = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1.5f, 0f, 0f, 25f,
            0.1f, 0.2f, 0.7f, 0f, 25f,
            0f, 0f, 0f, 01f, 0f
        )
    )
    private val n = 1f
    private var mColorMatrixEnhance = ColorMatrix(
//        floatArrayOf(
//            n, 0f, 0f, 0f, 128 * (1 - n),
//            0f, n, 0f, 0f, 128 * (1 - n),
//            0f, 0f, n, 0f, 128 * (1 - n),
//            0f, 0f, 0f, 1f, 0f
//        )
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    private var saturation = 0
    fun setOpenLut() {
//        openLut = !openLut
        openLut = true
    }

    fun setSaturationValue(saturation: Int) {
        this.saturation = saturation
    }

    fun getSaturationValue(): Int {
        return saturation
    }

    fun setAlpha(alpha: Int) {
        if (alpha in 0..255) {
            p?.alpha = alpha
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isPrepare = true
        if (callback != null)
            callback!!.onSurfaceCreated()
        Logger.d(TAG, "holder onSurfaceCreated")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Logger.d(TAG, "holder surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(this) {
            isPrepare = false
            Logger.d(TAG, "holder destroyed")
        }
    }

    companion object {
        private val TAG = "IrSurfaceView"
    }

    interface IfrCamOpenOverCallback {
        fun onSurfaceCreated()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\Logger.kt =====

package com.mpdc4gsr.libunified.app.matrix

import android.util.Log
import com.mpdc4gsr.libunified.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    @JvmStatic
    fun e(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg + "")
        }
    }

    @JvmStatic
    fun w(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun w(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg + "")
        }
    }

    @JvmStatic
    fun i(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun i(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg + "")
        }
    }

    @JvmStatic
    fun d(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(clazz.simpleName, msg + "")
        }
    }

    @JvmStatic
    fun d(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg + "")
        }
    }

    @JvmStatic
    fun v(clazz: Class<*>, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(clazz.simpleName, msg + "")
        }
    }

    fun v(tag: String?, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg + "")
        }
    }

    private val MYLOG_PATH_SDCARD_DIR = "/sdcard/Guide/log"
    private val MYLOGFILEName = "Log.txt"
    private val myLogSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val logfile = SimpleDateFormat("yyyy-MM-dd")
    fun f(tag: String, text: String) {
        val nowtime = Date()
        val needWriteFiel = logfile.format(nowtime)
        val needWriteMessage = myLogSdf.format(nowtime) + "    " + "    " + tag + "    " + text
        val dirsFile = File(MYLOG_PATH_SDCARD_DIR)
        if (!dirsFile.exists()) {
            dirsFile.mkdirs()
        }
        val file = File(dirsFile.toString(), needWriteFiel + MYLOGFILEName) // MYLOG_PATH_SDCARD_DIR
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
            }
        }
        try {
            val filerWriter = FileWriter(file, true)
            val bufWriter = BufferedWriter(filerWriter)
            bufWriter.write(needWriteMessage)
            bufWriter.newLine()
            bufWriter.close()
            filerWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\NativeGuideCore.kt =====

package com.mpdc4gsr.libunified.app.matrix

import android.graphics.Bitmap

class NativeGuideCore {
    init {
        System.loadLibrary("guide_zm04c_matrix")
    }

    external fun toFloatTempMatrix(floats: FloatArray, bytes: ByteArray)
    external fun yuv2Bitmap(bitmap: Bitmap, yuv: ByteArray)
    external fun crc(data: ByteArray): Int
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\ResultCode.kt =====

package com.mpdc4gsr.libunified.app.matrix

object ResultCode {
    val TAG = "mobilelibrary"
    val READY_CONNECT_DEVICE = 1
    val SUCC_FIND_MATCHED_DEVICE = 2
    val SUCC_FIND_DEVICE_INTERFACE = 3
    val SUCC_CONNECT_INTERFACE = 4
    val SUCC_FIND_ENDPOINT = 5
    val SUCC_USB_SEND_CMD = 6
    val ERROR_FIND_DEVICE_NOT_MATCH = -100
    val ERROR_NOT_FIND_DEVICE = -101
    val ERROR_NOT_FIND_INTERFACE = -102
    val ERROR_OPEN_DEVICE_FAILD = -103
    val ERROR_CONNECT_DEVICE_FAILD = -104
    val ERROR_FIND_ENDPOINT_FAILD = -105
    val ERROR_USE_NOT_AGRREN_PERMISSIONS = -106
    val ERROR_USE_USB_ISVALID = -107
    val ERROE_USB_SEND_CMD_FAILD = -108
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\RingBuffer.kt =====

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

    fun write(buffer: ByteArray?, offset: Int, length: Int): Int {
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

    fun read(buffer: ByteArray?, offset: Int, length: Int): Int {
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

    fun getUnReadLength(): Int {
        return mUnReadLength
    }

    fun getMaxLength(): Int {
        return byteArray.size
    }

    fun getFreeSpace(): Int {
        return byteArray.size - mUnReadLength
    }

    fun getByteArray(): ByteArray? {
        return byteArray
    }

    fun getReadPositon(): Int {
        return mReadPositon
    }

    override fun toString(): String {
        return "RingBuffer(byteArray=${byteArray.contentToString()}, mReadPositon=$mReadPositon, mUnReadLength=$mUnReadLength)"
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\UsbBuffer.kt =====

package com.mpdc4gsr.libunified.app.matrix

import android.util.Log

class UsbBuffer {
    private val TAG = "UsbBuffer"
    private var mRingBuffer: RingBuffer
    private var mFrameSize = 0
    private var mark1 = 0
    private var mPacketSize = 0
    private var mPakagebuffer: ByteArray

    constructor(frameSize: Int, headSize: Int, count: Int) {
        mFrameSize = frameSize
        mPacketSize = headSize
        mRingBuffer = RingBuffer(mFrameSize * count)
        mPakagebuffer = ByteArray(mPacketSize)
    }

    fun setFrameMark(mark1: Int) {
        this.mark1 = mark1
    }

    fun write(buffer: ByteArray?, offset: Int, length: Int) {
        mRingBuffer.write(buffer, offset, length)
    }

    private var findHeadFrame = false
    private var findHeadFramePos = -1
    private fun getMark(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toUByte().toInt().shl(0) or ((buf[offset + 1].toUByte()).toInt()
            .shl(8)))
    }

    private fun isValidFrame(frame: ByteArray): Boolean {
        var i = 0
        while (i < frame.size - 1) {
            if (getMark(frame, i) == mark1) {
                return true
            }
            i += 2
        }
        return false
    }

    private fun isValidFrameInt(frame: ByteArray): Int {
        var i = 0
        while (i < frame.size - 1) {
            if (getMark(frame, i) == mark1) {
                return i
            }
            i += 2
        }
        return -1
    }

    fun readFrame(frame: ByteArray): Boolean {
        if (mRingBuffer.getUnReadLength() < mFrameSize * 4) {
//            Logger.d(TAG, "RingBuffer <4");
            return false
        }
        while (findHeadFramePos == -1 && mRingBuffer.getUnReadLength() > mFrameSize * 2) {
            mRingBuffer.read(mPakagebuffer, 0, mPakagebuffer.size)
            findHeadFramePos = if (mPacketSize == mPakagebuffer.size) {
                //findHeadFrame = isValidFrame(mPakagebuffer);
                isValidFrameInt(mPakagebuffer)
            } else {
                break
            }
        }
//        Log.d(TAG, "1 findHeadFrame=" + findHeadFrame);
        if (findHeadFramePos != -1) {
            //Log.d(TAG, "1: " + BaseDataTypeConvertUtils.Companion.byteArr2HexString(mPakagebuffer));
            mRingBuffer.moveBack(mPacketSize - findHeadFramePos)
            mRingBuffer.moveForward(mFrameSize)
            mRingBuffer.read(mPakagebuffer, 0, mPacketSize)
            //Log.d(TAG, "2: " + BaseDataTypeConvertUtils.Companion.byteArr2HexString(mPakagebuffer));
            findHeadFrame = if (mPacketSize == mPakagebuffer.size) {
                isValidFrame(mPakagebuffer)
            } else {
                false
            }
            mRingBuffer.moveBack(mFrameSize + if (findHeadFrame) mPacketSize else 0)
            findHeadFramePos = -1
        }
        if (findHeadFrame) {
            mRingBuffer.read(frame, 0, frame.size)
            return true
        }
        while (mRingBuffer.getUnReadLength() < mFrameSize * 2) {
            try {
                synchronized(this) {
                    Log.d(TAG, "wait(100)")
                    lock.wait(100)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        return false
    }

    private val lock = Object()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\matrix\UsbStatusInterface.kt =====

package com.mpdc4gsr.libunified.app.matrix

interface UsbStatusInterface {
    fun usbConnect()
    fun usbDisConnect()
}


