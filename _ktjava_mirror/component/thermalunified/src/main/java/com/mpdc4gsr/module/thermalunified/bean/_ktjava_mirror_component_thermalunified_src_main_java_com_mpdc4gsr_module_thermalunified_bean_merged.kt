// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\bean' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:42


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\bean\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_bean_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\bean' subtree
// Files: 2; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\bean\ModelBean.kt =====

package com.mpdc4gsr.module.thermalunified.bean

data class ModelBean(
    var defaultModel: DataBean,
    var myselfModel: ArrayList<DataBean> = arrayListOf(),
)

data class DataBean(
    var id: Int = 1,
    var name: String = "1",
    var environment: Float = 30.0f,
    var distance: Float = 0.25f,
    var radiation: Float = 0.95f,
    var use: Boolean = false,
    // Additional thermal configuration properties
    var emissivity: Float = 0.95f,
    var ambientTemperature: Float = 30.0f,
    var humidity: Float = 0.6f,
    var atmosphericTemperature: Float = 30.0f,
    var transmittance: Float = 0.8f,
)


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\bean\SelectPositionBean.kt =====

package com.mpdc4gsr.module.thermalunified.bean

import android.graphics.Point
import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectPositionBean(
    val type: Int = 0,
    val startPosition: Point = Point(),
    val endPosition: Point = Point(),
) : Parcelable {
    constructor(rect: Rect) : this(3, Point(rect.left, rect.top), Point(rect.right, rect.bottom))

    fun getRect(): Rect = Rect(startPosition.x, startPosition.y, endPosition.x, endPosition.y)
}