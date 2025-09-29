package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.MediaController
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrVideoPlayerBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity

class GSRVideoPlayerActivity : BaseViewModelActivity<GSRVideoPlayerViewModel>() {
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

    private lateinit var binding: ActivityGsrVideoPlayerBinding
    private lateinit var videoPath: String

    override fun providerVMClass(): Class<GSRVideoPlayerViewModel> =
        GSRVideoPlayerViewModel::class.java

    override fun initContentView() = R.layout.activity_multi_modal_consolidated

    override fun initView() {
        binding = ActivityGsrVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""

        setupObservers()
        setupVideoView()

        viewModel.loadVideo(videoPath, packageName, this)
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    private fun setupObservers() {
        viewModel.videoState.observe(this) { videoState ->
            if (videoState.uri != null) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = videoState.title

                binding.videoView.setVideoURI(videoState.uri)
                binding.videoView.start()
                binding.videoView.requestFocus()

                Log.w(TAG, "Video URI loaded: ${videoState.uri}")
            }
        }

        viewModel.errorState.observe(this) { error ->
            if (error != null) {
                Log.e(TAG, error)
                finish()
            }
        }

        viewModel.videoInfo.observe(this) { videoInfo ->
            // Video info is ready for display when needed
            Log.d(TAG, "Video info loaded: ${videoInfo.fileName}")
        }
    }

    private fun setupVideoView() {
        binding.videoView.setMediaController(MediaController(this))

        binding.videoView.setOnPreparedListener { mediaPlayer ->
            Log.i(TAG, "Video prepared, starting playback")
            mediaPlayer.setVideoScalingMode(
                android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            )
            viewModel.onVideoReady()
        }

        binding.videoView.setOnErrorListener { _, what: Int, extra: Int ->
            viewModel.onVideoError(what, extra)
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.video_player_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    @Suppress("DEPRECATION")
                    onBackPressed()
                }
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
        val uri = viewModel.createShareUri(videoPath, packageName)
        if (uri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "video/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Video"))
        }
    }

    private fun showVideoInfo() {
        viewModel.videoInfo.value?.let { videoInfo ->
            val info = """
                File: ${videoInfo.fileName}
                Size: ${videoInfo.fileSize}
                Created: ${videoInfo.createdDate}
                Path: ${videoInfo.filePath}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Video Information")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            viewModel.onPlayStateChanged(false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Video will resume automatically if it was playing
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
