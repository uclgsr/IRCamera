package mpdc4gsr.compose.utils

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager

/**
 * Utilities for enabling smooth interoperability between Compose and traditional Views/Fragments
 * during the migration period
 */

/**
 * Embed a traditional Android View into Compose
 * Useful for migrating complex custom views gradually
 */
@Composable
fun AndroidViewWrapper(
    viewFactory: () -> View,
    modifier: Modifier = Modifier,
    update: (View) -> Unit = {}
) {
    AndroidView(
        factory = viewFactory,
        modifier = modifier,
        update = update
    )
}

/**
 * Embed a Fragment into Compose
 * Useful for gradual migration of Fragment-based screens
 */
@Composable
fun FragmentContainer(
    fragmentManager: FragmentManager,
    fragmentFactory: () -> Fragment,
    modifier: Modifier = Modifier.fillMaxSize(),
    containerId: Int = View.generateViewId()
) {
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId

                // Add the fragment to the container
                val fragment = fragmentFactory()
                fragmentManager.beginTransaction()
                    .replace(id, fragment)
                    .commit()
            }
        },
        modifier = modifier
    )
}

/**
 * Helper to create hybrid screens that mix Compose and traditional Views
 * Example: Compose UI with embedded thermal camera SurfaceView
 */
@Composable
fun HybridScreen(
    composeContent: @Composable () -> Unit,
    androidViewContent: @Composable () -> Unit
) {
    // This can be customized based on layout requirements
    // For now, simple vertical layout approach
    androidx.compose.foundation.layout.Column {
        composeContent()
        androidViewContent()
    }
}

/**
 * Bridge for StateFlow/LiveData to Compose State
 * Enhanced utilities for fragment integration
 */
object StateFlowBridge {
    // Additional utilities for complex state bridging can be added here
    // if the standard collectAsState() doesn't cover all use cases
}

/**
 * Enhanced Fragment-Compose interoperability utilities
 * for seamless migration support
 */
object FragmentComposeUtils {
    
    /**
     * Create a Compose wrapper for existing Fragment
     * Useful for gradual migration where you want to embed existing fragments
     * in new Compose screens
     */
    @Composable
    fun FragmentCompose(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        fragmentFactory: () -> Fragment,
        modifier: Modifier = Modifier.fillMaxSize()
    ) {
        FragmentContainer(
            fragmentManager = fragmentManager,
            fragmentFactory = fragmentFactory,
            modifier = modifier,
            containerId = View.generateViewId()
        )
    }
    
    /**
     * Navigation helper for Fragment to Compose transitions
     * Maintains back stack consistency
     */
    fun navigateFromFragmentToCompose(
        fragment: Fragment,
        composeActivityClass: Class<*>,
        extras: Bundle? = null,
        finishCurrent: Boolean = false
    ) {
        val intent = Intent(fragment.requireContext(), composeActivityClass).apply {
            extras?.let { putExtras(it) }
        }
        fragment.startActivity(intent)
        
        if (finishCurrent && fragment.activity != null) {
            fragment.activity?.finish()
        }
    }
    
    /**
     * State preservation helper for Fragment-Compose migration
     * Ensures state is maintained across the transition
     */
    fun preserveFragmentState(
        fragment: Fragment,
        key: String,
        value: Any
    ) {
        fragment.arguments = (fragment.arguments ?: Bundle()).apply {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Bundle -> putBundle(key, value)
                // Add more types as needed
            }
        }
    }
}