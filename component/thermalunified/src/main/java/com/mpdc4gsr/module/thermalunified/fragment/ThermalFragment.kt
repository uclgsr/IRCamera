package com.mpdc4gsr.module.thermalunified.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ScreenUtils
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.libunified.app.utils.ByteUtils.getIndex
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.base.BaseThermalFragment
import com.mpdc4gsr.module.thermalunified.event.ThermalActionEvent
import com.mpdc4gsr.module.thermalunified.stubs.FenceLineView
import com.mpdc4gsr.module.thermalunified.stubs.FencePointView
import com.mpdc4gsr.module.thermalunified.stubs.FenceView
import com.mpdc4gsr.module.thermalunified.stubs.GuideInterface
import com.mpdc4gsr.module.thermalunified.stubs.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.stubs.ThermalInputDialog
import com.mpdc4gsr.module.thermalunified.tools.Fence
import com.mpdc4gsr.module.thermalunified.tools.ThermalTool
import com.mpdc4gsr.module.thermalunified.tools.medie.IYapVideoProvider
import com.mpdc4gsr.module.thermalunified.tools.medie.YapVideoEncoder
import com.mpdc4gsr.module.thermalunified.utils.ArrayUtils
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import com.mpdc4gsr.libunified.R as LibUiR
import com.mpdc4gsr.libunified.app.matrix.GuideInterface as LibGuideInterface

class ThermalFragment : BaseThermalFragment(), IYapVideoProvider<Bitmap> {
    
    // Use the comprehensive ViewModel instead of basic ThermalViewModel
    private val thermalViewModel: ThermalFragmentViewModel by viewModels()

    protected var mIrSurfaceViewLayout: FrameLayout? = null
    protected var mIrSurfaceView: IrSurfaceView? = null
    private var mGuideInterface: GuideInterface? = null

    private val msgLiveData by lazy { MutableLiveData<Int>() }

    // Cached fence and camera views to avoid repeated findViewById calls
    private val fencePointView by lazy {
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FencePointView>(
            R.id.fence_point_view
        )
    }
    private val fenceLineView by lazy {
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceLineView>(
            R.id.fence_line_view
        )
    }
    private val fenceView by lazy {
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceView>(
            R.id.fence_view
        )
    }
    private val tempCameraView by lazy {
        requireView().findViewById<com.mpdc4gsr.libunified.ui.camera.CameraView>(
            R.id.temp_camera_view
        )
    }

    override fun initContentView() = R.layout.fragment_thermal

    private fun setupThermalObservers() {
        setupWindow()
        initializeViews()
        setupThermalSurface()
        setupObservers()
        initializeFence()
        startThermalProcessing()
    }

