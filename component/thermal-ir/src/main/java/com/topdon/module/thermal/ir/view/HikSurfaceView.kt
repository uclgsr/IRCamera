package com.topdon.module.thermal.ir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceView
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRProcess.ImageRes_t
import com.energy.iruvc.utils.CommonParams.IRPROCSRCFMTType
import com.energy.iruvc.utils.CommonParams.PseudoColorType
import com.infisense.usbir.utils.IRImageHelp
import com.infisense.usbir.utils.OpencvTools
import com.infisense.usbir.utils.PseudocodeUtils
import com.topdon.lib.core.bean.AlarmBean
// import com.topdon.pseudo.bean.CustomPseudoBean  // Temporarily disabled - pseudo component dependency
import com.topdon.module.thermal.ir.bean.DataBean // Use local data bean instead
import java.nio.ByteBuffer

/**
进行 Hik module预览的 SurfaceView.
 *
 * Created by LCG on 2024/11/30.
 */
/**
 * Custom Hik surface view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
class HikSurfaceView : SurfaceView {
    companion object {
        /**
超分amplification倍数
         */
        private const val MULTIPLE = 2
    }

    /**
是否enabled超分
     */
    var isOpenAmplify: Boolean = false
        set(value) {
            field = value
            val isPortrait = rotateAngle == 90 || rotateAngle == 270
            val width = (if (isPortrait) 192 else 256) * (if (value) MULTIPLE else 1)
            val height = (if (isPortrait) 256 else 192) * (if (value) MULTIPLE else 1)
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

    /**
thermal imaging画area逆时针rotation角度，取值 0、90、180、270，默认 270
     */
    @Volatile
    var rotateAngle: Int = 270
        set(value) {
            field = value
            val isPortrait = value == 90 || value == 270
            val width = (if (isPortrait) 192 else 256) * (if (isOpenAmplify) MULTIPLE else 1)
            val height = (if (isPortrait) 256 else 192) * (if (isOpenAmplify) MULTIPLE else 1)
            bitmap.reconfigure(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        }

    /**
temperature报警configurationinfo，用于drawingoutline或矩形.
     */
    var alarmBean = AlarmBean()

    /**
等温尺限制的low temperature值，单位摄氏度，MIN_VALUE 表示未set
     */
    var limitTempMin = Float.MIN_VALUE

    /**
等温尺限制的high temperature值，单位摄氏度，MAX_VALUE 表示未set
     */
    var limitTempMax = Float.MAX_VALUE

    /**
temperature报警用来outline的工具class.
     */
    private val irImageHelp = IRImageHelp()

    /**
refresh自定义renderingconfiguration
     */
    fun refreshCustomPseudo(it: DataBean) {
        // Temporarily disabled - pseudo component dependency
        // irImageHelp.setColorList(it.getColorList(), it.getPlaceList(), it.isUseGray, it.maxTemp, it.minTemp)
    }

    /**
当前使用pseudo-color.
     */
    @Volatile
    private var pseudoType: PseudoColorType = PseudoColorType.PSEUDO_3

    /**
set当前使用的pseudo-color代号
     *
1-White Hot 3-Iron Red 4-Rainbow 1 5-Rainbow 2 6-Rainbow 3 7-Red Hot 8-Hot Iron 9-Rainbow 4 10-Rainbow 5 11-Black Hot
     */
    fun setPseudoCode(code: Int) {
        pseudoType = PseudocodeUtils.changePseudocodeModeByOld(code)
    }

    /**
用于temperature及画arearotationparameter的尺寸.
     */
    private val imageRes = ImageRes_t()

    /**
当前displayimage的 Bitmap.
     */
    private var bitmap: Bitmap = Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)

    /**
未rotation前的 ARGB array.
     */
    private val sourceArgbArray = ByteArray(256 * 192 * 4)

    /**
rotation后的 ARGB array.
     */
    private val rotateArgbArray = ByteArray(256 * 192 * 4)

    /**
超分后的 ARGB array.
     */
    private val amplifyArray = ByteArray(256 * MULTIPLE * 192 * MULTIPLE * 4)

    /**
temperaturearray
     */
    private val tempArray = ByteArray(256 * 192 * 2)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        imageRes.width = 256.toChar()
        imageRes.height = 192.toChar()
    }

    /**
getscaling为当前 View 尺寸的image.
     */
    fun getScaleBitmap(): Bitmap =
        synchronized(this) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }

    /**
使用指定的 YUV datarefresh画area
     */
    fun refresh(
        yuvArray: ByteArray,
        newTempArray: ByteArray,
    ) {
raw data的宽高，即不应用rotation的宽高
        val sourceWidth = 256
        val sourceHeight = 192

        System.arraycopy(newTempArray, 0, tempArray, 0, tempArray.size)

自定义rendering时使用white hotpseudo-color，当置灰mode时range外直接不用改
        val pseudo: PseudoColorType = if (irImageHelp.getColorList() == null) pseudoType else PseudoColorType.PSEUDO_1
        LibIRProcess.convertYuyvMapToARGBPseudocolor(yuvArray, (sourceWidth * sourceHeight).toLong(), pseudo, sourceArgbArray)
自定义rendering
        irImageHelp.customPseudoColor(sourceArgbArray, tempArray, sourceWidth, sourceHeight)
等温尺
        irImageHelp.setPseudoColorMaxMin(sourceArgbArray, tempArray, limitTempMax, limitTempMin, sourceWidth, sourceHeight)
temperature报警outline或矩形
        val newArray = irImageHelp.contourDetection(alarmBean, sourceArgbArray, tempArray, sourceWidth, sourceHeight) ?: sourceArgbArray
rotation
        when (rotateAngle) {
            90 -> LibIRProcess.rotateLeft90(newArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotateArgbArray)
            180 -> LibIRProcess.rotate180(newArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotateArgbArray)
            270 -> LibIRProcess.rotateRight90(newArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotateArgbArray)
            else -> System.arraycopy(newArray, 0, rotateArgbArray, 0, rotateArgbArray.size)
        }
超分
        if (isOpenAmplify) {
            val width: Int = if (rotateAngle == 90 || rotateAngle == 270) sourceWidth else sourceHeight
            val height: Int = if (rotateAngle == 90 || rotateAngle == 270) sourceHeight else sourceWidth
            OpencvTools.supImage(rotateArgbArray, width, height, amplifyArray)
        }

        synchronized(this) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(if (isOpenAmplify) amplifyArray else rotateArgbArray))
        }

        val canvas: Canvas = holder.lockCanvas() ?: return
        canvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), null)
        holder.unlockCanvasAndPost(canvas)
    }
}
