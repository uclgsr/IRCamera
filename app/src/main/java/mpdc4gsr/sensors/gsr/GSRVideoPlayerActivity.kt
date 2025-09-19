package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.MediaController
import androidx.core.content.FileProvider
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrVideoPlayerBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity
import java.io.File

class GSRVideoPlayerActivity : BaseBindingActivity<ActivityGsrVideoPlayerBinding>() {
    companion object {
        private const val TAG = "GSRVideoPlayerActivity"
        private const val EXTRA_VIDEO_PATH = "video_path"

        fun startActivity(
            context: Context,
            videoPath: String,
        ) {
            val intent =
                Intent(context, GSRVideoPlayerActivity::class.java).apply {
                    putExtra(EXTRA_VIDEO_PATH, videoPath)
                }
            context.startActivity(intent)
        }
    }

    private lateinit var videoPath: String

    override fun initContentLayoutId() = R.layout.activity_gsr_video_player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val videoFile = File(videoPath)

        if (!videoFile.exists()) {
            finish()
            return
        }

        setupUI(videoFile)
        playVideo(videoFile)
    }

    private fun setupUI(videoFile: File) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = videoFile.name
    }

    private fun playVideo(videoFile: File) {
        Log.w(TAG, "Opening video file: ${videoFile.absolutePath}")

        val uri: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val authority = "$packageName.fileprovider"
                FileProvider.getUriForFile(this, authority, videoFile)
            } else {
                Uri.fromFile(videoFile)
            }

        Log.w(TAG, "Video URI: $uri")

        binding.videoView.setVideoURI(uri)
        binding.videoView.setMediaController(MediaController(this))
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            Log.i(TAG, "Video prepared, starting playback")

            mediaPlayer.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }
        binding.videoView.setOnErrorListener { _, what: Int, extra: Int ->
            Log.e(TAG, "Video playback error: what=$what, extra=$extra")
            false
        }
        binding.videoView.start()
        binding.videoView.requestFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.video_player_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            R.id.action_share -> {
                shareVideo()
                true
            }

            R.id.action_info -> {
                showVideoInfo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareVideo() {
        val videoFile = File(videoPath)
        val uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile)
            } else {
                Uri.fromFile(videoFile)
            }

        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "video/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }

    private fun showVideoInfo() {
        val videoFile = File(videoPath)
        val fileSize =
            if (videoFile.length() >= 1024 * 1024 * 1024) {
                "%.1f GB".format(videoFile.length() / (1024.0 * 1024.0 * 1024.0))
            } else if (videoFile.length() >= 1024 * 1024) {
                "%.1f MB".format(videoFile.length() / (1024.0 * 1024.0))
            } else {
                "%.1f KB".format(videoFile.length() / 1024.0)
            }

        val createdDate =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(videoFile.lastModified()))

        val info =
            """
            File: ${videoFile.name}
            Size: $fileSize
            Created: $createdDate
            Path: ${videoFile.absolutePath}
            """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Video Information")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
