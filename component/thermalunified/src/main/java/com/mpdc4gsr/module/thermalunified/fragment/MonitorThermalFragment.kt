package com.mpdc4gsr.module.thermalunified.fragment

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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.module.thermalunified.stubs.GuideInterface
import com.mpdc4gsr.module.thermalunified.stubs.IrSurfaceView
import com.mpdc4gsr.libunified.app.tools.TimeTool
import com.mpdc4gsr.libunified.app.utils.ByteUtils.getIndex
import com.mpdc4gsr.module.thermalunified.stubs.FenceLineView
import com.mpdc4gsr.module.thermalunified.stubs.FencePointView
import com.mpdc4gsr.module.thermalunified.stubs.FenceView
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.MonitorActivity
import com.mpdc4gsr.module.thermalunified.base.BaseThermalFragment
import com.mpdc4gsr.module.thermalunified.event.ThermalActionEvent
import com.mpdc4gsr.module.thermalunified.tools.Fence
import com.mpdc4gsr.module.thermalunified.tools.ThermalTool
import com.mpdc4gsr.module.thermalunified.tools.medie.IYapVideoProvider
import com.mpdc4gsr.module.thermalunified.tools.medie.YapVideoEncoder
import com.mpdc4gsr.module.thermalunified.utils.ArrayUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigDecimal

class MonitorThermalFragment : BaseThermalFragment(), IYapVideoProvider<Bitmap> {
    protected var mIrSurfaceViewLayout: FrameLayout? = null
    protected var mIrSurfaceView: IrSurfaceView? = null

    override fun initContentView() = R.layout.fragment_monitor_thermal

    private val msgLiveData by lazy { MutableLiveData<Int>() }

    // Cached fence views to avoid repeated findViewById calls
    private val fencePointView by lazy { requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FencePointView>(R.id.fence_point_view) }
    private val fenceLineView by lazy { requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceLineView>(R.id.fence_line_view) }
    private val fenceView by lazy { requireView().findViewById<com.mpdc4gsr.module.thermalunified.stubs.FenceView>(R.id.fence_view) }

    private fun setViewPosition(
        imageView: ImageView,
        index: Int,
    ) {
        if (rawWidth == 0 || rawHeight == 0) {
            return
        }
        val vg = imageView.parent as ViewGroup
        val pw = vg.width
        val ph = vg.height
        val y = index / rawWidth
        val x = index - y * rawWidth
        val x1 = x * pw / rawWidth
        val y1 = y * ph / rawHeight
        val maxX = x1 - imageView.width / 2
        val maxY = y1 - imageView.height / 2

        imageView.x = maxX.toFloat()
        imageView.y = maxY.toFloat()
    }

