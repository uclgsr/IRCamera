package com.topdon.menu.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.topdon.menu.R as MenuR
import com.topdon.lib.core.R
import com.topdon.menu.databinding.ViewCameraMenuBinding

/**
 * Menu 1 - Photo and video capture related functionality.
 *
 * Central photo/video button states:
 * - Photo mode - Normal
 * - Photo mode - Capturing - Instant capture
 * - Photo mode - Capturing - Delayed capture
 * - Video mode - Normal
 * - Video mode - Recording
 *
 * Created by LCG on 2024/11/8.
 */
class CameraMenuView : FrameLayout, View.OnClickListener {
    companion object {
        /** onCameraClickListener event code: photo/video capture */
        const val CODE_ACTION = 0
        /** onCameraClickListener event code: gallery */
        const val CODE_GALLERY = 1
        /** onCameraClickListener event code: more menu */
        const val CODE_MORE = 2
        /** onCameraClickListener event code: switch to photo */
        const val CODE_TO_PHOTO = 3
        /** onCameraClickListener event code: switch to video */
        const val CODE_TO_VIDEO = 4
    }


    /**
     * Whether currently in video mode.
     *
     * true-video mode false-photo mode
     */
    var isVideoMode: Boolean
        get() = binding.viewPager2.currentItem == 1
        set(value) {
            binding.viewPager2.currentItem = if (value) 1 else 0
        }

    /**
     * photo capture/video recording 文字whether可见及whether可switch, photo capture中或video recording中不允许switch.
     *
     * true-可见及可switch false-不可见及不可switch
     */
    var canSwitchMode: Boolean
        get() = binding.viewPager2.isUserInputEnabled
        set(value) {
            binding.viewPager2.isUserInputEnabled = value
            binding.tvPhoto.isVisible = value
            binding.tvVideo.isVisible = value
        }

    /**
     * 各个操作的点击事件监听.
     * actionCode: 0-photo capture/video recording 1-gallery 2-more menu 3-switch to photo 4-switch to video
     */
    var onCameraClickListener: ((actionCode: Int) -> Unit)? = null

    /**
     * 将central photo capture/video recording 按钮settings为 未photo capture/未video recording
     */
    fun setToNormal() {
        if (isVideoMode) {
            binding.ivAction.setImageResource(MenuR.drawable.svg_camera_video_normal)
        } else {
            binding.ivAction.setImageResource(MenuR.drawable.svg_camera_photo_normal)
        }
    }

    /**
     * 将central photo capture/video recording 按钮settings为 photo capture中-立即/photo capture中-延迟/video recording中
     * @param isDelay true-delayed capture false-instant capture video recording的话无所谓
     */
    fun setToRecord(isDelay: Boolean) {
        if (isVideoMode) {
            binding.ivAction.setImageResource(MenuR.drawable.svg_camera_video_record)
        } else {
            if (isDelay) {
                binding.ivAction.setImageResource(MenuR.drawable.svg_camera_photo_record_delay)
            } else {
                binding.ivAction.setImageResource(MenuR.drawable.svg_camera_photo_record_at_once)
            }
        }
    }

    /**
     * 使用指定的本地绝对路径刷新gallery封面.
     */
    fun refreshGallery(path: String) {
        try {
            Glide.with(this)
                .load(path)
                .apply(
                    RequestOptions.bitmapTransform(MultiTransformation(CenterCrop()))
                        .placeholder(MenuR.drawable.shape_oval_33)
                        .error(MenuR.drawable.shape_oval_33)
                )
                .into(binding.ivGallery)
        } catch (_: Exception) {
        }
    }



    private lateinit var binding: ViewCameraMenuBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        if (isInEditMode) {
            LayoutInflater.from(context).inflate(MenuR.layout.view_camera_menu, this, true)
        } else {
            binding = ViewCameraMenuBinding.inflate(LayoutInflater.from(context), this, true)

            binding.viewPager2.adapter = MenuCameraAdapter()
            binding.viewPager2.registerOnPageChangeCallback(MyOnPageChangeCallback())

            binding.ivAction.setOnClickListener(this)
            binding.ivGallery.setOnClickListener(this)
            binding.ivMore.setOnClickListener(this)
            binding.tvPhoto.setOnClickListener(this)
            binding.tvVideo.setOnClickListener(this)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.viewPager2.dispatchTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 考虑到 photo capture, video recording 所需的时间, 需要防止用户快速点击photo capturevideo recording, 保存点击时的时间戳避免.
     */
    private var lastClickTime: Long = 0

    override fun onClick(v: View?) {
        when (v) {
            binding.ivAction -> {// 开始拍照/开始录像/停止录像
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 500) {
                    lastClickTime = currentTime
                    onCameraClickListener?.invoke(CODE_ACTION)
                }
            }
            binding.ivGallery -> {//图库
                onCameraClickListener?.invoke(CODE_GALLERY)
            }
            binding.ivMore -> {//更多菜单
                onCameraClickListener?.invoke(CODE_MORE)
            }
            binding.tvPhoto -> {//拍照文字
                binding.viewPager2.currentItem = 0
            }
            binding.tvVideo -> {//视频文字
                binding.viewPager2.currentItem = 1
            }
        }
    }


    inner class MyOnPageChangeCallback : ViewPager2.OnPageChangeCallback() {

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val scrollWidth: Float = binding.tvPhoto.width / 2f + binding.tvVideo.width / 2f
            if (position == 0) {
                binding.tvPhoto.translationX = -scrollWidth * positionOffset
                binding.tvVideo.translationX = -scrollWidth * positionOffset
            } else {
                binding.tvPhoto.translationX = -scrollWidth
                binding.tvVideo.translationX = -scrollWidth
            }
        }

        override fun onPageSelected(position: Int) {
            binding.tvPhoto.isSelected = position == 0
            binding.tvVideo.isSelected = position == 1
            binding.ivAction.setImageResource(if (position == 1) MenuR.drawable.svg_camera_video_normal else MenuR.drawable.svg_camera_photo_normal)
            onCameraClickListener?.invoke(if (position == 1) CODE_TO_VIDEO else CODE_TO_PHOTO)
        }
    }

    /**
     * ViewPager2 所用 Adapter.
     */
    class MenuCameraAdapter : RecyclerView.Adapter<MenuCameraAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = View(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        }

        override fun getItemCount(): Int = 2

        class ViewHolder(rootView: View): RecyclerView.ViewHolder(rootView)
    }
}