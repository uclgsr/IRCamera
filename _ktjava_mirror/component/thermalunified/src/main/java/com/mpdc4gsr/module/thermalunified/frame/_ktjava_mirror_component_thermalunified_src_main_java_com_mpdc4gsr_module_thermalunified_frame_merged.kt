// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_frame_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame' subtree
// Files: 3; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame\FrameStruct.kt =====

package com.mpdc4gsr.module.thermalunified.frame

import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.libunified.app.bean.CustomPseudoBean
import com.mpdc4gsr.libunified.app.bean.WatermarkBean
import com.mpdc4gsr.libunified.app.common.ProductType.PRODUCT_NAME_TC007
import com.mpdc4gsr.libunified.app.utils.ByteUtils
import com.mpdc4gsr.libunified.app.utils.ByteUtils.toBytes

class FrameStruct() {
    companion object {
        private const val SIZE = 1024
        fun toCode(
            name: String,
            width: Int,
            height: Int,
            rotate: Int,
            pseudo: Int,
            initRotate: Int,
            correctRotate: Int,
            customPseudoBean: CustomPseudoBean,
            isShowPseudoBar: Boolean,
            textColor: Int,
            watermarkBean: WatermarkBean,
            alarmBean: AlarmBean,
            gainStatus: Int,
            textSize: Int,
            environment: Float,
            distance: Float,
            radiation: Float,
            isAmplify: Boolean,
        ): ByteArray {
            val resultArray = ByteArray(SIZE)
            resultArray[0] = (SIZE ushr 8).toByte()
            resultArray[1] = SIZE.toByte()
            val nameBytes = name.toBytes(16)
            System.arraycopy(nameBytes, 0, resultArray, 2, nameBytes.size)
            val versionName = try {
                val packageInfo = ContextProvider.getContext().packageManager.getPackageInfo(
                    ContextProvider.getContext().packageName, 0
                )
                packageInfo.versionName ?: ""
            } catch (e: Exception) {
                ""
            }
            val verBytes = versionName.toBytes(8)
            System.arraycopy(verBytes, 0, resultArray, 18, verBytes.size)
            resultArray[26] = (width ushr 8).toByte()
            resultArray[27] = width.toByte()
            resultArray[28] = (height ushr 8).toByte()
            resultArray[29] = height.toByte()
            resultArray[30] = (rotate ushr 8).toByte()
            resultArray[31] = rotate.toByte()
            resultArray[32] = (pseudo ushr 8).toByte()
            resultArray[33] = pseudo.toByte()
            resultArray[34] = (initRotate ushr 8).toByte()
            resultArray[35] = initRotate.toByte()
            resultArray[36] = (correctRotate ushr 8).toByte()
            resultArray[37] = correctRotate.toByte()
            val customPseudoArray = customPseudoBean.toByteArray()
            System.arraycopy(customPseudoArray, 0, resultArray, 81, customPseudoArray.size)
            resultArray[173] = if (isShowPseudoBar) 1 else 0
            resultArray[174] = (textColor ushr 24).toByte()
            resultArray[175] = (textColor ushr 16).toByte()
            resultArray[176] = (textColor ushr 8).toByte()
            resultArray[177] = textColor.toByte()
            val watermarkArray = watermarkBean.toByteArray()
            System.arraycopy(watermarkArray, 0, resultArray, 178, watermarkArray.size)
            val alarmArray = alarmBean.toByteArray()
            System.arraycopy(alarmArray, 0, resultArray, 628, alarmArray.size)
            resultArray[657] = gainStatus.toByte()
            resultArray[658] = (textSize ushr 8).toByte()
            resultArray[659] = textSize.toByte()
            val envBytes = java.nio.ByteBuffer.allocate(4).putFloat(environment).array()
            val distanceBytes = java.nio.ByteBuffer.allocate(4).putFloat(distance).array()
            val radiationBytes = java.nio.ByteBuffer.allocate(4).putFloat(radiation).array()
            System.arraycopy(envBytes, 0, resultArray, 660, 4)
            System.arraycopy(distanceBytes, 0, resultArray, 664, 4)
            System.arraycopy(radiationBytes, 0, resultArray, 668, 4)
            resultArray[672] = if (isAmplify) 1 else 0
            return resultArray
        }
    }

