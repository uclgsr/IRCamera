package com.topdon.module.thermal.activity

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import com.topdon.lib.core.R as LibR
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.BarUtils
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.thermal.R
import java.io.File


// Legacy ARouter route annotation - now using NavigationManager
class VideoActivity : BaseActivity() {

    companion object {
        const val KEY_PATH = "video_path"
    }

    var videoPath = ""

    override fun initContentView() = R.layout.activity_video

    override fun initView() {
        // Set toolbar title  
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(com.topdon.lib.core.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.video)
        
        BarUtils.setNavBarColor(this, ContextCompat.getColor(this, LibR.color.black))
        if (intent.hasExtra(KEY_PATH)) {
            videoPath = intent.getStringExtra(KEY_PATH)!!
        }
        previewVideo(videoPath)
    }

    override fun initData() {
    }

    private fun previewVideo(path: String) {
        Log.w("123", "打开文件:$path")
        val file = File(path.replace("//", "/"))
        Log.i("123", "打开文件file:$file")
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = "${packageName}.fileprovider"
            FileProvider.getUriForFile(this, authority, file)
        } else {
            Uri.fromFile(file)
        }
        Log.w("123", "打开文件uri:$uri")
        val videoView = findViewById<VideoView>(R.id.video_play)
        videoView.setVideoURI(uri)
        videoView.setMediaController(MediaController(this))
        videoView.start()
        videoView.requestFocus()
    }

}