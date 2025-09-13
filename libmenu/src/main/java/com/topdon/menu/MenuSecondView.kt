package com.topdon.menu

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.repository.GalleryRepository
import com.topdon.menu.adapter.ColorAdapter
import com.topdon.menu.adapter.FenceAdapter
import com.topdon.menu.adapter.SettingAdapter
import com.topdon.menu.adapter.TargetAdapter
import com.topdon.menu.adapter.TempLevelAdapter
import com.topdon.menu.adapter.TempPointAdapter
import com.topdon.menu.adapter.TempSourceAdapter
import com.topdon.menu.adapter.TwoLightAdapter
import com.topdon.menu.constant.FenceType
import com.topdon.menu.constant.MenuType
import com.topdon.menu.constant.SettingType
import com.topdon.menu.constant.TargetType
import com.topdon.menu.constant.TempPointType
import com.topdon.menu.constant.TwoLightType
import com.topdon.menu.databinding.ViewMenuSecondBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Secondary menu component for thermal imaging interface.
 * Handles advanced menu operations and user interactions. */
@SuppressLint("NotifyDataSetChanged")
/**
 * MenuSecondView implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0 */
class MenuSecondView : FrameLayout {
    /**
 * Menu type configuration to handle differences between device variations
     * (single light, dual light, Lite, TC007, 2D editing). Uses enum for distinction. */
    private val menuType: MenuType

    // View binding for improved type safety and performance
    private lateinit var binding: ViewMenuSecondBinding

    // *********************************************  Public Methods  *********************************************

    /**
 * Shows the corresponding menu based on the menu code.
     * Temperature measurement: 0-> Capture      Observation 10->Capture
     *
     * Temperature measurement: 1-> Point/Line/Area
     *
     *                    Observation 11->AI Recognition
     *
     * Temperature measurement: 2-> Dual Light
     *                    Observation 13->Target
     *
     * Temperature measurement: 3-> Pseudo Color   Observation 12->Pseudo Color
     *
     * Temperature measurement: 4-> Settings
     *
     *                    Observation 15->Settings
     *
     * Temperature measurement: 5-> Temperature Level
     *
     *                    Observation 14->High/Low Temperature Points */
    fun selectPosition(position: Int) {
        binding.cameraMenuView.isVisible = position == 0 || position == 10
        binding.recyclerFence.isVisible = position == 1
        binding.recyclerTwoLight.isVisible = position == 2
        binding.recyclerColor.isVisible = position == 3 || position == 12
        binding.recyclerSettingTe.isVisible = position == 4
        binding.recyclerTempLevel.isVisible = position == 5

        binding.recyclerTempSource.isVisible = position == 11
        binding.recyclerTarget.isVisible = position == 13
        binding.recyclerTempPoint.isVisible = position == 14
        binding.recyclerSettingOb.isVisible = position == 15
    }

    // *********************************************  Public Properties  *********************************************

    /**
 * Menu 1 - Capture/Recording operations click event listener.
     * actionCode: 0-Capture/Record  1-Gallery  2-More menu  3-Switch to capture  4-Switch to recording */
    var onCameraClickListener: ((actionCode: Int) -> Unit)?
        get() = binding.cameraMenuView.onCameraClickListener
        set(value) {
            binding.cameraMenuView.onCameraClickListener = value
        }

    /**
 * Temperature measurement mode - Menu 2 - Point/Line/Area switch event listener. */
    var onFenceListener: ((fenceType: FenceType, isSelected: Boolean) -> Unit)?
        get() = fenceAdapter.onFenceListener
        set(value) {
            fenceAdapter.onFenceListener = value
        }

    /**
 * Temperature measurement mode - Menu 3 - Dual Light click event listener.
     * isSelected: true-switch to selected false-switch to unselected */
    var onTwoLightListener: ((twoLightType: TwoLightType, isSelected: Boolean) -> Unit)?
        get() = twoLightAdapter.onTwoLightListener
        set(value) {
            twoLightAdapter.onTwoLightListener = value
        }

    /**
 * Temperature measurement mode - Menu 4 - Pseudo Color / Observation mode - Menu 3 - Pseudo Color switch event listener.
     * index - Selected pseudo color index in list (used by TC007)
     * code - Pseudo color encoding (legacy format, doesn't match index, used by non-TC007 devices)
     * size - Number of preset pseudo colors (used by TC007) */
    var onColorListener: ((index: Int, code: Int, size: Int) -> Unit)?
        get() = colorAdapter.onColorListener
        set(value) {
            colorAdapter.onColorListener = value
        }

