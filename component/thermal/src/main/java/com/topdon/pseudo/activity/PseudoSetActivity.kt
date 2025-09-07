package com.topdon.pseudo.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.topdon.module.thermal.R

/**
 * PseudoSetActivity - Consolidated pseudo color setting activity
 * Migrated from pseudo module for MPDC4GSR
 */
class PseudoSetActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.activity_list_item)
        
        title = "Pseudo Color Settings"
    }
}