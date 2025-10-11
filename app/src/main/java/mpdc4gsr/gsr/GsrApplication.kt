package mpdc4gsr.gsr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Minimal application entry point for the rewritten GSR stack. All heavy legacy
 * initialisation hooks have been removed; dependencies are provided through Hilt.
 */
@HiltAndroidApp
class GsrApplication : Application()
