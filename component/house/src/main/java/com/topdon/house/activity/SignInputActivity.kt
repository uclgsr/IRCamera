package com.topdon.house.activity

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ToastUtils
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.lib.core.view.TitleView
import com.topdon.house.view.SignView
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.ktbase.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 电子签名界面.
 *
 * 需要传递：
 * - [ExtraKeyConfig.IS_PICK_INSPECTOR] - true-检测师签名 false-房主签名
 *
 * 返回：
 * - [ExtraKeyConfig.IS_PICK_INSPECTOR] - true-检测师签名 false-房主签名
 * - [ExtraKeyConfig.RESULT_PATH_WHITE] - 白色画笔版签名图片在本地的绝对路径.
 * - [ExtraKeyConfig.RESULT_PATH_BLACK] - 黑色画笔版签名图片在本地的绝对路径.
 *
 * Created by LCG on 2024/8/28.
 */
class SignInputActivity : BaseActivity(), View.OnClickListener {
    private lateinit var titleView: TitleView
    private lateinit var clSave: View
    private lateinit var clClear: View
    private lateinit var signView: SignView
    
    override fun isLockPortrait(): Boolean = false

    override fun initContentView(): Int = R.layout.activity_sign_input

    override fun initView() {
        titleView = findViewById(R.id.title_view)
        clSave = findViewById(R.id.cl_save)
        clClear = findViewById(R.id.cl_clear)
        signView = findViewById(R.id.sign_view)
        
        titleView.setTitleText(if (intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, false)) LibR.string.inspector_signature else LibR.string.house_owner_signature)

        clSave.setOnClickListener(this)
        clClear.setOnClickListener(this)

        signView.onSignChangeListener = {
            clSave.alpha = if (it) 1f else 0.5f
        }
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            clSave -> {//保存
                if (!signView.hasSign) {
                    ToastUtils.showShort(getString(LibR.string.house_sign_finish_tips))
                    return
                }

                val whiteBitmap: Bitmap = signView.bitmap ?: return
                showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {
                    val currentTime = System.currentTimeMillis()
                    val whiteFile = FileConfig.getSignImageDir(this@SignInputActivity, "sign${currentTime}_white.png")
                    ImageUtils.save(whiteBitmap, whiteFile, Bitmap.CompressFormat.PNG)

                    val blackBitmap: Bitmap = whiteBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    for (y in 0 until blackBitmap.height) {
                        for (x in 0 until blackBitmap.width) {
                            if (blackBitmap.getPixel(x, y) == 0xffffffff.toInt()) {
                                blackBitmap.setPixel(x, y, 0xff000000.toInt())
                            }
                        }
                    }
                    val blackFile = FileConfig.getSignImageDir(this@SignInputActivity, "sign${currentTime}_black.png")
                    ImageUtils.save(blackBitmap, blackFile, Bitmap.CompressFormat.PNG)

                    withContext(Dispatchers.Main) {
                        val resultIntent = Intent()
                        resultIntent.putExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_INSPECTOR, false))
                        resultIntent.putExtra(ExtraKeyConfig.RESULT_PATH_WHITE, whiteFile.absolutePath)
                        resultIntent.putExtra(ExtraKeyConfig.RESULT_PATH_BLACK, blackFile.absolutePath)
                        setResult(RESULT_OK, resultIntent)
                        dismissLoadingDialog()
                        finish()
                    }
                }
            }
            clClear -> {//重签
                signView.clear()
            }
        }
    }
}