    var len = 0
    var name: String = ""
    var ver: String = ""
    var width = 0
    var height = 0
    var rotate = 0
    var pseudo = 0
    var initRotate = 0
    var correctRotate = 0
    var customPseudoBean = CustomPseudoBean()
    var isShowPseudoBar = false
    var textColor = 0xffffffff.toInt()
    var watermarkBean = WatermarkBean()
    var alarmBean = AlarmBean()
    var gainStatus: Int = 1
    var textSize: Int = 14f.spToPx(ContextProvider.getContext()).toInt()
    var environment: Float = 0f
    var distance: Float = 0f
    var radiation: Float = 0f
    var isAmplify: Boolean = false

    constructor(data: ByteArray) : this() {
        len = (data[0].toInt() and 0xff shl 8) or (data[1].toInt() and 0xff)
        var nameEndIndex = 17
        for (i in 17 downTo 2) {
            if (data[i].toInt() != 0) {
                nameEndIndex = i
                break
            }
        }
        val nameBytes = ByteArray(nameEndIndex - 1)
        System.arraycopy(data, 2, nameBytes, 0, nameBytes.size)
        name = String(nameBytes)
        val verBytes = ByteArray(8)
        System.arraycopy(data, 18, verBytes, 0, verBytes.size)
        ver = String(verBytes)
        width = (data[26].toInt() and 0xff shl 8) or (data[27].toInt() and 0xff)
        height = (data[28].toInt() and 0xff shl 8) or (data[29].toInt() and 0xff)
        rotate = (data[30].toInt() and 0xff shl 8) or (data[31].toInt() and 0xff)
        pseudo = (data[32].toInt() and 0xff shl 8) or (data[33].toInt() and 0xff)
        initRotate = (data[34].toInt() and 0xff shl 8) or (data[35].toInt() and 0xff)
        correctRotate = (data[36].toInt() and 0xff shl 8) or (data[37].toInt() and 0xff)
        val customPseudoArray = ByteArray(92)
        System.arraycopy(data, 81, customPseudoArray, 0, customPseudoArray.size)
        customPseudoBean = CustomPseudoBean.toCustomPseudoBean(customPseudoArray)
        isShowPseudoBar = data[173].toInt() == 1
        textColor = ByteUtils.bigBytesToInt(data[174], data[175], data[176], data[177])
        if (textColor == 0) {
            textColor = 0xffffffff.toInt()
        }
        val watermarkArray = ByteArray(450)
        System.arraycopy(data, 178, watermarkArray, 0, watermarkArray.size)
        watermarkBean = WatermarkBean.loadFromArray(watermarkArray)
        val alarmArray = ByteArray(28)
        System.arraycopy(data, 628, alarmArray, 0, alarmArray.size)
        alarmBean = AlarmBean.loadFromArray(alarmArray)
        gainStatus = data[657].toInt()
        val tmpTextSize = (data[658].toInt() and 0xff shl 8) or (data[659].toInt() and 0xff)
        if (tmpTextSize >= 14f.spToPx(ContextProvider.getContext())) {
            textSize = tmpTextSize
        }
        val envBytes = data.copyOfRange(660, 664)
        val distanceBytes = data.copyOfRange(664, 668)
        val radiationBytes = data.copyOfRange(668, 672)
        environment = java.nio.ByteBuffer.wrap(envBytes).float
        distance = java.nio.ByteBuffer.wrap(distanceBytes).float
        radiation = java.nio.ByteBuffer.wrap(radiationBytes).float
        isAmplify = data[672].toInt() == 1
    }

