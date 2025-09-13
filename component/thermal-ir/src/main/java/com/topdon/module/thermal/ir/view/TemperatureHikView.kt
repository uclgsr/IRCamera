package com.topdon.module.thermal.ir.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.sdkisp.LibIRTemp.TemperatureSampleResult
import com.energy.iruvc.utils.CommonParams.IRPROCSRCFMTType
import com.energy.iruvc.utils.Line
import com.infisense.usbir.utils.TempDrawHelper.Companion.correct
import com.infisense.usbir.utils.TempUtil

/**
海康pointlineareatemperature图层 View.
 *
 * Created by LCG on 2024/12/19.
 */
/**
 * Custom Temperature hik view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
class TemperatureHikView : TemperatureBaseView {
    /**
要drawing的temperatureinfo
     */
    @Volatile
    private var tempInfo = TempInfo()

    /**
calculationtemperature的工具class.
     */
    private var libIRTemp = LibIRTemp()

    /**
calculationtemperature的line程.
     */
    private var calculateThread: CalculateThread? = null

    /**
thermal imaging画area逆时针rotation角度，取值 0、90、180、270，默认 270
     */
    @Volatile
    var rotateAngle: Int = 270
        set(value) {
            field = value
            val isPortrait = value == 90 || value == 270
            setImageSize(if (isPortrait) 192 else 256, if (isPortrait) 256 else 192)
        }

    /**
temperature变更EventListener，temperature单位均为 **摄氏度**
     */
    @Volatile
    var onTempChangeListener: ((min: Float, max: Float) -> Unit)? = null

    /**
趋势图直line对应的temperaturedata变更Listener，单位摄氏度.
     */
    var onTrendChangeListener: ((tempList: List<Float>) -> Unit)? = null

    /**
temperature measurement结果Callback，单位均为摄氏度.
     */
    var onTempResultListener: ((tempInfo: TempInfo) -> Unit)? = null

    /**
当尚未经过 onMeasure 调用addpoint时，save要add的以 temperature scale寸 为坐标的point，在 onMeasure 阶段add。
     */
    private var wantAddPoint: Point? = null

    /**
当尚未经过 onMeasure 调用addpoint时，save要add的以 temperature scale寸 为坐标的line，在 onMeasure 阶段add。
     */
    private var wantAddLine: Line? = null

    /**
当尚未经过 onMeasure 调用addpoint时，save要add的以 temperature scale寸 为坐标的area，在 onMeasure 阶段add。
     */
    private var wantAddRect: Rect? = null

    /**
add一个以 temperature scale寸 为坐标的point
     */
    fun addSourcePoint(point: Point) {
        if (xScale > 0 && yScale > 0) {
            synchronized(this) {
                if (pointList.size == maxCount) { // 新增时已达最大数量
                    pointList.removeAt(0)
                }
                pointList.add(Point((point.x * xScale).toInt(), (point.y * yScale).toInt()))
            }
            invalidate()
        } else {
            wantAddPoint = point
        }
    }

    /**
add一个以 temperature scale寸 为坐标的line
     */
    fun addSourceLine(line: Line) {
        if (xScale > 0 && yScale > 0) {
            val start = Point((line.start.x * xScale).toInt(), (line.start.y * yScale).toInt())
            val end = Point((line.end.x * xScale).toInt(), (line.end.y * yScale).toInt())
            synchronized(this) {
                if (lineList.size == maxCount) { // 新增时已达最大数量
                    lineList.removeAt(0)
                }
                lineList.add(Line(start, end))
            }
            invalidate()
        } else {
            wantAddLine = line
        }
    }

    /**
add一个以 temperature scale寸 为坐标的area
     */
    fun addSourceRect(rect: Rect) {
        if (xScale > 0 && yScale > 0) {
            val left = (rect.left * xScale).toInt()
            val right = (rect.right * xScale).toInt()
            val top = (rect.top * yScale).toInt()
            val bottom = (rect.bottom * yScale).toInt()
            synchronized(this) {
                if (rectList.size == maxCount) { // 新增时已达最大数量
                    rectList.removeAt(0)
                }
                rectList.add(Rect(left, top, right, bottom))
            }
            invalidate()
        } else {
            wantAddRect = rect
        }
    }

    /**
用于temperature及画arearotationparameter的尺寸.
     */
    private val imageRes = LibIRProcess.ImageRes_t()

    /**
上一次执行temperaturearrayCallback的时间戳，用于控制 1 秒Callback 1 次.
     */
    private var beforeTime: Long = 0

    /**
未rotation前的temperaturearray.
     */
    private val sourceTempArray = ByteArray(256 * 192 * 2)

    /**
rotation后的temperaturearray，趋势图要用，而 [libIRTemp] 又没提供method读取里area的data，只好再搞一份
     */
    private val rotateTempArray = ByteArray(256 * 192 * 2)

    /**
refreshtemperaturedata
     */
    fun refreshTemp(newData: ByteArray) {
        val currentTime: Long = System.currentTimeMillis()
        if (currentTime - beforeTime > 1000) {
            beforeTime = currentTime

            System.arraycopy(newData, 0, sourceTempArray, 0, sourceTempArray.size)
            when (rotateAngle) {
                90 -> LibIRProcess.rotateLeft90(sourceTempArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, rotateTempArray)
                180 -> LibIRProcess.rotate180(sourceTempArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, rotateTempArray)
                270 -> LibIRProcess.rotateRight90(sourceTempArray, imageRes, IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, rotateTempArray)
                else -> System.arraycopy(sourceTempArray, 0, rotateTempArray, 0, rotateTempArray.size)
            }

            libIRTemp.setTempData(rotateTempArray)
            if (mode != Mode.CLEAR) {
                calculateThread?.calculateTemp()
            }
        }
    }

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
        setImageSize(192, 256)
        if (context is ComponentActivity) {
            context.lifecycle.addObserver(MyLifecycleObserver())
        }
    }

    override fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
    ) {
        super.setImageSize(imageWidth, imageHeight)
        libIRTemp = LibIRTemp(imageWidth, imageHeight)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        wantAddPoint?.let {
            addSourcePoint(it)
            wantAddPoint = null
        }
        wantAddLine?.let {
            addSourceLine(it)
            wantAddLine = null
        }
        wantAddRect?.let {
            addSourceRect(it)
            wantAddRect = null
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
由于历史遗留，2D编辑与thermal imaging的centerpoint逻辑不一致
2D编辑centerpoint跟随full imageset，full image开则开，full image关则关；thermal imagingcenterpoint为enabledfull image或有point、line、area则display
产品没明确定义centerpoint的逻辑，这里先照着thermal imaging来做
centerpoint
        if (isShowFull || pointList.isNotEmpty() || lineList.isNotEmpty() || rectList.isNotEmpty()) {
            drawPoint(canvas, Point(width / 2, height / 2))
            tempInfo.center?.let {
                drawTempText(canvas, width / 2, height / 2, it.maxTemperature)
            }
        }

full imageminimum、最high temperature
        if (isShowFull) {
            tempInfo.full?.let {
                val minX: Int = (it.minTemperaturePixel.x * xScale).toInt()
                val minY: Int = (it.minTemperaturePixel.y * yScale).toInt()
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, minX, minY, it.minTemperature)

                val maxX: Int = (it.maxTemperaturePixel.x * xScale).toInt()
                val maxY: Int = (it.maxTemperaturePixel.y * yScale).toInt()
                drawCircle(canvas, maxX, maxY, true)
                drawTempText(canvas, maxX, maxY, it.maxTemperature)
            }
        }

point
        for (i in pointList.indices) {
            val point: Point = pointList[i]
            drawPoint(canvas, point)
            if (i < tempInfo.pointResults.size) {
                drawCircle(canvas, point.x, point.y, true)
                drawTempText(canvas, point.x, point.y, tempInfo.pointResults[i].maxTemperature)
            }
        }
        operatePoint?.let { drawPoint(canvas, it) }

line
        for (i in lineList.indices) {
            drawLine(canvas, lineList[i])
            if (i < tempInfo.lineResults.size) {
                val result: TemperatureSampleResult = tempInfo.lineResults[i]
                val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
                val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
                val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
                val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, maxX, maxY, true)
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, maxX, maxY, result.maxTemperature)
                drawTempText(canvas, minX, minY, result.minTemperature)
            }
        }
        operateLine?.let { drawLine(canvas, it) }

