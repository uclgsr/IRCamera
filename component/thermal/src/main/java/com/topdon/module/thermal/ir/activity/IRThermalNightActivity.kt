package com.topdon.module.thermal.ir.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.topdon.module.thermal.R

/**
 * IRThermalNightActivity - Consolidated thermal night mode activity
 * Migrated from thermal-ir module for MPDC4GSR
 */
class IRThermalNightActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.activity_list_item)
        
        title = "Thermal Night Mode"
    }
}