    fun isTC007(): Boolean = name == PRODUCT_NAME_TC007
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame\FrameTools.kt =====

package com.mpdc4gsr.module.thermalunified.frame

import android.graphics.Bitmap
import android.graphics.Rect
import com.elvishew.xlog.XLog
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.utils.CommonParams
import com.example.suplib.wrapper.SupHelp
import com.mpdc4gsr.libunified.app.bean.CustomPseudoBean
import com.mpdc4gsr.libunified.ir.tools.ImageTools
import com.mpdc4gsr.libunified.ir.utils.IRImageHelp
import com.mpdc4gsr.libunified.ir.utils.OpencvTools
import java.io.IOException
import java.nio.ByteBuffer

class FrameTool {
    val imageWidth = 256
    val imageHeight = 192
    private val scrImageLen = imageWidth * imageHeight * 2
    private val srcTemperatureLen = imageWidth * imageHeight * 2
    private val imageBytes = ByteArray(scrImageLen)
    val temperatureBytes = ByteArray(srcTemperatureLen)
    private val imageRes = LibIRProcess.ImageRes_t()
    private var struct: FrameStruct = FrameStruct()
    private var maxLimit = -273f
    private var minLimit = -273f
    private var irImageHelp = IRImageHelp()
    private val supImageData = ByteArray(imageWidth * imageHeight * 4 * 4)
    private var dstArgbBytes: ByteArray? = null
    fun read(bytes: ByteArray) {
        try {
            val frame = ByteArray(bytes.size)
            System.arraycopy(bytes, 0, frame, 0, frame.size)
            println("bs len: ${frame.size}")
            System.arraycopy(frame, 0, imageBytes, 0, scrImageLen)
            System.arraycopy(
                frame,
                scrImageLen,
                temperatureBytes,
                0,
                srcTemperatureLen
            )
            println("imageBytes len: ${imageBytes.size}")
            println("temperatureBytes len: ${temperatureBytes.size}")
        } catch (e: Exception) {
            e.printStackTrace()
            XLog.e("Failed to read frame raw data: ${e.message}")
        }
    }

    fun initStruct(struct: FrameStruct) {
        this.struct = struct
        imageRes.width = imageWidth.toChar()
        imageRes.height = imageHeight.toChar()
    }

    fun initRotate(): ImageParams {
        var rotate = ImageParams.ROTATE_0
        when (struct.rotate) {
            0 -> rotate = ImageParams.ROTATE_0
            90 -> rotate = ImageParams.ROTATE_270
            180 -> rotate = ImageParams.ROTATE_180
            270 -> rotate = ImageParams.ROTATE_90
        }
        return rotate
    }

    fun getTempBytes(rotate: ImageParams = ImageParams.ROTATE_0): ByteArray {
        val tempBytes = ByteArray(srcTemperatureLen)
        val dstTempBytes = ByteArray(srcTemperatureLen)
        System.arraycopy(temperatureBytes, 0, tempBytes, 0, srcTemperatureLen)
        when (rotate) {
            ImageParams.ROTATE_270 ->
                LibIRProcess.rotateLeft90(
                    tempBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    dstTempBytes,
                )

            ImageParams.ROTATE_90 ->
                LibIRProcess.rotateRight90(
                    tempBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    dstTempBytes,
                )

            ImageParams.ROTATE_180 ->
                LibIRProcess.rotate180(
                    tempBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    dstTempBytes,
                )

            else -> System.arraycopy(temperatureBytes, 0, dstTempBytes, 0, srcTemperatureLen)
        }
        return dstTempBytes
    }

    fun getRotate90Temp(temperatureBytes: ByteArray): ByteArray {
        val tempBytes = ByteArray(temperatureBytes.size)
        val dstTempBytes = ByteArray(temperatureBytes.size)
        System.arraycopy(temperatureBytes, 0, tempBytes, 0, temperatureBytes.size)
        val imgRes = LibIRProcess.ImageRes_t()
        imgRes.height = 192.toChar()
        imgRes.width = 256.toChar()
        LibIRProcess.rotateRight90(
            tempBytes,
            imgRes,
            CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
            dstTempBytes
        )
        return dstTempBytes
    }

