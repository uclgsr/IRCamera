package mpdc4gsr.feature.device.ui

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.BaseComposeActivity
import mpdc4gsr.core.ui.BaseViewModel

class BlankDevActivityComposeViewModel : BaseViewModel()

/**
 * Compose version of BlankDevActivity
 *
 * Minimal activity for USB device attachment handling with modern UI.
 * Shows a brief message before closing automatically.
 */
class BlankDevActivityCompose : BaseComposeActivity<BlankDevActivityComposeViewModel>() {

    override fun createViewModel(): BlankDevActivityComposeViewModel = BlankDevActivityComposeViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-close after 2 seconds like the original
        lifecycleScope.launch {
            delay(2000)
            if (!isFinishing) {
                finish()
            }
        }
    }

    @Composable
    override fun Content(viewModel: BlankDevActivityComposeViewModel) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.UsbOff,
                    contentDescription = "USB Device",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "USB Device Handler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Processing USB device attachment...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        }
    }
}