package com.topdon.module.thermal.ir.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.topdon.module.thermal.R

/**
 * IRMainActivity - Consolidated thermal IR main activity
 * Migrated from thermal-ir module for MPDC4GSR
 */
class IRMainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simplified layout for consolidated thermal functionality
        setContentView(android.R.layout.activity_list_item)
        
        // TODO: Implement thermal IR main functionality
        title = "Thermal IR Main"
    }
}