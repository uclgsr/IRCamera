package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivitySessionExportBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity


class SessionExportActivity : BaseBindingActivity<ActivitySessionExportBinding>() {
    companion object {
    private const val EXTRA_SESSION_ID = "session_id"

    fun startActivity(
    context: Context,
    sessionId: String,
    ) {
    val intent =
    Intent(context, SessionExportActivity::class.java).apply {
    putExtra(EXTRA_SESSION_ID, sessionId)
    }
    context.startActivity(intent)
    }
    }

    override fun initContentLayoutId() = R.layout.activity_session_export

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        // Set the content programmatically
        (binding.root as? android.widget.TextView)?.apply {
            text = "Session Export\n\nSession ID: $sessionId\n\nExport Options:\n• CSV Format\n• JSON Format\n• Research-grade metadata\n• Synchronized timestamps\n\nExport functionality coming soon..."
            setPadding(32, 32, 32, 32)
            textSize = 16f
        }

    // Set the content programmatically
    binding.root.apply {
    text = "Session Export\n\nSession ID: $sessionId\n\nExport Options:\n• CSV Format\n• JSON Format\n• Research-grade metadata\n• Synchronized timestamps\n\nExport functionality coming soon..."
    setPadding(32, 32, 32, 32)
    textSize = 16f
    }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.title = "Export Session"
    }

    override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
    }
}
