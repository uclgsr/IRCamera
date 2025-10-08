// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu' directory and its subdirectories.
// Total files: 4 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\libunified_src_main_java_com_mpdc4gsr_libunified_app_menu_constant_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant' subtree
// Files: 6; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\FenceType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class FenceType {
    POINT,
    LINE,
    RECT,
    FULL,
    TREND,
    DEL,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\MenuType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class MenuType {
    SINGLE_LIGHT,
    DOUBLE_LIGHT,
    Lite,
    TC007,
    GALLERY_EDIT,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\SettingType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class SettingType {
    PSEUDO_BAR,
    CONTRAST,
    DETAIL,
    ROTATE,
    MIRROR,
    ALARM,
    FONT,
    COMPASS,
    WATERMARK,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TargetType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TargetType {
    MODE,
    STYLE,
    COLOR,
    DELETE,
    HELP,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TempPointType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TempPointType {
    HIGH,
    LOW,
    DELETE,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TwoLightType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TwoLightType {
    TWO_LIGHT_1,
    TWO_LIGHT_2,
    IR,
    LIGHT,
    CORRECT,
    P_IN_P,
    BLEND_EXTENT,
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\libunified_src_main_java_com_mpdc4gsr_libunified_app_menu_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\menu' subtree
// Files: 8; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\FenceType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class FenceType {
    POINT,
    LINE,
    RECT,
    FULL,
    TREND,
    DEL,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\MenuType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class MenuType {
    SINGLE_LIGHT,
    DOUBLE_LIGHT,
    Lite,
    TC007,
    GALLERY_EDIT,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\SettingType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class SettingType {
    PSEUDO_BAR,
    CONTRAST,
    DETAIL,
    ROTATE,
    MIRROR,
    ALARM,
    FONT,
    COMPASS,
    WATERMARK,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TargetType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TargetType {
    MODE,
    STYLE,
    COLOR,
    DELETE,
    HELP,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TempPointType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TempPointType {
    HIGH,
    LOW,
    DELETE,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\constant\TwoLightType.kt =====

package com.mpdc4gsr.libunified.app.menu.constant

enum class TwoLightType {
    TWO_LIGHT_1,
    TWO_LIGHT_2,
    IR,
    LIGHT,
    CORRECT,
    P_IN_P,
    BLEND_EXTENT,
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\util\PseudoColorConfig.kt =====

package com.mpdc4gsr.libunified.app.menu.util

object PseudoColorConfig {
    @JvmStatic
    fun getColors(code: Int): IntArray =
        when (code) {
            1 -> intArrayOf(0xffffffff.toInt(), 0xff000000.toInt())
            3 -> intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
            4 -> intArrayOf(
                0xffe7321d.toInt(),
                0xfffdee38.toInt(),
                0xff58e531.toInt(),
                0xff0003c8.toInt(),
                0xff01000e.toInt()
            )

            5 ->
                intArrayOf(
                    0xffe7321d.toInt(),
                    0xfffdee38.toInt(),
                    0xff65fa33.toInt(),
                    0xff5aeefd.toInt(),
                    0xff0d06d2.toInt(),
                    0xff701b71.toInt(),
                )

            6 ->
                intArrayOf(
                    0xfffce7e5.toInt(),
                    0xffec361e.toInt(),
                    0xfffdf339.toInt(),
                    0xff67f933.toInt(),
                    0xff2009f8.toInt(),
                    0xff3e0d8d.toInt(),
                    0xff060011.toInt(),
                )

            7 -> intArrayOf(0xffe83120.toInt(), 0xffc2c2c2.toInt(), 0xff010101.toInt())
            8 -> intArrayOf(
                0xffec391f.toInt(),
                0xfffffe3b.toInt(),
                0xff375e5e.toInt(),
                0xff000000.toInt()
            )

            9 ->
                intArrayOf(
                    0xfffdf3fe.toInt(),
                    0xfff081f7.toInt(),
                    0xffe2311c.toInt(),
                    0xfff8d333.toInt(),
                    0xff67fa43.toInt(),
                    0xff00066b.toInt(),
                    0xff000006.toInt(),
                )

            10 ->
                intArrayOf(
                    0xfffffff7.toInt(),
                    0xfffeff50.toInt(),
                    0xffe63023.toInt(),
                    0xffe331e6.toInt(),
                    0xff56d1fa.toInt(),
                    0xff5ffa3c.toInt(),
                    0xff0006d8.toInt(),
                    0xff000012.toInt(),
                )

            11 -> intArrayOf(0xff000000.toInt(), 0xffffffff.toInt())
            else -> intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
        }

    @JvmStatic
    fun getPositions(code: Int): FloatArray =
        when (code) {
            1 -> floatArrayOf(0f, 1f)
            3 -> floatArrayOf(0f, 0.5f, 1f)
            4 -> floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            5 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
            6 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            7 -> floatArrayOf(0f, 0.5f, 1f)
            8 -> floatArrayOf(0f, 0.33f, 0.66f, 1f)
            9 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            10 -> floatArrayOf(0f, 0.1f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            11 -> floatArrayOf(0f, 1f)
            else -> floatArrayOf(0f, 0.5f, 1f)
        }

    @JvmStatic
    fun getSeekBarColors(): IntArray = intArrayOf(0xffdddddd.toInt(), 0xff333333.toInt())

    @JvmStatic
    fun getSeekBarAlpha(): FloatArray = floatArrayOf(0f, 1f)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view\ColorView.kt =====

package com.mpdc4gsr.libunified.app.menu.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R

class ColorView : View {
    var colors: IntArray = intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
    var positions: FloatArray = floatArrayOf(0f, 0.5f, 1f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shaderSelectYes =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private var shaderSelectNot =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private val triangleDrawable: Drawable

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        paint.color = 0xffffffff.toInt()
        triangleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_color_select_svg)!!
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int = if (widthMode == MeasureSpec.UNSPECIFIED) 100 else widthSize
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val triangleSize: Int =
            (width * 12f / 62).toInt() // 62 and 12 from UI design - triangle width 12, total width 62
        val margin: Int = 4f.dpToPx(context).toInt() // 4dp spacing between color block and triangle
        val wantHeight: Int = barHeight + margin + triangleSize
        val height =
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                else -> wantHeight
            }
        setMeasuredDimension(width, height)
        refreshShader()
        triangleDrawable.setBounds(
            (width - triangleSize) / 2,
            barHeight + margin,
            (width - triangleSize) / 2 + triangleSize,
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius: Float = 10f.dpToPx(context).toFloat()
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        if (isSelected) {
            val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
            val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
            paint.shader = null
            canvas.drawRoundRect(
                0f,
                0f,
                width.toFloat(),
                barHeight.toFloat(),
                radius,
                radius,
                paint
            )
            paint.shader = shaderSelectYes
            canvas.drawRoundRect(
                strokeSize,
                strokeSize,
                width - strokeSize,
                strokeSize + selectBarHeight,
                radius,
                radius,
                paint
            )
            triangleDrawable.draw(canvas)
        } else {
            val normalBarWidth: Int =
                (width * 50f / 62).toInt() // Unselected width 50, total width 62
            val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
            val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
            val left: Float = ((width - normalBarWidth) / 2).toFloat()
            paint.shader = shaderSelectNot
            canvas.drawRoundRect(
                left,
                top,
                width - left,
                top + normalBarHeight,
                radius,
                radius,
                paint
            )
        }
    }

    fun refreshColor(
        colors: IntArray,
        positions: FloatArray,
    ) {
        this.colors = colors
        this.positions = positions
        refreshShader()
        invalidate()
    }

    private fun refreshShader() {
        val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
        val barHeight: Int =
            (measuredWidth * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
        shaderSelectYes = LinearGradient(
            0f,
            strokeSize,
            0f,
            strokeSize + selectBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        val normalBarWidth: Int =
            (measuredWidth * 50f / 62).toInt() // Unselected width 50, total width 62
        val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
        val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
        shaderSelectNot = LinearGradient(
            0f,
            top,
            0f,
            top + normalBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\util\libunified_src_main_java_com_mpdc4gsr_libunified_app_menu_util_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\util' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\util\PseudoColorConfig.kt =====

package com.mpdc4gsr.libunified.app.menu.util

object PseudoColorConfig {
    @JvmStatic
    fun getColors(code: Int): IntArray =
        when (code) {
            1 -> intArrayOf(0xffffffff.toInt(), 0xff000000.toInt())
            3 -> intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
            4 -> intArrayOf(
                0xffe7321d.toInt(),
                0xfffdee38.toInt(),
                0xff58e531.toInt(),
                0xff0003c8.toInt(),
                0xff01000e.toInt()
            )

            5 ->
                intArrayOf(
                    0xffe7321d.toInt(),
                    0xfffdee38.toInt(),
                    0xff65fa33.toInt(),
                    0xff5aeefd.toInt(),
                    0xff0d06d2.toInt(),
                    0xff701b71.toInt(),
                )

            6 ->
                intArrayOf(
                    0xfffce7e5.toInt(),
                    0xffec361e.toInt(),
                    0xfffdf339.toInt(),
                    0xff67f933.toInt(),
                    0xff2009f8.toInt(),
                    0xff3e0d8d.toInt(),
                    0xff060011.toInt(),
                )

            7 -> intArrayOf(0xffe83120.toInt(), 0xffc2c2c2.toInt(), 0xff010101.toInt())
            8 -> intArrayOf(
                0xffec391f.toInt(),
                0xfffffe3b.toInt(),
                0xff375e5e.toInt(),
                0xff000000.toInt()
            )

            9 ->
                intArrayOf(
                    0xfffdf3fe.toInt(),
                    0xfff081f7.toInt(),
                    0xffe2311c.toInt(),
                    0xfff8d333.toInt(),
                    0xff67fa43.toInt(),
                    0xff00066b.toInt(),
                    0xff000006.toInt(),
                )

            10 ->
                intArrayOf(
                    0xfffffff7.toInt(),
                    0xfffeff50.toInt(),
                    0xffe63023.toInt(),
                    0xffe331e6.toInt(),
                    0xff56d1fa.toInt(),
                    0xff5ffa3c.toInt(),
                    0xff0006d8.toInt(),
                    0xff000012.toInt(),
                )

            11 -> intArrayOf(0xff000000.toInt(), 0xffffffff.toInt())
            else -> intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
        }

    @JvmStatic
    fun getPositions(code: Int): FloatArray =
        when (code) {
            1 -> floatArrayOf(0f, 1f)
            3 -> floatArrayOf(0f, 0.5f, 1f)
            4 -> floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            5 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
            6 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            7 -> floatArrayOf(0f, 0.5f, 1f)
            8 -> floatArrayOf(0f, 0.33f, 0.66f, 1f)
            9 -> floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            10 -> floatArrayOf(0f, 0.1f, 0.2f, 0.4f, 0.6f, 0.8f, 0.9f, 1f)
            11 -> floatArrayOf(0f, 1f)
            else -> floatArrayOf(0f, 0.5f, 1f)
        }

    @JvmStatic
    fun getSeekBarColors(): IntArray = intArrayOf(0xffdddddd.toInt(), 0xff333333.toInt())

    @JvmStatic
    fun getSeekBarAlpha(): FloatArray = floatArrayOf(0f, 1f)
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view\libunified_src_main_java_com_mpdc4gsr_libunified_app_menu_view_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view\ColorView.kt =====

package com.mpdc4gsr.libunified.app.menu.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R

class ColorView : View {
    var colors: IntArray = intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
    var positions: FloatArray = floatArrayOf(0f, 0.5f, 1f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shaderSelectYes =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private var shaderSelectNot =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private val triangleDrawable: Drawable

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        paint.color = 0xffffffff.toInt()
        triangleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_color_select_svg)!!
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int = if (widthMode == MeasureSpec.UNSPECIFIED) 100 else widthSize
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val triangleSize: Int =
            (width * 12f / 62).toInt() // 62 and 12 from UI design - triangle width 12, total width 62
        val margin: Int = 4f.dpToPx(context).toInt() // 4dp spacing between color block and triangle
        val wantHeight: Int = barHeight + margin + triangleSize
        val height =
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                else -> wantHeight
            }
        setMeasuredDimension(width, height)
        refreshShader()
        triangleDrawable.setBounds(
            (width - triangleSize) / 2,
            barHeight + margin,
            (width - triangleSize) / 2 + triangleSize,
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius: Float = 10f.dpToPx(context).toFloat()
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        if (isSelected) {
            val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
            val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
            paint.shader = null
            canvas.drawRoundRect(
                0f,
                0f,
                width.toFloat(),
                barHeight.toFloat(),
                radius,
                radius,
                paint
            )
            paint.shader = shaderSelectYes
            canvas.drawRoundRect(
                strokeSize,
                strokeSize,
                width - strokeSize,
                strokeSize + selectBarHeight,
                radius,
                radius,
                paint
            )
            triangleDrawable.draw(canvas)
        } else {
            val normalBarWidth: Int =
                (width * 50f / 62).toInt() // Unselected width 50, total width 62
            val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
            val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
            val left: Float = ((width - normalBarWidth) / 2).toFloat()
            paint.shader = shaderSelectNot
            canvas.drawRoundRect(
                left,
                top,
                width - left,
                top + normalBarHeight,
                radius,
                radius,
                paint
            )
        }
    }

    fun refreshColor(
        colors: IntArray,
        positions: FloatArray,
    ) {
        this.colors = colors
        this.positions = positions
        refreshShader()
        invalidate()
    }

    private fun refreshShader() {
        val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
        val barHeight: Int =
            (measuredWidth * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
        shaderSelectYes = LinearGradient(
            0f,
            strokeSize,
            0f,
            strokeSize + selectBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        val normalBarWidth: Int =
            (measuredWidth * 50f / 62).toInt() // Unselected width 50, total width 62
        val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
        val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
        shaderSelectNot = LinearGradient(
            0f,
            top,
            0f,
            top + normalBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
    }
}