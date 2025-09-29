package mpdc4gsr.activities

import android.app.Activity
import android.os.Bundle

/**
 * Minimal activity for USB device attachment handling.
 * This activity is referenced in AndroidManifest.xml but was missing.
 */
class BlankDevActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is a minimal activity for USB device handling
        // It can be extended later if needed
        finish()
    }
}