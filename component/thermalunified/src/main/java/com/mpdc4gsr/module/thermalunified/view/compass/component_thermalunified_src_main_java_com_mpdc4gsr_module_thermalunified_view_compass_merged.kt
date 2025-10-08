// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass' directory and its subdirectories.
// Total files: 7 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassProvider.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor

class CompassProvider(private val context: Context) {
    fun get(): ICompass {
        val smoothing = 1
        val useTrueNorth = true
        var source = CompassSource.RotationVector
        val allSources = getAvailableSources(context)
        if (allSources.isEmpty()) {
            return NullCompass()
        }
        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }
        val compass =
            when (source) {
                CompassSource.RotationVector -> {
                    RotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.GeomagneticRotationVector -> {
                    GeomagneticRotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.CustomMagnetometer -> {
                    RotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.Orientation -> {
                    LegacyCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
                }
            }
        return compass as ICompass
    }

    companion object {
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()
            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }
            if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
                sources.add(CompassSource.GeomagneticRotationVector)
            }
            if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
                sources.add(CompassSource.CustomMagnetometer)
            }
            @Suppress("DEPRECATION")
            if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
                sources.add(CompassSource.Orientation)
            }
            return sources
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassSource.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

enum class CompassSource(val id: String) {
    RotationVector("rotation_vector"),
    GeomagneticRotationVector("geomagnetic_rotation_vector"),
    CustomMagnetometer("custom_magnetometer"),
    Orientation("orientation"),
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\LinearCompassView.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.module.thermalunified.utils.getPixelLinear
import com.mpdc4gsr.module.thermalunified.utils.getValuesBetween
import com.mpdc4gsr.module.thermalunified.utils.realX
import com.mpdc4gsr.module.thermalunified.utils.realY
import kotlinx.coroutines.*

class LinearCompassView : View {
    private val paint = Paint()
    private val textPaint = Paint()
    private val markerPaint = Paint()
    private val shortLinePaint = Paint()
    private val longLinePaint = Paint()
    private val positionPaint = Paint()
    private lateinit var canvas: Canvas
    private var lineColor: Int = Color.WHITE
    private var textColor: Int = Color.WHITE
    private var shortLineColor: Int = Color.WHITE
    private var longLineColor: Int = Color.WHITE
    private var positionColor: Int = Color.WHITE
    private var centerAzimuthColor = Color.WHITE
    private lateinit var context: Context
    private var textSize: Float = 0f
    private var shortLineSize = 0f
    private var longLineSize = 0f
    private var positionSize = 0f
    private var markerSize = 0f
    private var backgroundColor = Color.BLACK
    private var lastDrawTime = 0L
    private var step = 1000 / 10
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var curBitmap: Bitmap? = null

