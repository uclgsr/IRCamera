package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.csl.irCamera.R

/**
 * Session Export Activity  
 * Export session data in various research formats
 */
class SessionExportActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        
        fun startActivity(context: Context, sessionId: String) {
            val intent = Intent(context, SessionExportActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically
        val textView = TextView(this).apply {
            text = "Session Export\n\nSession ID: ${intent.getStringExtra(EXTRA_SESSION_ID)}\n\nExport Options:\n• CSV Format\n• JSON Format\n• Research-grade metadata\n• Synchronized timestamps\n\nExport functionality coming soon..."
            setPadding(32, 32, 32, 32)
            textSize = 16f
        }
        
        setContentView(textView)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export Session"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}