    /**
 * Temperature measurement mode - Menu 5 - Settings / Observation mode - Menu 6 - Settings click event listener.
     * isSelected: true-selected state when clicked false-unselected state when clicked
     * Warning, font, watermark are considered highlighted and selected only when effective. 
     * Here we maintain the original code logic, leaving the selection refresh of the settings 
     * menu to the upper-layer listener, will consider changes later when time permits. */
    var onSettingListener: ((type: SettingType, isSelected: Boolean) -> Unit)?
        get() = settingTeAdapter.onSettingListener
        set(value) {
            settingTeAdapter.onSettingListener = value
            settingObAdapter.onSettingListener = value
        }

    /**
 * Temperature measurement mode - Menu 6 - High/Low temperature level click event listener.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Auto switch: -1
     * - High temperature (low gain): 0
     * - Normal temperature (high gain): 1 */
    var onTempLevelListener: ((code: Int) -> Unit)?
        get() = tempLevelAdapter.onTempLevelListener
        set(value) {
            tempLevelAdapter.onTempLevelListener = value
        }

    /**
 * Observation mode - Menu 2 - High/Low temperature source click event listener.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Nothing selected: -1
     * - Dynamic recognition: 0
     * - High temperature source: 1
     * - Low temperature source: 2 */
    var onTempSourceListener: ((code: Int) -> Unit)?
        get() = tempSourceAdapter.onTempSourceListener
        set(value) {
            tempSourceAdapter.onTempSourceListener = value
        }

    /**
 * Observation mode - Menu 4 - Target click event listener. */
    var onTargetListener: ((targetType: TargetType) -> Unit)?
        get() = targetAdapter.onTargetListener
        set(value) {
            targetAdapter.onTargetListener = value
        }

    /**
 * Observation mode - Menu 5 - High/Low temperature points click event listener. */
    var onTempPointListener: ((type: TempPointType, isSelected: Boolean) -> Unit)?
        get() = tempPointAdapter.onTempPointListener
        set(value) {
            tempPointAdapter.onTempPointListener = value
        }

    /**
 * Adapter used for Temperature measurement mode - Menu 2 - Point/Line/Area. */
    private val fenceAdapter: FenceAdapter

    /**
 * Adapter used for Temperature measurement mode - Menu 3 - Dual Light. */
    private val twoLightAdapter: TwoLightAdapter

    /**
 * Adapter used for Temperature measurement mode - Menu 4 - Pseudo Color or Observation mode - Menu 3 - Pseudo Color. */
    private val colorAdapter = ColorAdapter()

    /**
 * Adapter used for Temperature measurement mode - Menu 5 - Settings. */
    private val settingTeAdapter: SettingAdapter

    /**
 * Adapter used for Temperature measurement mode - Menu 6 - High/Low temperature level. */
    private val tempLevelAdapter: TempLevelAdapter

    /**
 * Adapter used for Observation mode - Menu 2 - High/Low temperature source. */
    private val tempSourceAdapter = TempSourceAdapter()

    /**
 * Adapter used for Observation mode - Menu 4 - Target. */
    private val targetAdapter = TargetAdapter()

    /**
 * Adapter used for Observation mode - Menu 5 - High/Low temperature points. */
    private val tempPointAdapter = TempPointAdapter()