area
        for (i in rectList.indices) {
            drawRect(canvas, rectList[i])
            if (i < tempInfo.rectResults.size) {
                val result: TemperatureSampleResult = tempInfo.rectResults[i]
                val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
                val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
                val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
                val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, maxX, maxY, true)
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, maxX, maxY, result.maxTemperature)
                drawTempText(canvas, minX, minY, result.minTemperature)
            }
        }
        operateRect?.let { drawRect(canvas, it) }

趋势图
        trendLine?.let {
            drawLine(canvas, it)
            drawTrendText(canvas, it)
            val result: TemperatureSampleResult = tempInfo.trend ?: return@let
            val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
            val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
            val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
            val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
            drawCircle(canvas, maxX, maxY, true)
            drawCircle(canvas, minX, minY, false)
            drawTempText(canvas, maxX, maxY, result.maxTemperature)
            drawTempText(canvas, minX, minY, result.minTemperature)
        }
        operateTrend?.let {
            drawLine(canvas, it)
            drawTrendText(canvas, it)
        }
    }

    /**
执行temperaturecalculation的line程.
     */
    private inner class CalculateThread : HandlerThread("Calculate Thread") {
        private val mainHandler = Handler(Looper.getMainLooper())
        private var currentHandler: Handler? = null

        override fun start() {
            super.start()
            val looper: Looper = getLooper() ?: return
            currentHandler = MyHandler(looper)
        }

        override fun quit(): Boolean {
            mainHandler.removeCallbacksAndMessages(null)
            return super.quit()
        }

        fun calculateTemp() {
            currentHandler?.sendEmptyMessage(0)
        }

        private inner class MyHandler(looper: Looper) : Handler(looper) {
            override fun handleMessage(msg: Message) {
                val fullResult = libIRTemp.getTemperatureOfRect(Rect(0, 0, imageWidth, imageHeight))
                mainHandler.post {
                    onTempChangeListener?.invoke(fullResult.minTemperature, fullResult.maxTemperature)
                }

                if (mode == Mode.CLEAR) {
                    return
                }

                val centerResult = if (isShowFull) libIRTemp.getTemperatureOfPoint(Point(imageWidth / 2, imageHeight / 2)) else null

                var trendResult: TemperatureSampleResult? = null
                trendLine?.let {
                    val startPoint = Point((it.start.x / xScale).toInt(), (it.start.y / yScale).toInt())
                    val endPoint = Point((it.end.x / xScale).toInt(), (it.end.y / yScale).toInt())
                    try {
                        trendResult = libIRTemp.getTemperatureOfLine(Line(startPoint, endPoint))
                    } catch (_: IllegalArgumentException) {
当 View 尺寸变更就会导致 xScale、yScale 变更，而已drawing的pointlinearea坐标还是未变更前的坐标
以 旧坐标及新 scale 去calculationtemperature坐标的话，就有可能超出temperature坐标range从而抛出exception，这里捕获
                    }

                    val tempList: List<Float> = TempUtil.getLineTemps(startPoint, endPoint, rotateTempArray, imageWidth)
                    mainHandler.post {
                        onTrendChangeListener?.invoke(tempList)
                    }
                }

                val pointList: List<Point> = getPointListSafe()
                val pointResultList: ArrayList<TemperatureSampleResult> = ArrayList(pointList.size)
                for (point in pointList) {
                    val sourcePoint = Point((point.x / xScale).toInt(), (point.y / yScale).toInt())
                    try {
                        pointResultList.add(libIRTemp.getTemperatureOfPoint(sourcePoint))
                    } catch (_: IllegalArgumentException) {
当 View 尺寸变更就会导致 xScale、yScale 变更，而已drawing的pointlinearea坐标还是未变更前的坐标
以 旧坐标及新 scale 去calculationtemperature坐标的话，就有可能超出temperature坐标range从而抛出exception，这里捕获
                    }
                }

                val lineList: List<Line> = getLineListSafe()
                val lineResultList: ArrayList<TemperatureSampleResult> = ArrayList(lineList.size)
                for (line in lineList) {
                    val sourceLine =
                        Line(
                            Point((line.start.x / xScale).toInt(), (line.start.y / yScale).toInt()),
                            Point((line.end.x / xScale).toInt(), (line.end.y / yScale).toInt()),
                        )
                    try {
                        lineResultList.add(libIRTemp.getTemperatureOfLine(sourceLine))
                    } catch (_: IllegalArgumentException) {
当 View 尺寸变更就会导致 xScale、yScale 变更，而已drawing的pointlinearea坐标还是未变更前的坐标
以 旧坐标及新 scale 去calculationtemperature坐标的话，就有可能超出temperature坐标range从而抛出exception，这里捕获
                    }
                }

                val rectList: List<Rect> = getRectListSafe()
                val rectResultList: ArrayList<TemperatureSampleResult> = ArrayList(rectList.size)
                for (rect in rectList) {
                    val sourceRect =
                        Rect(
                            (rect.left / xScale).toInt(),
                            (rect.top / yScale).toInt(),
                            (rect.right / xScale).toInt(),
                            (rect.bottom / yScale).toInt(),
                        )
                    try {
                        rectResultList.add(libIRTemp.getTemperatureOfRect(sourceRect))
                    } catch (_: IllegalArgumentException) {
当 View 尺寸变更就会导致 xScale、yScale 变更，而已drawing的pointlinearea坐标还是未变更前的坐标
以 旧坐标及新 scale 去calculationtemperature坐标的话，就有可能超出temperature坐标range从而抛出exception，这里捕获
                    }
                }

                tempInfo = TempInfo(centerResult, if (isShowFull) fullResult else null, trendResult, pointResultList, lineResultList, rectResultList)
                mainHandler.post {
                    onTempResultListener?.invoke(tempInfo)
                }
                postInvalidate()
            }
        }
    }

    private inner class MyLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            calculateThread = CalculateThread()
            calculateThread?.start()
        }

        override fun onStop(owner: LifecycleOwner) {
            calculateThread?.quit()
            calculateThread = null
        }
    }

    /**
pointlineareafull image等temperaturecalculation结果info封装，坐标采用 View 坐标，单位均为摄氏度
     */
/**
 * Temp info utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
data class TempInfo(
        val center: TemperatureSampleResult? = null,
        val full: TemperatureSampleResult? = null,
        val trend: TemperatureSampleResult? = null,
        val pointResults: List<TemperatureSampleResult> = ArrayList(0),
        val lineResults: List<TemperatureSampleResult> = ArrayList(0),
        val rectResults: List<TemperatureSampleResult> = ArrayList(0),
    )
}