    private var mGuideInterface: GuideInterface? = null

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
        mIrSurfaceView?.let { surfaceView ->
            surfaceView.layoutParams = ifrSurfaceViewLayoutParams
            surfaceView.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
            mIrSurfaceViewLayout!!.addView(surfaceView)
        }


        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = screenWidth * 270 / 360
        Log.w("123", "screenWidth[ph][ph]:$screenWidth / $screenHeight")
        Log.w("123", "screenWidth[ph][ph]:${screenWidth.toFloat() / screenHeight}")
        width = screenWidth
        height = screenHeight
        highCrossWidth = resources.getDimension(R.dimen.high_cross_width).toInt()
        highCrossHeight = resources.getDimension(R.dimen.high_cross_height).toInt()
        mIrSurfaceViewLayout!!.viewTreeObserver.addOnGlobalLayoutListener {
            irSurfaceViewLayoutParams =
                mIrSurfaceViewLayout!!.layoutParams as ConstraintLayout.LayoutParams?
            displayViewLayoutParams =
                mDisplayFrameLayout!!.layoutParams as FrameLayout.LayoutParams
            fenceLayoutParams = mFenceLayout!!.layoutParams as FrameLayout.LayoutParams
            cameraLayoutParams = mFenceLayout!!.layoutParams as FrameLayout.LayoutParams
            when (rotateType) {
                1, 3 -> {
                    irSurfaceViewWidth = height
                    irSurfaceViewHeight = width
                    if (irSurfaceViewWidth < width) {
                        irSurfaceViewWidth = width
                        irSurfaceViewHeight = screenWidth * 360 / 270
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

        }

        initFence()

        onIrVideoStart()
        mIrSurfaceView?.let { surfaceView ->
            surfaceView.post {
                Log.w("123", "w:${surfaceView.width}, h:${surfaceView.height}")
            }
        }

        msgLiveData.observe(this) { msg ->
            if (msg == 0) {
                mCenterTextView!!.text = "[ph][ph][ph] $mCenter"
                mMaxTextView!!.text = "[ph][ph][ph] $mMaxTemp"
                mMinTextView!!.text = "[ph][ph][ph] $mMinTemp"
                maxImg?.let { setViewPosition(it, maxIndex) }
                minImg?.let { setViewPosition(it, minIndex) }
            }
        }
    }

    override fun initData() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isRecord = false
        onIrVideoStop()
    }

    fun onIrVideoStart() {
        mIsIrVideoStart =
            if (mIsIrVideoStart) {
                ToastUtils.showShort("[ph][ph][ph][ph][ph][ph]")
                return
            } else {
                true
            }
        mGuideInterface = GuideInterface()
        val ret =
            mGuideInterface!!.init(
                requireContext(),
                object : com.mpdc4gsr.libunified.app.matrix.GuideInterface.IrDataCallback {
                    override fun processIrData(
                        yuv: ByteArray,
                        temp: FloatArray,
                    ) {

                        if (mIrBitmap == null) {
                            mIrBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
                        }
                        mGuideInterface?.let { guide ->
                            guide.yuv2Bitmap(mIrBitmap, yuv)

                            try {
                                mIrSurfaceView?.let { surfaceView ->
                                    surfaceView.doDraw(mIrBitmap, guide.getImageStatus())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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
                            mCenter =
                                bigDecimal.setScale(1, java.math.RoundingMode.HALF_UP).toFloat()
                            mMaxTemp =
                                maxBigDecimal.setScale(1, java.math.RoundingMode.HALF_UP).toFloat()
                            mMinTemp =
                                minBigDecimal.setScale(1, java.math.RoundingMode.HALF_UP).toFloat()
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

            Log.w("123", "[ph][ph][ph][ph][ph][ph][ph]")
            mGuideInterface = null
            mIsIrVideoStart = false
        }
    }

    private fun rotateBitmap(
        origin: Bitmap,
        rotate: Float,
    ): Bitmap? {
        try {
            if (origin == null) {
                return null
            }
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
                Log.w("123", "[ph][ph][ph][ph][ph][ph]")
                return
            } else {
                false
            }
        mGuideInterface?.exit()
        mGuideInterface = null
        Log.w("123", "[ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onLowRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface?.setRange(1)
        ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onHighRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface?.setRange(2)
        ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph][ph]")
    }

    fun onTempBtnClick() {
        if (mGuideInterface == null) {
            ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph]")
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
            ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface?.nuc()
    }


    fun onLut(view: View) {
        mIrSurfaceView?.setOpenLut()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun action(event: ThermalActionEvent) {
        Log.w("123", "event:${event.action}")
        when (event.action) {
            1001 -> {

                ToastUtils.showShort("[ph][ph]")
                picture()
            }

            1002 -> {

                ToastUtils.showShort("[ph][ph]")
                video()
            }

            2001 -> {

                addPoint()
            }

            2002 -> {

                addLine()
            }

            2003 -> {

                addFence()
            }

            2004 -> {

                onTempBtnClick()
            }

            2006 -> {

                clearFence()
            }

            in 3000..3010 -> {

                setColor(event.action)
            }

            in 5000..5010 -> {

                full()
            }

            10001 -> {

                recordThermal()
            }

            10003 -> {

                isRecord = false
            }
        }
    }

    private fun clearFence() {
        fenceFlag = 0x000
        mFenceLayout!!.visibility = View.GONE
        selectIndex.clear()
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
            ToastUtils.showShort("[ph][ph][ph][ph][ph][ph][ph]")
            return
        }
        mGuideInterface?.changePalette(index)
    }

    var fenceFlag = 0x000

    private fun addPoint() {
        showFence(1)
        type = "point"
    }

    private fun addLine() {
        showFence(2)
        type = "line"
    }

    private fun addFence() {
        showFence(3)
        type = "fence"
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

    var selectIndex: ArrayList<Int> = arrayListOf()

    private fun initFence() {
        fencePointView.listener =
            object : FencePointView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    val activity: MonitorActivity = requireActivity() as MonitorActivity
                    selectIndex.clear()
                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType).getPointIndex(startPoint)
                    activity.select(1, selectIndex)
                }
            }
        fenceLineView.listener =
            object : FenceLineView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    endPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType)
                            .getLineIndex(startPoint, endPoint)
                    val activity: MonitorActivity = requireActivity() as MonitorActivity
                    activity.select(2, selectIndex)
                }
            }
        fenceView.listener =
            object : FenceView.CallBack {
                override fun callback(
                    startPoint: IntArray,
                    endPoint: IntArray,
                    srcRect: IntArray,
                ) {

                    selectIndex =
                        Fence(srcRect = srcRect, rotateType = rotateType)
                            .getAreaIndex(startPoint, endPoint)
                    val activity: MonitorActivity = requireActivity() as MonitorActivity
                    activity.select(3, selectIndex)
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

    private fun full() {
        rotateType =
            if (rotateType == 0) {
                Log.w("123", "[ph][ph][ph][ph]")
                1
            } else {
                0
            }
        mIrSurfaceView?.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
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

    var isRecord = false
    var type = ""
    var timeMillis = 1000L

    private fun recordThermal() {
        val thermalId = TimeTool.showDateSecond()
        lifecycleScope.launch {
            isRecord = true
            val activity: MonitorActivity = requireActivity() as MonitorActivity
            var time = 0L
            while (isRecord) {
                activity.updateTime(time)
                val bean = ThermalEntity()
                bean.userId = SharedManager.getUserId()
                bean.thermalId = thermalId
                bean.thermal = mCenter
                bean.thermalMax = mMaxTemp
                bean.thermalMin = mMinTemp
                bean.type = type
                bean.createTime = System.currentTimeMillis()
                AppDatabase.getInstance().thermalDao().insert(bean)
                delay(timeMillis)
                time++
            }
            Log.w("123", "[ph][ph][ph][ph], [ph][ph][ph]:$time")
        }
    }
}
