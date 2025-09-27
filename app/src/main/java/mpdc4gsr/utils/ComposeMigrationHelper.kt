package mpdc4gsr.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.greenrobot.eventbus.EventBus

/**
 * Migration utilities to help convert Activities to Compose while maintaining
 * compatibility with existing patterns.
 */
object ComposeMigrationHelper {

    /**
     * Handle EventBus registration in Compose.
     * Use when migrating Activities that use EventBus but don't extend BaseComposeActivity.
     */
    @Composable
    fun EventBusEffect(subscriber: Any) {
        val lifecycleOwner = LocalLifecycleOwner.current
        
        DisposableEffect(subscriber, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (!EventBus.getDefault().isRegistered(subscriber)) {
                            EventBus.getDefault().register(subscriber)
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        if (EventBus.getDefault().isRegistered(subscriber)) {
                            EventBus.getDefault().unregister(subscriber)
                        }
                    }
                    else -> {}
                }
            }
            
            lifecycleOwner.lifecycle.addObserver(observer)
            
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                if (EventBus.getDefault().isRegistered(subscriber)) {
                    EventBus.getDefault().unregister(subscriber)
                }
            }
        }
    }

    /**
     * Convert traditional View click patterns to Compose.
     */
    data class ClickHandler(
        val onClick: () -> Unit,
        val onDoubleClick: (() -> Unit)? = null,
        val onLongClick: (() -> Unit)? = null
    )

    /**
     * Helper for migrating permission handling from Activities to Compose.
     */
    @Composable
    fun rememberPermissionHandler(): PermissionHandler {
        val context = LocalContext.current
        return remember {
            PermissionHandler(context)
        }
    }

    /**
     * Simplified permission handling for migration.
     */
    class PermissionHandler(private val context: android.content.Context) {
        // Add permission handling methods as needed during migration
        // This can be expanded based on the patterns found in existing Activities
    }

    /**
     * Common conversion patterns for migrating View-based layouts to Compose.
     */
    object ViewToComposePatterns {
        
        // LinearLayout vertical -> Column
        // LinearLayout horizontal -> Row
        // RelativeLayout -> Box with Alignment
        // ScrollView -> Column with verticalScroll()
        // RecyclerView -> LazyColumn/LazyRow
        // ViewPager -> HorizontalPager
        // Fragments -> Composable functions
        
        /**
         * Common pattern: Convert visibility based on boolean to Compose
         */
        fun convertVisibility(isVisible: Boolean): @Composable () -> Unit = {
            if (isVisible) {
                // Content here
            }
        }
        
        /**
         * Common pattern: Convert View.GONE/VISIBLE/INVISIBLE logic
         */
        sealed class ComposeVisibility {
            object Visible : ComposeVisibility()
            object Gone : ComposeVisibility() // Don't render at all
            object Invisible : ComposeVisibility() // Render but transparent
        }
    }

    /**
     * Migration tracking to help identify what has been converted.
     */
    object MigrationTracker {
        private val migratedActivities = mutableSetOf<String>()
        
        fun markActivityMigrated(activityName: String) {
            migratedActivities.add(activityName)
        }
        
        fun isActivityMigrated(activityName: String): Boolean {
            return migratedActivities.contains(activityName)
        }
        
        fun getMigrationProgress(): Pair<Int, Int> {
            // Return (migrated, total) - total can be updated as we identify activities
            return migratedActivities.size to 114 // Based on our analysis
        }
    }
}