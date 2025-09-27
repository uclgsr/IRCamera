package com.mpdc4gsr.libunified.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.mpdc4gsr.libunified.app.bean.CustomPseudoBean
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig

/**
 * Stub implementation of PseudoSetActivity to replace the removed pseudo module.
 * This activity simply returns the input CustomPseudoBean without modification.
 */
class PseudoSetActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the input CustomPseudoBean
        val customPseudoBean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                ExtraKeyConfig.CUSTOM_PSEUDO_BEAN,
                CustomPseudoBean::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<CustomPseudoBean>(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN)
        } ?: CustomPseudoBean()

        // Immediately return the same bean (no modification)
        val resultIntent = Intent()
        resultIntent.putExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN, customPseudoBean)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}