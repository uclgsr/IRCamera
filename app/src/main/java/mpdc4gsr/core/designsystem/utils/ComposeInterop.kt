package mpdc4gsr.core.designsystem.utils

import android.content.Context
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

@Composable
fun AndroidViewWrapper(
    viewFactory: (Context) -> View,
    modifier: Modifier = Modifier,
    update: (View) -> Unit = {},
) {
    AndroidView(
        factory = viewFactory,
        modifier = modifier,
        update = update,
    )
}

@Composable
fun FragmentContainer(
    fragmentManager: FragmentManager,
    fragmentFactory: () -> Fragment,
    modifier: Modifier = Modifier.fillMaxSize(),
    containerId: Int = View.generateViewId(),
) {
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId
                // Add the fragment to the container
                val fragment = fragmentFactory()
                fragmentManager
                    .beginTransaction()
                    .replace(id, fragment)
                    .commit()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun HybridScreen(
    composeContent: @Composable () -> Unit,
    androidViewContent: @Composable () -> Unit,
) {
    // This can be customized based on layout requirements
    // For now, simple vertical layout approach
    androidx.compose.foundation.layout.Column {
        composeContent()
        androidViewContent()
    }
}

object StateFlowBridge {
    // Additional utilities for complex state bridging can be added here
    // if the standard collectAsState() doesn't cover all use cases
}

object FragmentComposeUtils {
    @Composable
    fun FragmentCompose(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        fragmentFactory: () -> Fragment,
        modifier: Modifier = Modifier.fillMaxSize(),
    ) {
        FragmentContainer(
            fragmentManager = fragmentManager,
            fragmentFactory = fragmentFactory,
            modifier = modifier,
            containerId = View.generateViewId(),
        )
    }

    fun navigateFromFragmentToCompose(
        fragment: Fragment,
        composeActivityClass: Class<*>,
        extras: Bundle? = null,
        finishCurrent: Boolean = false,
    ) {
        val intent =
            Intent(fragment.requireContext(), composeActivityClass).apply {
                extras?.let { putExtras(it) }
            }
        fragment.startActivity(intent)
        if (finishCurrent && fragment.activity != null) {
            fragment.activity?.finish()
        }
    }

    fun preserveFragmentState(
        fragment: Fragment,
        key: String,
        value: Any,
    ) {
        fragment.arguments =
            (fragment.arguments ?: Bundle()).apply {
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
