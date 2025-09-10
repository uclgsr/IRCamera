package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.csl.irCamera.databinding.ActivitySessionDetailBinding
import com.csl.irCamera.R

/**
 * Session Detail Activity
 * Detailed view of recording session with data analysis and export options
 */
class SessionDetailActivity : BaseBindingActivity<ActivitySessionDetailBinding>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"

        fun startActivity(
            context: Context,
            sessionId: String,
        ) {
            val intent =
                Intent(context, SessionDetailActivity::class.java).apply {
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
            context.startActivity(intent)
        }
    }

    override fun initContentLayoutId() = R.layout.activity_session_detail

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        
        // Set the content programmatically since we don't have a complex layout
        (binding.root as? android.widget.TextView)?.apply {
            text = "Session Details\n\nSession ID: $sessionId\n\nDetailed session analysis coming soon..."
            setPadding(32, 32, 32, 32)
            textSize = 16f
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session Details"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
