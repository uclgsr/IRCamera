package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivitySessionConsolidatedBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity

class SessionDetailActivity : BaseBindingActivity<ActivitySessionConsolidatedBinding>() {
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

    override fun initContentLayoutId() = R.layout.activity_session_consolidated

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        (binding.root as? android.widget.TextView)?.apply {
            text =
                "Session Details\n\nSession ID: $sessionId\n\nDetailed session analysis coming soon..."
            setPadding(32, 32, 32, 32)
            textSize = 16f
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session Details"
    }

    override fun onSupportNavigateUp(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        return true
    }
}