    constructor(context: Context) : this(context, null) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        initView()
    }

    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        ctx,
        attrs,
        defStyleAttr,
    ) {
        this.context = ctx
        textSize = 13f.spToPx(context).toFloat()
        shortLineSize = 0.5f.spToPx(context).toFloat()
        longLineSize = 0.5f.spToPx(context).toFloat()
        positionSize = 11f.spToPx(context).toFloat()
        markerSize = 2f.spToPx(context).toFloat()
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.LinearCompassView, 0, 0)
        lineColor = attributes.getColor(R.styleable.LinearCompassView_lineColor, Color.WHITE)
        textColor = attributes.getColor(R.styleable.LinearCompassView_textColor, Color.WHITE)
        backgroundColor =
            attributes.getColor(R.styleable.LinearCompassView_backgroundColor, Color.BLACK)
        shortLineColor =
            attributes.getColor(R.styleable.LinearCompassView_shortLineColor, Color.WHITE)
        longLineColor =
            attributes.getColor(R.styleable.LinearCompassView_longLineColor, Color.WHITE)
        positionColor =
            attributes.getColor(R.styleable.LinearCompassView_positionColor, Color.WHITE)
        centerAzimuthColor =
            attributes.getColor(R.styleable.LinearCompassView_compassMarkerColor, Color.WHITE)
        shortLineSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_shortLineSize,
                shortLineSize,
            )
        longLineSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_longLineSize,
                longLineSize,
            )
        positionSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_positionSize,
                positionSize,
            )
        markerSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_markerSize,
                markerSize,
            )
        attributes.recycle()
        initView()
    }

    private fun initView() {
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.isAntiAlias = true
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.isAntiAlias = true
        textPaint.strokeWidth = 1f
        markerPaint.color = centerAzimuthColor
        markerPaint.strokeWidth = markerSize
        markerPaint.style = Paint.Style.FILL_AND_STROKE
        markerPaint.isAntiAlias = true
        shortLinePaint.color = shortLineColor
        shortLinePaint.strokeWidth = shortLineSize
        shortLinePaint.style = Paint.Style.STROKE
        shortLinePaint.isAntiAlias = true
        longLinePaint.color = longLineColor
        longLinePaint.strokeWidth = longLineSize
        longLinePaint.style = Paint.Style.STROKE
        longLinePaint.isAntiAlias = true
        positionPaint.color = positionColor
        positionPaint.textSize = positionSize
        positionPaint.style = Paint.Style.FILL_AND_STROKE
        positionPaint.isAntiAlias = true
        positionPaint.strokeWidth = 1f
    }

    private var showAzimuthArrow = true
    private var azimuth = 0f
    private var range = 180f
    private var text: String = ""
    private fun getRawMinimum() = azimuth - range / 2
    private fun getRawMaximum() = azimuth + range / 2
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        this.canvas = canvas
        drawAzimuthArrow()
        drawCompassLine()
    }

    private fun drawBackGround() {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawAzimuthArrow() {
        if (!showAzimuthArrow) {
            return
        }
        val endWidth = width / 2f
        val endHeight = (3 / 10f) * height
        canvas.drawText(
            text,
            realX(text, endWidth, textPaint),
            realY(text, endHeight, textPaint),
            textPaint
        )
    }

    private fun drawCompassLine() {
        drawCompass()
        val bottomHeight = height * 7 / 10f
        canvas.drawLine(0f, (bottomHeight - 1), width.toFloat(), bottomHeight, shortLinePaint)
        canvas.drawLine(
            width / 2f + markerSize / 2,
            height * (3 / 10f),
            width / 2f + markerSize / 2,
            height * (7 / 10f),
            markerPaint,
        )
    }

    fun setCurAzimuth(azimuth: Int) {
        scope.launch(Dispatchers.IO) {
            this@LinearCompassView.azimuth = azimuth.toFloat()
            this@LinearCompassView.text = azimuth.toString()
            var curTime = System.currentTimeMillis()
            if (curTime - lastDrawTime > step) {
                lastDrawTime = curTime
                launch(Dispatchers.Main) {
                    curBitmap = this@LinearCompassView.drawToBitmap()
                    invalidate()
                }
            }
        }
    }

    private fun drawCompass() {
        getValuesBetween(getRawMinimum(), getRawMaximum(), 5f).map {
            it.toInt()
        }.toMutableList().forEach {
            val x = toPixel(it.toFloat())
            val lineHeight =
                when {
                    it % 90 == 0 -> (3 / 10f) * height
                    it % 15 == 0 -> (4 / 10f) * height
                    else -> (5 / 10f) * height
                }
            val bottomHeight = height * 7 / 10f
            when {
                it % 90 == 0 -> canvas.drawLine(x, lineHeight, x, bottomHeight, longLinePaint)
                else -> canvas.drawLine(x, lineHeight, x, bottomHeight, shortLinePaint)
            }
            if (it % 45 == 0) {
                val coord = getPositionText(it)
                canvas.drawText(
                    coord,
                    realX(coord, x, positionPaint),
                    realY(coord, height - 2f, positionPaint),
                    positionPaint
                )
            }
        }
    }

    private fun getPositionText(position: Int): String =
        when (position) {
            -90, 270 -> resources.getString(R.string.compass_west)
            -45, 315 -> resources.getString(R.string.compass_northwest)
            0, 360 -> resources.getString(R.string.compass_north)
            45, 405 -> resources.getString(R.string.compass_northeast)
            90, 450 -> resources.getString(R.string.compass_east)
            135, 495 -> resources.getString(R.string.compass_southeast)
            -180, 180 -> resources.getString(R.string.compass_south)
            -135, 225 -> resources.getString(R.string.compass_southwest)
            else -> ""
        }

    private fun toPixel(bearing: Float): Float {
        return getPixelLinear(
            bearing,
            azimuth,
            width.toFloat(),
            range,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Recreate the scope if it was cancelled
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\MagQualityCompassWrapper.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import kotlin.math.min

class MagQualityCompassWrapper(private val compass: ICompass, private val magnetometer: ISensor) :
    AbstractSensor(), ICompass {
    override val bearing: Bearing
        get() = compass.bearing
    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }
    override val hasValidReading: Boolean
        get() = compass.hasValidReading
    override val rawBearing: Float
        get() = compass.rawBearing
    override val quality: Quality
        get() = Quality.values()[min(magnetometer.quality.ordinal, compass.quality.ordinal)]

    override fun startImpl() {
        compass.start(this::onReading)
        magnetometer.start(this::onReading)
    }

    override fun stopImpl() {
        compass.stop(this::onReading)
        magnetometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullCompass.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection

class NullCompass : NullSensor(), ICompass {
    override val bearing: Bearing = Bearing.from(CompassDirection.North)
    override var declination: Float = 0f
    override val rawBearing: Float = 0f
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullSensor.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.CoroutineTimer

abstract class NullSensor(private val interval: Long = 0) : AbstractSensor() {
    override val hasValidReading: Boolean = true
    private val timer =
        CoroutineTimer {
            notifyListeners()
        }

    override fun startImpl() {
        if (interval == 0L) {
            timer.once(0L)
        } else {
            timer.interval(interval)
        }
    }

    override fun stopImpl() {
        timer.stop()
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\SensorService.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.ICompass

class SensorService(ctx: Context) {
    private var context = ctx.applicationContext
    fun hasCompass(): Boolean {
        return Sensors.hasCompass(context)
    }

    fun getCompass(): ICompass {
        return CompassProvider(context).get()
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }
}