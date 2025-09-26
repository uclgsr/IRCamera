package com.mpdc4gsr.libunified.app.menu.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mpdc4gsr.libunified.databinding.ViewCameraMenuBinding

/**
 * Custom view for camera menu controls
 */
class CameraMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewCameraMenuBinding
    
    var onCameraClickListener: ((Int) -> Unit)? = null
    var isVideoMode: Boolean = false
        set(value) {
            field = value
            updateUI()
        }
    
    var canSwitchMode: Boolean = true
        set(value) {
            field = value
            updateUI()
        }

    init {
        binding = ViewCameraMenuBinding.inflate(LayoutInflater.from(context), this, true)
        setupViews()
    }

    private fun setupViews() {
        binding.ivAction.setOnClickListener { 
            if (isVideoMode) {
                onCameraClickListener?.invoke(MODE_VIDEO_TOGGLE)
            } else {
                onCameraClickListener?.invoke(MODE_PHOTO)
            }
        }
        binding.ivGallery.setOnClickListener { 
            onCameraClickListener?.invoke(MODE_GALLERY)
        }
        binding.ivMore.setOnClickListener { 
            onCameraClickListener?.invoke(MODE_MORE)
        }
        binding.tvPhoto.setOnClickListener {
            setToNormal()
        }
        binding.tvVideo.setOnClickListener {
            setToRecord()
        }
    }

    private fun updateUI() {
        binding.tvPhoto.isSelected = !isVideoMode
        binding.tvVideo.isSelected = isVideoMode
        
        // Update action button appearance based on mode
        if (isVideoMode) {
            binding.ivAction.setImageResource(com.mpdc4gsr.libunified.R.drawable.svg_camera_video_normal)
        } else {
            binding.ivAction.setImageResource(com.mpdc4gsr.libunified.R.drawable.svg_camera_photo_normal)
        }
    }

    fun setToNormal() {
        isVideoMode = false
    }

    fun setToRecord() {
        isVideoMode = true
    }

    fun refreshGallery(isVideo: Boolean = false) {
        // Trigger gallery refresh
        onCameraClickListener?.invoke(MODE_GALLERY_REFRESH)
    }

    companion object {
        const val MODE_PHOTO = 0
        const val MODE_VIDEO_TOGGLE = 1
        const val MODE_GALLERY = 2
        const val MODE_MORE = 3
        const val MODE_GALLERY_REFRESH = 4
    }
}