    fun getScrPseudoColorScaledBitmap(
        pseudoColorMode: CommonParams.PseudoColorType = CommonParams.PseudoColorType.PSEUDO_3,
        max: Float = -273f,
        min: Float = 273f,
        rotate: ImageParams = ImageParams.ROTATE_0,
        customPseudoBean: CustomPseudoBean,
        maxTemperature: Float,
        minTemperature: Float,
        isAmplify: Boolean,
    ): Bitmap? {
        maxLimit = max
        minLimit = min
        val imageBytesTemp = ByteArray(imageBytes.size)
        System.arraycopy(imageBytes, 0, imageBytesTemp, 0, imageBytesTemp.size)
        val pixNum = imageWidth * imageHeight
        val argbLen = pixNum * 4
        var argbBytes = ByteArray(argbLen)
        dstArgbBytes = ByteArray(argbLen)
        val maxRGB = IntArray(3)
        val minRGB = IntArray(3)
        if (customPseudoBean.isUseCustomPseudo) {
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                imageBytesTemp,
                pixNum.toLong(),
                CommonParams.PseudoColorType.PSEUDO_1,
                argbBytes
            )
            val colorList: IntArray? = customPseudoBean.getColorList(struct.isTC007())
            val places: FloatArray? = customPseudoBean.getPlaceList()
            if (colorList != null) {
                val customMaxTemp = customPseudoBean.maxTemp
                val customMinTemp = customPseudoBean.minTemp
                val maxColor: Int = colorList.last()
                val minColor: Int = colorList.first()
                maxRGB[0] = maxColor shr 16 and 0xFF
                maxRGB[1] = maxColor shr 8 and 0xFF
                maxRGB[2] = maxColor and 0xFF
                minRGB[0] = minColor shr 16 and 0xFF
                minRGB[1] = minColor shr 8 and 0xFF
                minRGB[2] = minColor and 0xFF
                var j = 0
                val argbBytesLength = imageWidth * imageHeight * 4
                var index = 0
                while (index < argbBytesLength) {
                    var temperature0: Float =
                        (
                                (temperatureBytes[j].toInt() and 0xff) + (
                                        temperatureBytes[j + 1]
                                            .toInt() and 0xff
                                        ) * 256
                                ).toFloat()
                    temperature0 = (temperature0 / 64 - 273.15).toFloat()
                    if (temperature0 in customMinTemp..customMaxTemp) {
                        val rgb =
                            OpencvTools.getOneColorByTempUnif(
                                customMaxTemp,
                                customMinTemp,
                                temperature0,
                                colorList,
                                places,
                            )
                        if (rgb != null) {
                            argbBytes[index] = rgb[0].toByte()
                            argbBytes[index + 1] = rgb[1].toByte()
                            argbBytes[index + 2] = rgb[2].toByte()
                        }
                    } else if (temperature0 > customMaxTemp) {
                        if (!customPseudoBean.isUseGray) {
                            argbBytes[index] = maxRGB[0].toByte()
                            argbBytes[index + 1] = maxRGB[1].toByte()
                            argbBytes[index + 2] = maxRGB[2].toByte()
                        }
                    } else if (temperature0 < customMinTemp) {
                        if (!customPseudoBean.isUseGray) {
                            argbBytes[index] = minRGB[0].toByte()
                            argbBytes[index + 1] = minRGB[1].toByte()
                            argbBytes[index + 2] = minRGB[2].toByte()
                        }
                    }
                    argbBytes[index + 3] = 255.toByte()
                    index += 4
                    j += 2
                }
            }
        } else {
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                imageBytesTemp,
                pixNum.toLong(),
                pseudoColorMode,
                argbBytes
            )
            if (!(maxLimit == -273f && minLimit == -273f) && !(maxTemperature == maxLimit && minLimit == minTemperature)) {
                ImageTools.dualReadFrame(
                    argbBytes,
                    temperatureBytes,
                    maxLimit,
                    minLimit
                )
            }
        }
        if ((struct.alarmBean.isHighOpen && struct.alarmBean.highTemp != Float.MAX_VALUE) ||
            (struct.alarmBean.isLowOpen && struct.alarmBean.lowTemp != Float.MIN_VALUE)
        ) {
            try {
                argbBytes =
                    irImageHelp.contourDetection(
                        struct.alarmBean,
                        argbBytes, temperatureBytes,
                        imageWidth,
                        imageHeight,
                    )!!
            } catch (e: IOException) {
            }
        }
        argbBytesRotate(argbBytes, dstArgbBytes!!, rotate)
        val dstImageRes = getDstImageRes(rotate)
        var scrBitmap: Bitmap? = null
        if (isAmplify) {
            val initResult: Any? = SupHelp.getInstance().initA4KCPP()
            if (SupHelp.getInstance().loadOpenclSuccess) {
                OpencvTools.supImage(
                    dstArgbBytes,
                    dstImageRes.height.code,
                    dstImageRes.width.code,
                    supImageData,
                )
                scrBitmap =
                    Bitmap.createBitmap(
                        dstImageRes.width.code * 2,
                        dstImageRes.height.code * 2, Bitmap.Config.ARGB_8888,
                    )
                scrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(supImageData, 0, argbLen * 4))
            } else {
                scrBitmap =
                    Bitmap.createBitmap(
                        dstImageRes.width.code,
                        dstImageRes.height.code, Bitmap.Config.ARGB_8888,
                    )
                dstArgbBytes?.let {
                    scrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(it, 0, argbLen))
                }
            }
        } else {
            scrBitmap =
                Bitmap.createBitmap(
                    dstImageRes.width.code,
                    dstImageRes.height.code, Bitmap.Config.ARGB_8888,
                )
            dstArgbBytes?.let {
                scrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(it, 0, argbLen))
            }
        }
        return scrBitmap
    }

    fun getBaseBitmap(rotate: ImageParams): Bitmap {
        val dstImageRes = getDstImageRes(rotate)
        val scrBitmap =
            Bitmap.createBitmap(
                dstImageRes.width.code,
                dstImageRes.height.code,
                Bitmap.Config.ARGB_8888,
            )
        dstArgbBytes?.let {
            scrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(it, 0, it.size))
        }
        return scrBitmap
    }

    private fun getDstImageRes(rotate: ImageParams): LibIRProcess.ImageRes_t {
        val dstImageRes = LibIRProcess.ImageRes_t()
        if (rotate == ImageParams.ROTATE_270 || rotate == ImageParams.ROTATE_90) {
            dstImageRes.width = imageRes.height
            dstImageRes.height = imageRes.width
        } else {
            dstImageRes.width = imageRes.width
            dstImageRes.height = imageRes.height
        }
        return dstImageRes
    }

    private fun argbBytesRotate(
        argbBytes: ByteArray,
        dstArgbBytes: ByteArray,
        rotate: ImageParams,
    ) {
        when (rotate) {
            ImageParams.ROTATE_270 ->
                LibIRProcess.rotateLeft90(
                    argbBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                    dstArgbBytes,
                )

            ImageParams.ROTATE_90 ->
                LibIRProcess.rotateRight90(
                    argbBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                    dstArgbBytes,
                )

            ImageParams.ROTATE_180 ->
                LibIRProcess.rotate180(
                    argbBytes,
                    imageRes,
                    CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                    dstArgbBytes,
                )

            else -> System.arraycopy(argbBytes, 0, dstArgbBytes, 0, argbBytes.size)
        }
    }

    fun getSrcTemp(): LibIRTemp.TemperatureSampleResult {
        val irTemp = LibIRTemp(imageWidth, imageHeight)
        irTemp.setTempData(temperatureBytes)
        return irTemp.getTemperatureOfRect(Rect(0, 0, imageWidth, imageHeight))
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\frame\ImageParams.kt =====

package com.mpdc4gsr.module.thermalunified.frame

enum class ImageParams(val value: Int) {
    ROTATE_0(0),
    ROTATE_90(1),
    ROTATE_180(2),
    ROTATE_270(3),
}