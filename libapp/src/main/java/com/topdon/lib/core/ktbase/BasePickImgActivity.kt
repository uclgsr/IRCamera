package com.topdon.lib.core.ktbase

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.ActivityImagePickIrPlushBinding
import com.topdon.lib.core.dialog.ColorSelectDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.view.ImageEditView
import kotlinx.coroutines.launch
import java.io.File


abstract class BasePickImgActivity : BaseActivity(), View.OnClickListener {
    protected lateinit var binding: ActivityImagePickIrPlushBinding

    val RESULT_IMAGE_PATH = "RESULT_IMAGE_PATH"

    private var hasTakePhoto = false

    override fun initContentView(): Int {
        return R.layout.activity_image_pick_ir_plush
    }

    override fun initView() {
    }

    override fun initData() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickIrPlushBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivEditCircle.isSelected = true
        binding.imageEditView.type = ImageEditView.Type.CIRCLE
        binding.viewColor.setBackgroundColor(binding.imageEditView.color)

        binding.ivEditColor.setOnClickListener(this)
        binding.ivEditCircle.setOnClickListener(this)
        binding.ivEditRect.setOnClickListener(this)
        binding.ivEditArrow.setOnClickListener(this)
        binding.ivEditClear.setOnClickListener(this)
        binding.imgPick.setOnClickListener(this)

        binding.titleView.setLeftClickListener {
            if (hasTakePhoto) {
                switchPhotoState(false)
            } else {
                finish()
            }
        }
        binding.titleView.setRightClickListener {
            if (hasTakePhoto) {
                val absolutePath: String = intent.getStringExtra(RESULT_IMAGE_PATH)!!
                ImageUtils.save(
                    binding.imageEditView.buildResultBitmap(),
                    File(absolutePath),
                    Bitmap.CompressFormat.PNG
                )
                val intent = Intent()
                intent.putExtra(RESULT_IMAGE_PATH, absolutePath)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        resize()
    }

    private fun resize() {
        val widthPixels = resources.displayMetrics.widthPixels
        val heightPixels = resources.displayMetrics.heightPixels
        binding.titleView.measure(
            MeasureSpec.makeMeasureSpec(widthPixels, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightPixels, MeasureSpec.AT_MOST),
        )

        val ivPickHeight = SizeUtils.dp2px(60f + 20 + 20) 
        val menuHeight = (widthPixels * 75f / 384).toInt()
        val bottomHeight = ivPickHeight.coerceAtLeast(menuHeight)
        val canUseHeight = heightPixels - binding.titleView.measuredHeight - bottomHeight
        val wantHeight = (widthPixels * 256f / 192).toInt()
        if (wantHeight <= canUseHeight) { 
            binding.fragmentContainerView.layoutParams =
                binding.fragmentContainerView.layoutParams.apply {
                    this.width = widthPixels
                    this.height = wantHeight
                }
            binding.imageEditView.layoutParams =
                binding.imageEditView.layoutParams.apply {
                    this.width = widthPixels
                    this.height = wantHeight
                }
        } else {
            binding.fragmentContainerView.layoutParams =
                binding.fragmentContainerView.layoutParams.apply {
                    this.width = (canUseHeight * 192f / 256).toInt()
                    this.height = canUseHeight
                }
            binding.imageEditView.layoutParams =
                binding.imageEditView.layoutParams.apply {
                    this.width = (canUseHeight * 192f / 256).toInt()
                    this.height = canUseHeight
                }
        }
    }

    open suspend fun getPickBitmap(): Bitmap? {
        return null
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.imgPick -> {
                lifecycleScope.launch {
                    getPickBitmap()?.let {
                        switchPhotoState(true)
                        binding.imageEditView.sourceBitmap = it
                        binding.imageEditView.clear()
                    }
                }
            }

            binding.ivEditColor -> {
                val colorPickDialog = ColorSelectDialog(this, binding.imageEditView.color)
                colorPickDialog.onPickListener = {
                    binding.imageEditView.color = it
                    binding.viewColor.setBackgroundColor(it)
                }
                colorPickDialog.show()
            }

            binding.ivEditCircle -> {
                binding.ivEditCircle.isSelected = true
                binding.ivEditRect.isSelected = false
                binding.ivEditArrow.isSelected = false
                binding.imageEditView.type = ImageEditView.Type.CIRCLE
            }

            binding.ivEditRect -> {
                binding.ivEditCircle.isSelected = false
                binding.ivEditRect.isSelected = true
                binding.ivEditArrow.isSelected = false
                binding.imageEditView.type = ImageEditView.Type.RECT
            }

            binding.ivEditArrow -> {
                binding.ivEditCircle.isSelected = false
                binding.ivEditRect.isSelected = false
                binding.ivEditArrow.isSelected = true
                binding.imageEditView.type = ImageEditView.Type.ARROW
            }

            binding.ivEditClear -> binding.imageEditView.clear()
        }
    }

    @Deprecated("This method is deprecated")
    override fun onBackPressed() {
        if (hasTakePhoto) {
            switchPhotoState(false)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun switchPhotoState(hasTakePhoto: Boolean) {
        this.hasTakePhoto = hasTakePhoto
        binding.imageEditView.isVisible = hasTakePhoto
        binding.clEditMenu.isVisible = hasTakePhoto
        binding.imgPick.isVisible = !hasTakePhoto
        binding.fragmentContainerView.isVisible = !hasTakePhoto
        binding.titleView.setRightDrawable(if (hasTakePhoto) R.drawable.app_save else 0)
    }

    private fun showExitTipsDialog(listener: (() -> Unit)) {
        TipDialog.Builder(this)
            .setMessage(R.string.diy_tip_save)
            .setPositiveListener(R.string.app_exit) {
                listener.invoke()
            }
            .setCancelListener(R.string.app_cancel)
            .create().show()
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }
}