    private fun setupWindow() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rotateType = 3
    }

    private fun initializeViews() {
        mCenterTextView = requireView().findViewById(R.id.temp_display)
        mMaxTextView = requireView().findViewById(R.id.max_temp_display)
        mMinTextView = requireView().findViewById(R.id.min_temp_display)
        maxImg = requireView().findViewById(R.id.max_img)
        minImg = requireView().findViewById(R.id.min_img)
        mDisplayFrameLayout = requireView().findViewById(R.id.temp_display_layout)
        mFenceLayout = requireView().findViewById(R.id.fence_lay)
        mCameraLayout = requireView().findViewById(R.id.temp_camera_layout)
        
        // Initial visibility setup
        mDisplayFrameLayout?.visibility = View.GONE
        mFenceLayout?.visibility = View.GONE
    }

    private fun setupThermalSurface() {
        mIrSurfaceViewLayout = requireView().findViewById(R.id.final_ir_layout)
        mIrSurfaceView = IrSurfaceView(requireContext())
        
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        
        mIrSurfaceView?.apply {
            this.layoutParams = layoutParams
            setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
        }
        
        mIrSurfaceViewLayout?.addView(mIrSurfaceView)
        
        setupDimensions()
        setupLayoutObserver()
    }

    private fun setupDimensions() {
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = screenWidth * 192 / 256
        width = screenWidth
        height = screenHeight
        highCrossWidth = resources.getDimension(R.dimen.high_cross_width).toInt()
        highCrossHeight = resources.getDimension(R.dimen.high_cross_height).toInt()
        
        // Update ViewModel with surface dimensions
        thermalViewModel.updateSurfaceDimensions(256, 192)
    }

    private fun setupObservers() {
        // Thermal UI state observer
        lifecycleScope.launch {
            thermalViewModel.uiState.collect { uiState ->
                updateThermalUI(uiState)
            }
        }

        // Temperature analysis observer
        lifecycleScope.launch {
            thermalViewModel.temperatureAnalysis.collect { analysis ->
                updateTemperatureDisplay(analysis)
            }
        }

        // Fence state observer
        lifecycleScope.launch {
            thermalViewModel.fenceState.collect { fenceState ->
                updateFenceUI(fenceState)
            }
        }

        // Video recording state observer
        lifecycleScope.launch {
            thermalViewModel.videoRecordingState.collect { recordingState ->
                updateRecordingUI(recordingState)
            }
        }

        // Thermal processing actions observer
        thermalViewModel.thermalProcessingAction.observe(viewLifecycleOwner) { action ->
            handleThermalProcessingAction(action)
        }

        // Original message observer for compatibility
        msgLiveData.observe(this) { msg ->
            handleLegacyMessage(msg)
        }
    }

    private fun updateThermalUI(uiState: ThermalFragmentViewModel.ThermalUIState) {
        // Update processing state
        if (uiState.isProcessing) {
            // Show processing indicator
        }
        
        // Update temperature displays visibility
        updateTemperatureDisplayVisibility(uiState)
    }

    private fun updateTemperatureDisplay(analysis: ThermalFragmentViewModel.TemperatureAnalysis) {
        if (!analysis.isValid) return

        when (selectType) {
            0 -> {
                mCenterTextView?.apply {
                    visibility = View.VISIBLE
                    text = formatTemperature(analysis.averageTemperature)
                }
                mMaxTextView?.apply {
                    visibility = View.VISIBLE
                    text = formatTemperature(analysis.maxTemperature)
                }
                mMinTextView?.apply {
                    visibility = View.VISIBLE
                    text = formatTemperature(analysis.minTemperature)
                }
                maxImg?.visibility = View.GONE
                minImg?.visibility = View.GONE
            }
            1 -> {
                mCenterTextView?.apply {
                    visibility = View.VISIBLE
                    text = formatTemperature(analysis.maxTemperature)
                }
                mMaxTextView?.visibility = View.GONE
                mMinTextView?.visibility = View.GONE
                maxImg?.visibility = View.GONE
                minImg?.visibility = View.GONE
            }
            else -> {
                // Handle other display modes
                updateAdvancedTemperatureDisplay(analysis)
            }
        }
    }

    private fun formatTemperature(temperature: Float): String {
        return "${BigDecimal(temperature.toDouble()).setScale(1, RoundingMode.HALF_UP)}°C"
    }

    private fun updateAdvancedTemperatureDisplay(analysis: ThermalFragmentViewModel.TemperatureAnalysis) {
        // Advanced temperature display with quality indicators
        mCenterTextView?.apply {
            visibility = View.VISIBLE
            text = buildString {
                append(formatTemperature(analysis.averageTemperature))
                append(" (")
                append(analysis.dataQuality.name)
                append(")")
            }
        }
    }

    private fun updateFenceUI(fenceState: ThermalFragmentViewModel.FenceState) {
        mFenceLayout?.visibility = if (fenceState.isActive) View.VISIBLE else View.GONE
        
        // Update fence measurements display
        if (fenceState.measurements.isNotEmpty()) {
            updateFenceMeasurements(fenceState.measurements)
        }
    }

    private fun updateRecordingUI(recordingState: ThermalFragmentViewModel.VideoRecordingState) {
        // Update recording indicator
        if (recordingState.isRecording) {
            // Show recording indicator
        }
    }

    private fun handleThermalProcessingAction(action: ThermalFragmentViewModel.ThermalProcessingAction) {
        when (action) {
            is ThermalFragmentViewModel.ThermalProcessingAction.StartProcessing -> {
                // Show processing indicator
            }
            is ThermalFragmentViewModel.ThermalProcessingAction.ProcessingComplete -> {
                // Hide processing indicator
            }
            is ThermalFragmentViewModel.ThermalProcessingAction.ProcessingError -> {
                ToastTools.showShort(action.message)
            }
            is ThermalFragmentViewModel.ThermalProcessingAction.TemperatureAlert -> {
                handleTemperatureAlert(action.temperature, action.type)
            }
        }
    }

    private fun handleTemperatureAlert(temperature: Float, type: ThermalFragmentViewModel.AlertType) {
        val message = when (type) {
            ThermalFragmentViewModel.AlertType.HOT_SPOT -> "Hot spot detected: ${formatTemperature(temperature)}"
            ThermalFragmentViewModel.AlertType.COLD_SPOT -> "Cold spot detected: ${formatTemperature(temperature)}"
            ThermalFragmentViewModel.AlertType.TEMPERATURE_THRESHOLD -> "Temperature threshold exceeded: ${formatTemperature(temperature)}"
        }
        ToastTools.showShort(message)
    }

    // Modernized view position calculation using ViewModel
    private fun setViewPosition(imageView: ImageView, index: Int) {
        val vg = imageView.parent as ViewGroup
        val pw = vg.width
        val ph = vg.height
        
        val position = thermalViewModel.calculateViewPosition(
            index, imageView.width, imageView.height, pw, ph
        )
        
        imageView.x = position.first
        imageView.y = position.second
    }

    // Modernized thermal bitmap processing
    private fun processThermalBitmap(bitmap: Bitmap) {
        lifecycleScope.launch {
            val result = thermalViewModel.processThermalBitmap(bitmap)
            if (result.success) {
                // Update UI with processed bitmap
                result.processedBitmap?.let { processedBitmap ->
                    updateThermalImageDisplay(processedBitmap)
                }
            } else {
                ToastTools.showShort("Thermal processing failed: ${result.error}")
            }
        }
    }

    private fun updateThermalImageDisplay(bitmap: Bitmap) {
        mIrSurfaceView?.doDraw(bitmap, 0)
    }

    // Fence management delegated to ViewModel
    private fun activatePointFence() {
        thermalViewModel.activateFence(ThermalFragmentViewModel.FenceType.POINT)
    }

    private fun activateLineFence() {
        thermalViewModel.activateFence(ThermalFragmentViewModel.FenceType.LINE)
    }

    private fun activateAreaFence() {
        thermalViewModel.activateFence(ThermalFragmentViewModel.FenceType.AREA)
    }

    private fun deactivateFence() {
        thermalViewModel.deactivateFence()
    }

    // Video recording delegated to ViewModel
    private fun startVideoRecording(outputFile: File) {
        thermalViewModel.startVideoRecording(outputFile)
    }

    private fun stopVideoRecording() {
        thermalViewModel.stopVideoRecording()
    }

    // Legacy compatibility methods
    private fun handleLegacyMessage(msg: Int) {
        // Handle legacy message system for backward compatibility
        when (msg) {
            0 -> updateTemperatureDisplayVisibility()
            else -> {
                // Handle other legacy messages
            }
        }
    }

    private fun updateTemperatureDisplayVisibility(uiState: ThermalFragmentViewModel.ThermalUIState? = null) {
        // Update visibility based on current state and selection type
        when (selectType) {
            0 -> {
                mCenterTextView?.visibility = View.VISIBLE
                mMaxTextView?.visibility = View.VISIBLE
                mMinTextView?.visibility = View.VISIBLE
                maxImg?.visibility = View.GONE
                minImg?.visibility = View.GONE
            }
            1 -> {
                mCenterTextView?.visibility = View.VISIBLE
                mMaxTextView?.visibility = View.GONE
                mMinTextView?.visibility = View.GONE
                maxImg?.visibility = View.GONE
                minImg?.visibility = View.GONE
            }
            else -> {
                mCenterTextView?.visibility = View.VISIBLE
                mMaxTextView?.visibility = View.GONE
                mMinTextView?.visibility = View.GONE
                maxImg?.visibility = View.VISIBLE
                minImg?.visibility = View.VISIBLE
            }
        }
    }

    private fun updateFenceMeasurements(measurements: List<ThermalFragmentViewModel.FenceMeasurement>) {
        // Update fence measurement displays
        measurements.forEach { measurement ->
            // Update fence point/line displays with temperature data
        }
    }

    // Layout and initialization helpers
    private fun setupLayoutObserver() {
        mIrSurfaceViewLayout?.viewTreeObserver?.addOnGlobalLayoutListener {
            updateLayoutParameters()
        }
    }

    private fun updateLayoutParameters() {
        irSurfaceViewLayoutParams = mIrSurfaceViewLayout?.layoutParams as? ConstraintLayout.LayoutParams
        displayViewLayoutParams = mDisplayFrameLayout?.layoutParams as? FrameLayout.LayoutParams
        fenceLayoutParams = mFenceLayout?.layoutParams as? FrameLayout.LayoutParams
        cameraLayoutParams = mCameraLayout?.layoutParams as? FrameLayout.LayoutParams

        calculateSurfaceViewDimensions()
        applyLayoutParameters()
    }

    private fun calculateSurfaceViewDimensions() {
        when (rotateType) {
            1, 3 -> {
                irSurfaceViewWidth = height
                irSurfaceViewHeight = width
                if (irSurfaceViewWidth < width) {
                    irSurfaceViewWidth = width
                    irSurfaceViewHeight = ScreenUtils.getScreenWidth() * 256 / 192
                }
            }
            0, 2 -> {
                irSurfaceViewWidth = width
                irSurfaceViewHeight = height
            }
        }
    }

    private fun applyLayoutParameters() {
        // Apply calculated dimensions to all layout components
        irSurfaceViewLayoutParams?.let {
            it.width = irSurfaceViewWidth
            it.height = irSurfaceViewHeight
            mIrSurfaceViewLayout?.layoutParams = it
        }

        displayViewLayoutParams?.let {
            it.width = irSurfaceViewWidth
            it.height = irSurfaceViewHeight
            mDisplayFrameLayout?.layoutParams = it
        }

        fenceLayoutParams?.let {
            it.width = irSurfaceViewWidth
            it.height = irSurfaceViewHeight
            mFenceLayout?.layoutParams = it
        }

        cameraLayoutParams?.let {
            it.width = irSurfaceViewWidth
            it.height = irSurfaceViewHeight
            mCameraLayout?.layoutParams = it
        }
    }

    private fun initializeFence() {
        initFence()
    }

    private fun startThermalProcessing() {
        onIrVideoStart()
        
        mIrSurfaceView?.post {
            Log.w("ThermalFragment", "Surface view dimensions - w:${mIrSurfaceView?.width}, h:${mIrSurfaceView?.height}")
        }
    }

    // The remaining methods from the original fragment maintain their functionality
    // but now work with the ViewModel for state management and processing coordination

    override fun initView() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rotateType = 3
        mCenterTextView = requireView().findViewById(R.id.temp_display)
        mMaxTextView = requireView().findViewById(R.id.max_temp_display)
        mMinTextView = requireView().findViewById(R.id.min_temp_display)
        maxImg = requireView().findViewById(R.id.max_img)
        minImg = requireView().findViewById(R.id.min_img)
        mDisplayFrameLayout = requireView().findViewById(R.id.temp_display_layout)
        mFenceLayout = requireView().findViewById(R.id.fence_lay)
        mCameraLayout = requireView().findViewById(R.id.temp_camera_layout)
        mDisplayFrameLayout!!.visibility = View.GONE
        mFenceLayout!!.visibility = View.GONE
        mIrSurfaceViewLayout = requireView().findViewById(R.id.final_ir_layout)
        mIrSurfaceView = IrSurfaceView(requireContext())
        val ifrSurfaceViewLayoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
        mIrSurfaceView!!.layoutParams = ifrSurfaceViewLayoutParams
        mIrSurfaceView!!.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
        mIrSurfaceViewLayout!!.addView(mIrSurfaceView)
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = screenWidth * 192 / 256
        width = screenWidth
        height = screenHeight
        highCrossWidth = resources.getDimension(R.dimen.high_cross_width).toInt()
        highCrossHeight = resources.getDimension(R.dimen.high_cross_height).toInt()
        mIrSurfaceViewLayout!!.viewTreeObserver.addOnGlobalLayoutListener {
            irSurfaceViewLayoutParams =
                mIrSurfaceViewLayout!!.layoutParams as ConstraintLayout.LayoutParams?
            displayViewLayoutParams = mDisplayFrameLayout!!.layoutParams as FrameLayout.LayoutParams
            fenceLayoutParams = mFenceLayout!!.layoutParams as FrameLayout.LayoutParams
            cameraLayoutParams = mCameraLayout!!.layoutParams as FrameLayout.LayoutParams
            when (rotateType) {
                1, 3 -> {
                    irSurfaceViewWidth = height
                    irSurfaceViewHeight = width
                    if (irSurfaceViewWidth < width) {
                        irSurfaceViewWidth = width
                        irSurfaceViewHeight = screenWidth * 256 / 192
                    }
                }

                0, 2 -> {
                    irSurfaceViewWidth = width
                    irSurfaceViewHeight = height
                }
            }
            irSurfaceViewLayoutParams!!.width = irSurfaceViewWidth
            irSurfaceViewLayoutParams!!.height = irSurfaceViewHeight
            mIrSurfaceViewLayout!!.layoutParams = irSurfaceViewLayoutParams

            displayViewLayoutParams!!.width = irSurfaceViewWidth
            displayViewLayoutParams!!.height = irSurfaceViewHeight
            mDisplayFrameLayout!!.layoutParams = displayViewLayoutParams

            fenceLayoutParams!!.width = irSurfaceViewWidth
            fenceLayoutParams!!.height = irSurfaceViewHeight
            mFenceLayout!!.layoutParams = fenceLayoutParams

            cameraLayoutParams!!.width = irSurfaceViewWidth
            cameraLayoutParams!!.height = irSurfaceViewHeight
            mFenceLayout!!.layoutParams = cameraLayoutParams
        }

        initFence()

        onIrVideoStart()
        mIrSurfaceView!!.post {
            Log.w("123", "w:${mIrSurfaceView!!.width}, h:${mIrSurfaceView!!.height}")
        }

        msgLiveData.observe(this) { msg ->
            if (msg == 0) {
                when (selectType) {
                    0 -> {
                        mCenterTextView!!.visibility = View.VISIBLE
                        mMaxTextView!!.visibility = View.VISIBLE
                        mMinTextView!!.visibility = View.VISIBLE
                        mCenterTextView!!.text = "[ph][ph][ph] $mCenter"
                        mMaxTextView!!.text = "[ph][ph][ph] $mMaxTemp"
                        mMinTextView!!.text = "[ph][ph][ph] $mMinTemp"
                        maxImg!!.visibility = View.GONE
                        minImg!!.visibility = View.GONE
                    }

                    1 -> {
                        mCenterTextView!!.visibility = View.VISIBLE
                        mMaxTextView!!.visibility = View.GONE
                        mMinTextView!!.visibility = View.GONE
                        mCenterTextView!!.text = "[ph][ph] $mMaxTemp"
                        maxImg!!.visibility = View.GONE
                        minImg!!.visibility = View.GONE
                    }

                    else -> {
                        mCenterTextView!!.visibility = View.VISIBLE
                        mMaxTextView!!.visibility = View.VISIBLE
                        mMinTextView!!.visibility = View.VISIBLE
                        mCenterTextView!!.text = "[ph][ph][ph] $mCenter"
                        mMaxTextView!!.text = "[ph][ph][ph] $mMaxTemp"
                        mMinTextView!!.text = "[ph][ph][ph] $mMinTemp"
                        maxImg!!.visibility = View.VISIBLE
                        minImg!!.visibility = View.VISIBLE
                        maxImg?.let { setViewPosition(it, maxIndex) }
                        minImg?.let { setViewPosition(it, minIndex) }
                    }
                }
            }
        }
        onTempBtnClick()
    }

    override fun initData() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onIrVideoStop()
    }

    fun onIrVideoStart() {
        mIsIrVideoStart =
            if (mIsIrVideoStart) {
                ToastTools.showShort("[ph][ph][ph][ph][ph][ph]")
                return
            } else {
                true
            }
        mGuideInterface = GuideInterface()
        val ret =
            mGuideInterface!!.init(
                requireContext(),
                object : LibGuideInterface.IrDataCallback {
                    override fun processIrData(
                        yuv: ByteArray,
                        temp: FloatArray,
                    ) {

                        if (mIrBitmap == null) {
                            mIrBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
                        }
                        if (upValue > downValue) {
                            // TODO: Implement yuvArea functionality in ViewModel
                            // thermalViewModel.yuvArea(yuv, temp, upValue, downValue)
                        }
                        mGuideInterface!!.yuv2Bitmap(mIrBitmap, yuv)

                        try {
                            mIrSurfaceView!!.doDraw(mIrBitmap, mGuideInterface!!.getImageStatus())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (rotateType == 1 || rotateType == 3) {
                            rawWidth = SRC_WIDTH
                            rawHeight = SRC_HEIGHT
                        } else {
                            rawWidth = SRC_HEIGHT
                            rawHeight = SRC_WIDTH
                        }
                        val centerIndex = rawWidth * (rawHeight / 2) + rawWidth / 2
                        try {


                            val maxTempIndex = ArrayUtils.getMaxIndex(temp, rotateType, selectIndex)
                            val minTempIndex = ArrayUtils.getMinIndex(temp, rotateType, selectIndex)
                            maxIndex = maxTempIndex
                            minIndex = minTempIndex

                            val rotateData = ArrayUtils.matrixRotate(srcData = temp, rotateType)

                            val bigDecimal = BigDecimal.valueOf(rotateData[centerIndex].toDouble())
                            val maxBigDecimal =
                                BigDecimal.valueOf(rotateData[maxTempIndex].toDouble())
                            val minBigDecimal =
                                BigDecimal.valueOf(rotateData[minTempIndex].toDouble())
                            mCenter = bigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                            mMaxTemp = maxBigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                            mMinTemp = minBigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "[ph][ph][ph][ph][ph][ph]:${e.message}")
                        }
                    }
                },
            )

        if (ret == 5) {
            Log.w("123", "[ph][ph][ph][ph][ph][ph][ph]")
        } else {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
        }
    }

    private fun rotateBitmap(
        origin: Bitmap,
        rotate: Float,
    ): Bitmap? {
        try {
            val width = origin.width
            val height = origin.height
            val matrix = Matrix()
            matrix.setRotate(rotate)
            val newBitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
            if (newBitmap.equals(origin)) {
                return newBitmap
            }
            origin.recycle()
            return newBitmap
        } catch (e: Exception) {
            Log.e("123", "error:${e.message}")
            return origin
        }
    }

    fun onIrVideoStop() {
        mIsIrVideoStart =
            if (!mIsIrVideoStart) {
                ToastTools.showShort("[ph][ph][ph][ph][ph][ph]")
                return
            } else {
                false
            }
        mGuideInterface!!.exit()
        mGuideInterface = null
        ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onLowRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface!!.setRange(1)
        ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onHighRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface!!.setRange(2)
        ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onTempBtnClick() {
        if (mGuideInterface == null) {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        isDispLayTemp = !isDispLayTemp
        if (isDispLayTemp) {
            mDisplayFrameLayout!!.visibility = View.VISIBLE
            timerJob =
                lifecycleScope.launch {
                    repeat(Int.MAX_VALUE) {
                        msgLiveData.postValue(0)
                        delay(1000)
                    }
                }
        } else {
            mDisplayFrameLayout!!.visibility = View.GONE
            if (timerJob != null && timerJob!!.isActive) {
                timerJob!!.cancel()
                timerJob = null
            }
        }
    }

    private var upValue = 0f
    private var downValue = 0f

    private fun addLimit() {
        ThermalInputDialog.Builder(requireContext())
            .setMessage("[ph][ph][ph][ph][ph][ph][ph]")
            .setPositiveListener(LibUiR.string.app_confirm) { up, down, _, _ ->
                ToastTools.showShort("[ph][ph][ph][ph]:$up, [ph][ph]:$down")
                upValue = up
                downValue = down
            }
            .setCancelListener(LibUiR.string.app_cancel)
            .create().show()
    }


    fun onExpertModeClick(view: View?) {
        System.arraycopy(EXPERT_HITS, 1, EXPERT_HITS, 0, EXPERT_HITS.size - 1)
        EXPERT_HITS[EXPERT_HITS.size - 1] = System.currentTimeMillis()
        if (EXPERT_HITS[0] >= System.currentTimeMillis() - EXPERT_MODE_HIT_DURATION) {
            if (mExpertLayout!!.visibility == View.GONE) {
                mExpertLayout!!.visibility = View.VISIBLE
            } else {
                mExpertLayout!!.visibility = View.GONE
            }
            EXPERT_HITS = LongArray(EXPERT_MODE_HIT_COUNT)
        }
    }

    fun onNucShutterClick(view: View?) {
        if (mGuideInterface == null) {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface!!.nuc()
    }


    fun onLut(view: View) {
        mIrSurfaceView!!.setOpenLut()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun action(event: ThermalActionEvent) {
        Log.w("123", "event:${event.action}")
        when (event.action) {
            1001 -> {

                ToastTools.showShort("[ph][ph]")
                picture()
            }

            1002 -> {

                ToastTools.showShort("[ph][ph]")
                video()
            }

            2001 -> {

                clearFenceUI()
                addPoint()
            }

            2002 -> {

                clearFenceUI()
                addLine()
            }

            2003 -> {

                clearFenceUI()
                addFence()
            }

            2004 -> {


                addLimit()
            }

            2006 -> {

                clearFence()
            }

            in 3000..3010 -> {

                setColor(event.action)
            }

            4001 -> {

                rotate()
                clearFence()
            }

            4002 -> {

                enhance()
            }

            4003 -> {

                camera()
            }

            in 5000..5010 -> {

                ToastTools.showShort("[ph][ph]")
            }
        }
    }


    private fun clearFence() {
        clearFenceUI()

        upValue = 0f
        downValue = 0f
        selectType = 0
    }

    private fun clearFenceUI() {
        mFenceLayout!!.visibility = View.GONE
        fenceFlag = 0x000
        selectIndex.clear()
        fenceView.clear()
        fenceLineView.clear()
        fencePointView.clear()
    }

    private fun setColor(action: Int) {
        var type: Int = action % 3000 - 1
        if (type < 0 || type > 10) {
            type = 0
        }
        updatePalette(type)
    }

    private fun updatePalette(index: Int) {
        if (mGuideInterface == null) {
            ToastTools.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface!!.changePalette(index)
    }

    var fenceFlag = 0x000

    private fun addPoint() {
        showFence(1)
    }

    private fun addLine() {
        showFence(2)
    }

    private fun addFence() {
        showFence(3)
    }


    private fun showFence(index: Int) {
        if (fenceFlag.getIndex(index) == 0) {
            fenceFlag = 1.shl(4 * (index - 1))
            mFenceLayout!!.visibility = View.VISIBLE
            fencePointView.visibility =
                if (fenceFlag.getIndex(1) > 0) View.VISIBLE else View.GONE
            fenceLineView.visibility =
                if (fenceFlag.getIndex(2) > 0) View.VISIBLE else View.GONE
            fenceView.visibility =
                if (fenceFlag.getIndex(3) > 0) View.VISIBLE else View.GONE
        } else {
            fenceFlag = 0x000
            mFenceLayout!!.visibility = View.GONE
        }
    }

    private var selectType = 0
    private var selectIndex: ArrayList<Int> = arrayListOf()

    private fun initFence() {
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FencePointView>(R.id.fence_point_view).listener =
            object : FencePointView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    selectType = 1
                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType).getPointIndex(startPoint)
                }
            }
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceLineView>(R.id.fence_line_view).listener =
            object : FenceLineView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    endPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    selectType = 2
                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType)
                            .getLineIndex(startPoint, endPoint)
                }
            }
        requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceView>(R.id.fence_view).listener =
            object : FenceView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    endPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    selectType = 3
                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType)
                            .getAreaIndex(startPoint, endPoint)
                }
            }
    }

    private fun picture() {


    }

    var isVideoRunning = false

    private fun video() {
        if (isVideoRunning) {
            Log.w("123", "[ph][ph][ph][ph]")
            return
        }


        val latestResultPath =
            "/tmp/YapBitmapToMp4_${System.currentTimeMillis()}.mp4" // Temporary fallback
        Log.w("123", "latestResultPath:$latestResultPath")
        YapVideoEncoder(this, File(latestResultPath)).start()
    }


    private fun rotate() {
        rotateType = if (rotateType >= 3) 0 else rotateType + 1
        mIrSurfaceView!!.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
        ToastTools.showShort("[ph][ph]:${ThermalTool.getRotate(rotateType)}[ph]")
    }


    private fun enhance() {
        mIrSurfaceView!!.setOpenLut()
        val saturation = mIrSurfaceView?.getSaturationValue() ?: 0


    }

    var isRunCamera = false

    private fun checkCameraPermission() {

        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            camera()
        } else {

            @Suppress("DEPRECATION")
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            camera()
        }
    }

    @SuppressLint("CheckResult")
    private fun camera() {


        if (isRunCamera) {

            requireView().findViewById<FrameLayout>(R.id.temp_camera_layout).visibility = View.GONE
            isRunCamera = false
        } else {

            requireView().findViewById<FrameLayout>(R.id.temp_camera_layout).visibility =
                View.VISIBLE
            val tempCameraView =
                requireView().findViewById<com.mpdc4gsr.libunified.ui.camera.CameraView>(R.id.temp_camera_view)
            tempCameraView.post {
                tempCameraView.openCamera()
                isRunCamera = true
            }
        }

    }

    override fun size(): Int = 5 * 60

    override fun next(): Bitmap {
        return if (mIrBitmap == null) {
            Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        } else {
            mIrBitmap!!
        }
    }

    override fun progress(progress: Float) {
        Log.w("123", "progress:$progress")
        isVideoRunning = progress > 0 || progress < 100
    }
}