    /**
 * Adapter used for Observation mode - Menu 6 - Settings. */
    private val settingObAdapter = SettingAdapter(isObserver = true)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.MenuSecondView, defStyleAttr, defStyleRes)
        menuType =
            when (typedArray.getInt(R.styleable.MenuSecondView_deviceType, 0)) {
                0 -> MenuType.SINGLE_LIGHT
                1 -> MenuType.DOUBLE_LIGHT
                2 -> MenuType.Lite
                4 -> MenuType.GALLERY_EDIT
                else -> MenuType.TC007
            }
        typedArray.recycle()

        if (isInEditMode) {
            binding = ViewMenuSecondBinding.inflate(LayoutInflater.from(context), this, true)
            fenceAdapter = FenceAdapter(menuType)
            twoLightAdapter = TwoLightAdapter(menuType)
            settingTeAdapter = SettingAdapter(menuType)
            tempLevelAdapter = TempLevelAdapter(menuType)
        } else {
            // Initialize view binding - replaces findViewById calls
            binding = ViewMenuSecondBinding.inflate(LayoutInflater.from(context), this, true)

            refreshImg(GalleryRepository.DirType.LINE)

            // Initialize Temperature measurement mode - Menu 2 - Point/Line/Area menu
            fenceAdapter = FenceAdapter(menuType)
            binding.recyclerFence.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerFence.adapter = fenceAdapter

            // Initialize Temperature measurement mode - Menu 3 - Dual Light menu
            twoLightAdapter = TwoLightAdapter(menuType)
            binding.recyclerTwoLight.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerTwoLight.adapter = twoLightAdapter

            // Initialize Temperature measurement mode - Menu 4 - Pseudo Color or Observation mode - Menu 3 - Pseudo Color menu
            binding.recyclerColor.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerColor.adapter = colorAdapter

            // Initialize Temperature measurement mode - Menu 5 - Settings menu
            settingTeAdapter = SettingAdapter(menuType)
            binding.recyclerSettingTe.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerSettingTe.adapter = settingTeAdapter

            // Initialize Temperature measurement mode - Menu 6 - High/Low temperature level menu
            tempLevelAdapter = TempLevelAdapter(menuType)
            binding.recyclerTempLevel.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerTempLevel.adapter = tempLevelAdapter

            // Initialize Observation mode - Menu 2 - High/Low temperature source menu
            binding.recyclerTempSource.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerTempSource.adapter = tempSourceAdapter

            // Initialize Observation mode - Menu 4 - Target menu
            binding.recyclerTarget.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerTarget.adapter = targetAdapter

            // Initialize Observation mode - Menu 5 - High/Low temperature points menu
            binding.recyclerTempPoint.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerTempPoint.adapter = tempPointAdapter

            // Initialize Observation mode - Menu 6 - Settings menu
            binding.recyclerSettingOb.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            binding.recyclerSettingOb.adapter = settingObAdapter
        }
    }

    // *********************************************  Menu 1 - Capture/Recording  *********************************************

    /**
 * Whether currently in recording mode.
     *
     * true-recording mode false-capture mode */
    var isVideoMode: Boolean
        get() = binding.cameraMenuView.isVideoMode
        set(value) {
            binding.cameraMenuView.isVideoMode = value
        }

    /**
 * For TS001 only, when switching between temperature measurement/observation modes, need to reset to capture state after closing delayed capture, continuous capture, or recording. */
    fun switchToCamera() {
        binding.cameraMenuView.canSwitchMode = true
        binding.cameraMenuView.isVideoMode = false
        binding.cameraMenuView.setToNormal()
    }

    /**
 * Similar to reset, this method aims to reset state to non-capture, non-recording state and enable capture/recording switching.
     * Called in the start() method of various thermal imaging Activities and in the current View */
    fun updateCameraModel() {
        binding.cameraMenuView.canSwitchMode = true
        binding.cameraMenuView.setToNormal()
    }

    /**
 * Executes refreshimg functionality. */
    fun refreshImg(type: GalleryRepository.DirType = GalleryRepository.DirType.LINE) {
        updateCameraModel() // Restore state
        CoroutineScope(Dispatchers.IO).launch {
            val path = GalleryRepository.readLatest(type)
            launch(Dispatchers.Main) {
                binding.cameraMenuView.refreshGallery(path)
            }
        }
    }

    /**
 * Set the middle capture/recording button to capturing-immediate/capturing-delayed/recording state */
    fun setToRecord(isDelay: Boolean) {
        binding.cameraMenuView.canSwitchMode = false
        binding.cameraMenuView.setToRecord(isDelay)
    }

    /**
 * Set the middle capture/recording button to capturing-immediate state */
    fun setToCamera() {
        binding.cameraMenuView.setToRecord(false)
    }

    // *****************************************  temperature measurementmode-menu2-point/line/area  *****************************************

    /**
 * Temperature measurement mode - Menu 2 - Point/Line/Area currently selected menu type, null indicates all unselected. */
    var fenceSelectType: FenceType?
        get() = fenceAdapter.selectType
        set(value) {
            fenceAdapter.selectType = value
        }

    // *****************************************  temperature measurementmode-menu3-dual light  *****************************************
    /**
 * Currently selected dual light type
     * - Single light: Should not use this property
     * - Lite: Should not use this property
     * - Dual light: Dual light 1, Dual light 2, Infrared, Visible light
     * - TC007: Dual light, Infrared, Visible light, Picture-in-picture */
    var twoLightType: TwoLightType
        get() = twoLightAdapter.twoLightType
        set(value) {
            twoLightAdapter.twoLightType = value
        }

    /**
 * Set dual light multi-selection state
     * - Single light: Picture-in-picture, Fusion level
     * - Lite: Picture-in-picture, Fusion level
     * - Dual light: Registration, Picture-in-picture, Fusion level
     * - TC007: Registration, Fusion level */
    fun setTwoLightSelected(
        twoLightType: TwoLightType,
        isSelected: Boolean,
    ) {
        twoLightAdapter.setSelected(twoLightType, isSelected)
    }

    // **********************************  temperature measurementmode-menu4-pseudo color/observationmode-menu3-pseudo color  **********************************

    /**
 * Select specified pseudo color in pseudo color menu based on pseudo color code. If unsupported code is passed, results in all unselected state.
     * @param code 1-White Hot 3-Iron Red 4-Rainbow 1 5-Rainbow 2 6-Rainbow 3 7-Red Hot 8-Hot Iron 9-Rainbow 4 10-Rainbow 5 11-Black Hot */
    fun setPseudoColor(code: Int) {
        colorAdapter.selectCode = code
    }

    // **********************************  temperature measurementmode-menu5-settings or observationmode-menu6-settings  **********************************

    /**
 * Set the selection state of specified option in settings menu */
    fun setSettingSelected(
        settingType: SettingType,
        isSelected: Boolean,
    ) {
        settingTeAdapter.setSelected(settingType, isSelected)
        settingObAdapter.setSelected(settingType, isSelected)
    }

    /**
 * Set rotation angle for rotation option in settings menu
     * @param rotateAngle Note! This value is the core rotation angle, not UI rotation angle */
    fun setSettingRotate(rotateAngle: Int) {
        settingTeAdapter.rotateAngle = rotateAngle
        settingObAdapter.rotateAngle = rotateAngle
    }

    // *****************************************  temperature measurement mode - menu 6 - high/low temperature level  *****************************************

    /**
 * Whether temperature level uses Fahrenheit as unit
     *
     * true-Fahrenheit false-Celsius */
    var isUnitF: Boolean
        get() = tempLevelAdapter.isUnitF
        set(value) {
            tempLevelAdapter.isUnitF = value
        }

    /**
 * Set Temperature measurement mode - Menu 6 - High/Low temperature level.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Auto switch: -1
     * - High temperature (low gain): 0
     * - Normal temperature (high gain): 1 */
    fun setTempLevel(code: Int) {
        tempLevelAdapter.selectCode = code
    }

    // *****************************************  observation mode - menu 2 - high/low temperature source  *****************************************

    /**
 * Set Observation mode - Menu 2 - High/Low temperature source selection.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Nothing selected: -1
     * - Dynamic recognition: 0
     * - High temperature source: 1
     * - Low temperature source: 2 */
    fun setTempSource(code: Int) {
        tempSourceAdapter.selectCode = code
    }

    // *****************************************  observationmode-menu4-target  *****************************************

    /**
 * Set selection state of specified option in Observation mode - Menu 4 - Target */
    fun setTargetSelected(
        targetType: TargetType,
        isSelected: Boolean,
    ) {
        targetAdapter.setSelected(targetType, isSelected)
    }

    /**
 * Set icon type for Observation mode - Menu 4 - Target - Measurement mode.
     *
     * Due to legacy constraints (saved in SharedPreferences), the code values are:
     * - Human: 10
     * - Sheep: 11
     * - Dog: 12
     * - Bird: 13 */
    fun setTargetMode(modeCode: Int) {
        targetAdapter.setTargetMode(modeCode)
    }

    // *****************************************  observation mode - menu 5 - high/low temperature point  *****************************************

    /**
 * Set selection state of high temperature point or low temperature point in Observation mode - Menu 5 - High/Low temperature points menu. */
    fun setTempPointSelect(
        tempPointType: TempPointType,
        isSelected: Boolean,
    ) {
        tempPointAdapter.setSelected(tempPointType, isSelected)
    }

    /**
 * Clear all selection states in Observation mode - Menu 5 - High/Low temperature points menu.
     * Maintain original logic here, consider whether to directly delete selected items later. */
    fun clearTempPointSelect() {
        tempPointAdapter.clearAllSelect()
    }
}
